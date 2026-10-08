const $=x=>document.getElementById(x), params=new URLSearchParams(location.search);
const store=location.pathname.startsWith("/s/")?location.pathname.slice(3):"";
async function data(url,opts={}){const r=await fetch(url,opts);const j=await r.json();if(!r.ok)throw Error(j.error||"Service unavailable");return j}
async function loadStore(){
 if(!store)return;
 try{const j=await data("/api/retailers/"+encodeURIComponent(store));
 document.documentElement.style.setProperty("--accent",j.retailer.color);
 $("retailer").textContent="Available from "+j.retailer.name;
 }catch(e){$("buy").disabled=true;$("message").textContent=e.message}
}
$("purchase").onsubmit=async e=>{
 e.preventDefault();$("buy").disabled=true;$("message").textContent="";
 try{
  const j=await data("/api/checkout",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({retailer:store,email:$("email").value,ageConfirmed:$("adult").checked})});
  sessionStorage.setItem("lotto:"+j.orderId,j.accessToken);
  const f=document.createElement("form");f.action=j.paymentUrl;f.method="POST";
  for(const [k,v] of Object.entries(j.paymentFields)){const input=document.createElement("input");input.type="hidden";input.name=k;input.value=v;f.appendChild(input)}
  document.body.appendChild(f);f.submit();
 }catch(err){$("message").textContent=err.message;$("buy").disabled=false}
};
const order=params.get("order");
async function receipt(){
 $("shop").hidden=true;$("receipt").hidden=false;
 const access=sessionStorage.getItem("lotto:"+order);
 if(!access){$("status").textContent="To recover a serial, open the payment receipt on the same browser used during checkout or contact support with the order reference.";return}
 try{
  const j=await data("/api/orders/"+encodeURIComponent(order),{headers:{"X-Order-Token":access}});
  if(j.status!=="paid"){$("status").textContent="PayFast confirmation is pending. Use Refresh to check again.";return}
  $("status").textContent="Payment confirmed · "+j.retailer;
  $("serial").textContent=j.serial;$("paid").hidden=false;
  $("copy").onclick=()=>navigator.clipboard.writeText(j.serial);
  $("download").disabled=!j.downloadReady;
  $("download").onclick=async()=>{
   try{const r=await fetch("/api/orders/"+encodeURIComponent(order)+"/apk",{headers:{"X-Order-Token":access}});
    if(!r.ok)throw Error("APK not yet available");const blob=await r.blob(),url=URL.createObjectURL(blob);
    const a=document.createElement("a");a.href=url;a.download="Lotto-Intelligence.apk";a.click();setTimeout(()=>URL.revokeObjectURL(url),30000);
   }catch(err){$("status").textContent=err.message}
  };
 }catch(err){$("status").textContent=err.message}
}
if(order)receipt();else loadStore();
