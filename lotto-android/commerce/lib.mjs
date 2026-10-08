import crypto from "node:crypto";

export const PRICE_CENTS = 5000;
export const PRICE_DISPLAY = "50.00";
const HEX = /^[0-9a-f]{64}$/i;

export function validHash(value) {
  return typeof value === "string" && HEX.test(value);
}
export function validInstallationId(value) {
  return typeof value === "string" && /^[0-9a-f-]{36}$/i.test(value);
}
export function generateSerial() {
  const hex = crypto.randomBytes(12).toString("hex").toUpperCase();
  return "LTI-" + hex.match(/.{1,6}/g).join("-");
}
export function serialHash(serial, key) {
  if (!key || key.length < 32) throw Error("SERIAL_HMAC_KEY must be at least 32 chars");
  return crypto.createHmac("sha256", key).update(serial.trim().toUpperCase()).digest("hex");
}
export function tokenHash(token) {
  return crypto.createHash("sha256").update(token).digest("hex");
}
export function newToken() {
  return crypto.randomBytes(32).toString("hex");
}
export function encryptSerial(serial, keyB64) {
  const key = Buffer.from(keyB64 || "", "base64");
  if (key.length !== 32) throw Error("SERIAL_ENCRYPTION_KEY_B64 must contain 32 bytes");
  const iv = crypto.randomBytes(12);
  const enc = crypto.createCipheriv("aes-256-gcm", key, iv);
  const ciphertext = Buffer.concat([enc.update(serial, "utf8"), enc.final()]);
  return [iv, enc.getAuthTag(), ciphertext].map(x => x.toString("base64url")).join(".");
}
export function decryptSerial(value, keyB64) {
  const key = Buffer.from(keyB64 || "", "base64");
  const [iv, tag, ciphertext] = value.split(".").map(x => Buffer.from(x, "base64url"));
  if (key.length !== 32 || iv.length !== 12 || tag.length !== 16) throw Error("Invalid encrypted serial");
  const dec = crypto.createDecipheriv("aes-256-gcm", key, iv);
  dec.setAuthTag(tag);
  return Buffer.concat([dec.update(ciphertext), dec.final()]).toString("utf8");
}
function payFastEncode(value) {
  return encodeURIComponent(String(value).trim())
    .replace(/%20/g, "+")
    .replace(/~/g, "%7E")
    .replace(/%[a-f0-9]{2}/gi, m => m.toUpperCase())
    .replace(/[!'()*]/g, c => "%" + c.charCodeAt(0).toString(16).toUpperCase());
}
export function payFastParamString(pairs, passphrase) {
  const entries = Array.isArray(pairs) ? pairs : Object.entries(pairs);
  const filtered = entries.filter(([name, value]) =>
    name !== "signature" && name !== "setup" && value !== null &&
    value !== undefined && String(value).trim() !== ""
  );
  let joined = filtered.map(([key, value]) => `${key}=${payFastEncode(value)}`).join("&");
  if (passphrase) joined += `&passphrase=${payFastEncode(passphrase)}`;
  return joined;
}
export function payFastSignature(pairs, passphrase) {
  return crypto.createHash("md5").update(payFastParamString(pairs, passphrase)).digest("hex");
}
export function timingSafeEqualHex(a, b) {
  if (!/^[a-f0-9]{32,128}$/i.test(a || "") || !/^[a-f0-9]{32,128}$/i.test(b || "")) return false;
  const x = Buffer.from(a, "hex"), y = Buffer.from(b, "hex");
  return x.length === y.length && crypto.timingSafeEqual(x, y);
}
export function verifyPaymentData(fields, order, merchantId) {
  const cents = Math.round(Number(fields.amount_gross) * 100);
  return fields.payment_status === "COMPLETE" &&
    fields.m_payment_id === order.id &&
    fields.merchant_id === merchantId &&
    Number.isFinite(cents) && cents === order.amount_cents &&
    /^\d+$/.test(String(fields.pf_payment_id || ""));
}
export function signOfflineLicense(payload, privateKey) {
  if (!privateKey) throw Error("LICENSE_PRIVATE_KEY_PEM missing");
  const p64 = Buffer.from(JSON.stringify(payload)).toString("base64url");
  const signature = crypto.sign("RSA-SHA256", Buffer.from(p64), privateKey).toString("base64url");
  return p64 + "." + signature;
}
export function verifyOfflineLicense(token, publicKey, deviceHash, installId) {
  try {
    const [p64, sig] = token.split(".");
    if (!p64 || !sig) return false;
    if (!crypto.verify("RSA-SHA256", Buffer.from(p64), publicKey, Buffer.from(sig, "base64url"))) return false;
    const body = JSON.parse(Buffer.from(p64, "base64url").toString("utf8"));
    return body.deviceHash === deviceHash && body.installationId === installId && body.product === "lotto-intelligence";
  } catch { return false; }
}
