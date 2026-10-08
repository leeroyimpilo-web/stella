import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";
import pg from "pg";
import {
  PRICE_CENTS, PRICE_DISPLAY, generateSerial, serialHash, tokenHash,
  newToken, encryptSerial, decryptSerial, payFastSignature,
  payFastParamString, timingSafeEqualHex, verifyPaymentData,
  signOfflineLicense, validHash, validInstallationId
} from "./lib.mjs";

const here = path.dirname(fileURLToPath(import.meta.url));
const env = process.env;
const sandbox = env.PAYFAST_MODE !== "live";
const base = (env.PUBLIC_BASE_URL || "").replace(/\/$/, "");
const pool = new pg.Pool({
  connectionString: env.DATABASE_URL,
  ssl: env.DATABASE_URL?.includes("sslmode=require") ? { rejectUnauthorized: false } : undefined,
  max: 8
});
const payfastHost = sandbox ? "sandbox.payfast.co.za" : "www.payfast.co.za";
const attempts = new Map();
const MAX_BODY = 80_000;

if (!env.DATABASE_URL) throw new Error("DATABASE_URL must be configured");
if (!base.startsWith("https://") && !base.startsWith("http://localhost:"))
  throw new Error("PUBLIC_BASE_URL must be HTTPS (or localhost for testing)");
if (!sandbox && env.ENABLE_LIVE_PAYMENTS !== "true")
  throw new Error("Live PayFast payments blocked until ENABLE_LIVE_PAYMENTS=true");

function send(res, status, value, headers = {}) {
  res.writeHead(status, {
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    "X-Frame-Options": "DENY",
    "Referrer-Policy": "no-referrer",
    "Content-Type": "application/json; charset=utf-8",
    ...headers
  });
  res.end(JSON.stringify(value));
}
function html(res, status, content) {
  res.writeHead(status, {
    "Content-Type": "text/html; charset=utf-8",
    "Content-Security-Policy": "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' https:; connect-src 'self'; form-action 'self' https://sandbox.payfast.co.za https://www.payfast.co.za; object-src 'none'",
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "same-origin"
  });
  res.end(content);
}
function limited(req, label, max = 20) {
  const clientIp = req.socket.remoteAddress || "unknown";
  const bucket = `${label}:${clientIp}`, now = Date.now();
  const old = attempts.get(bucket);
  if (!old || old.ends < now) {
    attempts.set(bucket, { uses: 1, ends: now + 15 * 60_000 });
    return false;
  }
  old.uses++;
  return old.uses > max;
}
async function body(req, form = false) {
  const chunks = [];
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > MAX_BODY) throw Object.assign(Error("Body too large"), { status: 413 });
    chunks.push(chunk);
  }
  const raw = Buffer.concat(chunks).toString("utf8");
  if (form) {
    const params = new URLSearchParams(raw);
    const entries = [...params.entries()];
    if (new Set(entries.map(([k])=>k)).size !== entries.length) throw Object.assign(Error("Duplicate webhook keys"), { status: 400 });
    return entries;
  }
  return JSON.parse(raw);
}
function admin(req) {
  const secret = env.ADMIN_TOKEN;
  if (!secret || secret.length < 40) return false;
  return timingSafeEqualHex(tokenHash((req.headers.authorization || "").replace(/^Bearer /i,"")), tokenHash(secret));
}
function orderTokenOk(row, given) {
  return typeof given === "string" && given.length >= 40 &&
    timingSafeEqualHex(row.token_hash, tokenHash(given));
}
function cleanRetailer(r) {
  return { slug: r.slug, name: r.display_name, color: r.brand_color,
    logoUrl: r.logo_url, commissionPercent: r.commission_percent };
}
async function findRetailer(slug) {
  const {rows} = await pool.query(
    "SELECT * FROM retailers WHERE slug=$1 AND active=true", [slug]);
  return rows[0];
}
function splitFor(r) {
  if (r.commission_percent === 0) return null;
  if (!r.split_approved || !/^\d{8}$/.test(r.payfast_receiver_id || ""))
    throw Object.assign(Error("Retailer's PayFast split account has not been approved"), {status:503});
  return JSON.stringify({
    split_payment: {
      merchant_id: Number(r.payfast_receiver_id),
      percentage: r.commission_percent
    }
  });
}
async function checkout(req, res) {
  if (limited(req,"checkout",15)) return send(res,429,{error:"Too many attempts"});
  const input=await body(req);
  if (input.ageConfirmed !== true) return send(res,400,{error:"Sales are restricted to adults 18+"});
  const email=String(input.email || "").trim().toLowerCase();
  const slug=String(input.retailer || "");
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || email.length > 254)
    return send(res,400,{error:"Please enter a valid email address"});
  if (!/^[a-z0-9-]{2,50}$/.test(slug)) return send(res,400,{error:"Invalid store reference"});
  if (!env.PAYFAST_MERCHANT_ID || !env.PAYFAST_MERCHANT_KEY || !env.PAYFAST_PASSPHRASE)
    return send(res,503,{error:"Payments have not been configured"});
  const store=await findRetailer(slug);
  if(!store) return send(res,404,{error:"Retailer not found"});
  const setup=splitFor(store);
  const id=crypto.randomUUID(), accessToken=newToken();
  await pool.query(
    "INSERT INTO orders(id,retailer_id,buyer_email,token_hash,commission_percent) VALUES($1,$2,$3,$4,$5)",
    [id,store.id,email,tokenHash(accessToken),store.commission_percent]
  );
  const paymentFields={
    merchant_id:env.PAYFAST_MERCHANT_ID,merchant_key:env.PAYFAST_MERCHANT_KEY,
    return_url:`${base}/success?order=${encodeURIComponent(id)}`,
    cancel_url:`${base}/s/${encodeURIComponent(slug)}?cancelled=1`,
    notify_url:`${base}/api/payfast/itn`,
    name_first:"Customer",email_address:email,
    m_payment_id:id, amount: PRICE_DISPLAY,
    item_name:"Lotto Intelligence Android App"
  };
  paymentFields.signature=payFastSignature(paymentFields,env.PAYFAST_PASSPHRASE);
  if(setup) paymentFields.setup=setup;
  send(res,200,{
    orderId:id,accessToken,
    paymentUrl:`https://${payfastHost}/eng/process`,
    paymentFields
  });
}
async function validateWithPayFast(entries) {
  const payload=payFastParamString(entries.filter(([k]) => k!=="signature"),null);
  const response=await fetch(`https://${payfastHost}/eng/query/validate`,{
    method:"POST",
    headers:{"Content-Type":"application/x-www-form-urlencoded","User-Agent":"LottoIntelligence/0.1"},
    body:payload,
    signal:AbortSignal.timeout(15_000)
  });
  return response.ok && (await response.text()).trim()==="VALID";
}
async function itn(req,res) {
  const entries=await body(req,true);
  const fields=Object.fromEntries(entries);
  const given=String(fields.signature || "");
  const computed=payFastSignature(entries,env.PAYFAST_PASSPHRASE);
  if(!timingSafeEqualHex(given,computed))
    return send(res,400,{error:"Payment signature invalid"});
  if(!/^[0-9a-f-]{36}$/i.test(fields.m_payment_id || ""))
    return send(res,400,{error:"Invalid order"});
  // A notification is NOT proof of payment without verifying it with PayFast.
  if(!await validateWithPayFast(entries))
    return send(res,400,{error:"Payment server validation failed"});
  const client=await pool.connect();
  try {
    await client.query("BEGIN");
    const result=await client.query("SELECT * FROM orders WHERE id=$1 FOR UPDATE",[fields.m_payment_id]);
    const order=result.rows[0];
    if(!order || !verifyPaymentData(fields,order,env.PAYFAST_MERCHANT_ID)) {
      await client.query("ROLLBACK");
      return send(res,400,{error:"Payment details mismatch"});
    }
    if(order.status==="paid") {
      if (order.pf_payment_id !== fields.pf_payment_id) {
        await client.query("ROLLBACK");
        return send(res,409,{error:"Transaction mismatch"});
      }
      await client.query("COMMIT");
      return send(res,200,{received:true,duplicate:true});
    }
    if(order.status!=="pending") {
      await client.query("ROLLBACK");
      return send(res,409,{error:"Order no longer payable"});
    }
    if(!env.SERIAL_HMAC_KEY || !env.SERIAL_ENCRYPTION_KEY_B64) throw Error("Serial encryption not configured");
    const serial=generateSerial();
    const cipher=encryptSerial(serial,env.SERIAL_ENCRYPTION_KEY_B64);
    await client.query(
      "UPDATE orders SET status='paid',pf_payment_id=$1,paid_at=now() WHERE id=$2",
      [fields.pf_payment_id,order.id]);
    await client.query(
      "INSERT INTO licenses(id,order_id,serial_hash,serial_cipher) VALUES($1,$2,$3,$4)",
      [crypto.randomUUID(),order.id,serialHash(serial,env.SERIAL_HMAC_KEY),cipher]);
    await client.query("COMMIT");
    return send(res,200,{received:true});
  } catch (error) {
    await client.query("ROLLBACK").catch(()=>{});
    throw error;
  } finally { client.release(); }
}
async function orderStatus(req,res,id) {
  const {rows}=await pool.query(
    "SELECT o.id,o.status,o.token_hash,r.display_name,l.serial_cipher FROM orders o JOIN retailers r ON o.retailer_id=r.id LEFT JOIN licenses l ON l.order_id=o.id WHERE o.id=$1",
    [id]);
  const o=rows[0];
  if(!o || !orderTokenOk(o,req.headers["x-order-token"]))
    return send(res,404,{error:"Order not found"});
  return send(res,200,{
    orderId:o.id,status:o.status,retailer:o.display_name,
    serial:o.status==="paid" && o.serial_cipher ?
      decryptSerial(o.serial_cipher,env.SERIAL_ENCRYPTION_KEY_B64):null,
    downloadReady:o.status==="paid" && fs.existsSync(env.APK_FILE_PATH||"")
  });
}
async function downloadApk(req,res,id) {
  const {rows}=await pool.query("SELECT status,token_hash FROM orders WHERE id=$1",[id]);
  if(!rows[0] || !orderTokenOk(rows[0],req.headers["x-order-token"]) ||
     rows[0].status!=="paid") return send(res,403,{error:"Paid order required"});
  const apk=env.APK_FILE_PATH || "";
  if(!apk || !fs.existsSync(apk)) return send(res,503,{error:"Signed production APK not uploaded"});
  const stat=fs.statSync(apk);
  if(!stat.isFile()) return send(res,503,{error:"APK unavailable"});
  res.writeHead(200,{
    "Content-Type":"application/vnd.android.package-archive",
    "Content-Length":stat.size,
    "Content-Disposition":'attachment; filename="Lotto-Intelligence.apk"',
    "Cache-Control":"private, no-store",
    "X-Content-Type-Options":"nosniff"
  });
  fs.createReadStream(apk).pipe(res);
}
async function activate(req,res) {
  if(limited(req,"activate",15)) return send(res,429,{error:"Too many activation attempts"});
  const input=await body(req);
  const serial=String(input.serial || "").trim().toUpperCase();
  if(!/^LTI-([A-F0-9]{6}-){3}[A-F0-9]{6}$/.test(serial) ||
     !validHash(input.deviceHash) || !validInstallationId(input.installationId)) {
    return send(res,400,{error:"Invalid serial or device parameters"});
  }
  if(!env.LICENSE_PRIVATE_KEY_PEM) return send(res,503,{error:"Licence signing is not configured"});
  const hash=serialHash(serial,env.SERIAL_HMAC_KEY);
  const client=await pool.connect();
  try {
    await client.query("BEGIN");
    const {rows}=await client.query(
      "SELECT l.*,o.status,r.display_name,r.slug FROM licenses l JOIN orders o ON l.order_id=o.id JOIN retailers r ON o.retailer_id=r.id WHERE l.serial_hash=$1 FOR UPDATE OF l",
      [hash]);
    const license=rows[0];
    if(!license || !license.active || license.status!=="paid") {
      await client.query("ROLLBACK");
      return send(res,403,{error:"Serial not valid or payment reversed"});
    }
    if(license.bound_device_hash && license.bound_device_hash!==input.deviceHash) {
      await client.query("ROLLBACK");
      return send(res,409,{error:"Licence is registered to another device. Buy a new licence or request a verified transfer."});
    }
    // Same-device reinstallation: requires the purchase serial again, but no new charge.
    await client.query(
      "UPDATE licenses SET bound_device_hash=$1,installation_id=$2,activated_at=now() WHERE id=$3",
      [input.deviceHash,input.installationId,license.id]);
    await client.query("INSERT INTO activation_events(id,license_id,event_type) VALUES($1,$2,$3)",
      [crypto.randomUUID(),license.id,license.installation_id===input.installationId?"refresh":"activation"]);
    await client.query("COMMIT");
    const payload={
      product:"lotto-intelligence",licenseId:license.id,
      deviceHash:input.deviceHash,installationId:input.installationId,
      retailer:license.display_name,retailerSlug:license.slug,
      issuedAt:new Date().toISOString()
    };
    const key=env.LICENSE_PRIVATE_KEY_PEM.replace(/\\n/g,"\n");
    return send(res,200,{activated:true,token:signOfflineLicense(payload,key),retailer:license.display_name});
  } catch(e) {
    await client.query("ROLLBACK").catch(()=>{});
    throw e;
  } finally { client.release(); }
}
async function createRetailer(req,res) {
  if(!admin(req)) return send(res,401,{error:"Admin authorisation required"});
  const input=await body(req);
  const slug=String(input.slug||"").toLowerCase();
  const name=String(input.name||"").trim();
  const color=String(input.color||"#FFDA00");
  const pct=Number(input.commissionPercent??30);
  const receiver=String(input.payfastReceiverId||"");
  const approved=input.splitApproved===true;
  const logo=input.logoUrl?String(input.logoUrl):null;
  if(!/^[a-z0-9-]{2,50}$/.test(slug) || name.length<2 || name.length>80 ||
     !/^#[0-9a-fA-F]{6}$/.test(color) || !Number.isInteger(pct) || pct<0 || pct>80 ||
     (receiver && !/^\d{8}$/.test(receiver)) || (logo && !/^https:\/\//.test(logo)) ||
     (approved && !receiver)) return send(res,400,{error:"Invalid retailer fields"});
  const accessToken=newToken();
  const id=crypto.randomUUID();
  await pool.query(
    "INSERT INTO retailers(id,slug,display_name,brand_color,logo_url,commission_percent,payfast_receiver_id,split_approved,access_token_hash) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9)",
    [id,slug,name,color,logo,pct,receiver||null,approved,tokenHash(accessToken)]);
  send(res,201,{id,slug,storeUrl:`${base}/s/${slug}`,retailerDashboardToken:accessToken,
    note:"Store and token created. Keep the dashboard token private. Live checkout requires verified PayFast accounts."});
}
async function listRetailers(req,res) {
  if(!admin(req)) return send(res,401,{error:"Admin authorisation required"});
  const {rows}=await pool.query("SELECT id,slug,display_name,commission_percent,split_approved,payfast_receiver_id,active,created_at FROM retailers ORDER BY created_at DESC");
  send(res,200,{retailers:rows});
}
async function report(req,res) {
  const supplied=req.headers["x-retailer-token"];
  if(typeof supplied!=="string" || supplied.length<40) return send(res,401,{error:"Retailer key required"});
  const {rows}=await pool.query(
    "SELECT id,slug,display_name,commission_percent FROM retailers WHERE access_token_hash=$1 AND active=true",
    [tokenHash(supplied)]);
  if(!rows[0]) return send(res,401,{error:"Unknown retailer key"});
  const retailer=rows[0];
  const result=await pool.query(
    "SELECT COUNT(*) FILTER (WHERE status='paid')::int AS paid_orders, COUNT(*)::int AS total_orders, COALESCE(SUM(amount_cents) FILTER (WHERE status='paid'),0)::int AS gross_cents, COALESCE(SUM(amount_cents*commission_percent/100) FILTER (WHERE status='paid'),0)::int AS expected_share_cents FROM orders WHERE retailer_id=$1",
    [retailer.id]);
  send(res,200,{retailer:cleanRetailer({...retailer, brand_color:"#FFDA00",logo_url:null}),...result.rows[0],
    disclaimer:"Expected gross allocation before processing fees or chargebacks. Compare actual splits with PayFast statements."});
}
const page=fs.readFileSync(path.join(here,"public","index.html"),"utf8");
async function handler(req,res) {
  try {
    const url=new URL(req.url || "/",base);
    const pathname=url.pathname;
    if(req.method==="GET" && pathname==="/health")
      return send(res,200,{ok:true,mode:sandbox?"sandbox":"live"});
    if(req.method==="GET" && (pathname==="/" || pathname==="/success" || /^\/s\/[a-z0-9-]+$/.test(pathname)))
      return html(res,200,page);
    if(req.method==="GET" && pathname.startsWith("/api/retailers/")) {
      const retailer=await findRetailer(pathname.slice("/api/retailers/".length));
      return retailer?send(res,200,{retailer:cleanRetailer(retailer),price:PRICE_DISPLAY}):
        send(res,404,{error:"Retailer unavailable"});
    }
    if(req.method==="POST" && pathname==="/api/checkout") return await checkout(req,res);
    if(req.method==="POST" && pathname==="/api/payfast/itn") return await itn(req,res);
    if(req.method==="POST" && pathname==="/api/activate") return await activate(req,res);
    const match=pathname.match(/^\/api\/orders\/([0-9a-f-]{36})(\/apk)?$/i);
    if(req.method==="GET" && match)
      return match[2]?await downloadApk(req,res,match[1]):await orderStatus(req,res,match[1]);
    if(req.method==="POST" && pathname==="/api/admin/retailers") return await createRetailer(req,res);
    if(req.method==="GET" && pathname==="/api/admin/retailers") return await listRetailers(req,res);
    if(req.method==="GET" && pathname==="/api/retailer/report") return await report(req,res);
    send(res,404,{error:"Not found"});
  } catch(error) {
    const code=Number(error.status)||500;
    if(code>=500) console.error("Commerce request failed:", error.message);
    send(res,code,{error:code>=500?"Service temporarily unavailable":error.message});
  }
}
const server=http.createServer(handler);
server.listen(Number(env.PORT)||3000,()=>console.log("Lotto Commerce listening; PayFast mode:",sandbox?"SANDBOX":"LIVE"));
