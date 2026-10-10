// Exact finite-sample probability engine. No prediction or historical weighting.
export const VERIFIED='2026-10-10';
export const GAMES=[
 {id:'daily',name:'Daily Lotto',n:36,k:5,m:0,j:0,bonus:false,price:3,entry:3,tag:'Daily',note:'5 numbers from 36'},
 {id:'lotto',name:'Lotto',n:52,k:6,m:0,j:0,bonus:true,price:5,entry:5,tag:'Wed / Sat',note:'6 numbers from 52'},
 {id:'plus1',name:'Lotto Plus 1',n:52,k:6,m:0,j:0,bonus:true,price:2.5,entry:7.5,tag:'Wed / Sat',note:'Includes required Lotto entry'},
 {id:'max',name:'Lotto 5 Max',n:52,k:6,m:0,j:0,bonus:true,price:2.5,entry:10,tag:'Wed / Sat',note:'Includes Lotto and Plus 1 entries'},
 {id:'power',name:'PowerBall',n:50,k:5,m:16,j:1,bonus:false,price:10,entry:10,tag:'Tue / Fri',note:'5 from 50 + 1 from 16'},
 {id:'xtra',name:'PowerBall Xtra',n:50,k:5,m:16,j:1,bonus:false,price:5,entry:15,tag:'Tue / Fri',note:'Includes required PowerBall entry'}
];
export function choose(n,k){if(!Number.isInteger(n)||!Number.isInteger(k)||n<0||k<0||k>n)return 0n;k=Math.min(k,n-k);let c=1n;for(let i=1;i<=k;i++)c=c*BigInt(n-k+i)/BigInt(i);return c;}
export function validateGame(g){for(const key of ['n','k','m','j'])if(!Number.isInteger(g[key]))throw Error('Game sizes must be whole numbers.');if(g.n<2||g.n>100||g.k<1||g.k>10||g.k>g.n)throw Error('Choose a main pool of 2–100 and 1–10 numbers.');if(g.m<0||g.m>50||g.j<0||g.j>5||g.j>g.m||(g.m===0)!==(g.j===0))throw Error('Extra pool must be 0 with 0 picks, or 1–50 with 1–5 picks.');if(g.bonus&&(g.m||g.n===g.k))throw Error('Same-pool bonus requires spare main balls and no separate extra pool.');return g;}
export function outcomes(g){return choose(g.n,g.k)*(g.m?choose(g.m,g.j):1n);}
export function matchProbability(n,k,r){return Number(choose(k,r)*choose(n-k,k-r))/Number(choose(n,k));}
export function distribution(g){validateGame(g);const rows=[];for(let r=g.k;r>=0;r--){const main=matchProbability(g.n,g.k,r);if(g.m){for(let s=g.j;s>=0;s--){rows.push({r,s,label:`${r} main + ${s} extra`,p:main*matchProbability(g.m,g.j,s)});}}else if(g.bonus){const b=(g.k-r)/(g.n-g.k);if(b>0)rows.push({r,s:1,label:`${r} main + bonus`,p:main*b});if(b<1)rows.push({r,s:0,label:`${r} main, no bonus`,p:main*(1-b)});}else rows.push({r,s:0,label:`${r} main`,p:main});}return rows;}
export function targetProbability(g,r,extra=false){let p=0;for(let i=r;i<=g.k;i++)p+=matchProbability(g.n,g.k,i);return p*(extra&&g.m?1/Number(choose(g.m,g.j)):1);}
export function repeated(p,d){if(p<=0)return 0;if(p>=1)return 1;return -Math.expm1(d*Math.log1p(-p));}
export function wilson(hits,n){const z=1.959963984540054,p=hits/n,d=1+z*z/n,c=(p+z*z/(2*n))/d,h=z*Math.sqrt(p*(1-p)/n+z*z/(4*n*n))/d;return [hits===0?0:Math.max(0,c-h),hits===n?1:Math.min(1,c+h)];}
export function randomInt(max){if(!Number.isInteger(max)||max<1||max>0x100000000)throw Error('Invalid random range.');const a=new Uint32Array(1),limit=Math.floor(0x100000000/max)*max;let x;do{globalThis.crypto.getRandomValues(a);x=a[0];}while(x>=limit);return x%max;}
export function sample(pool,k,rng=randomInt){const p=[...pool];for(let i=0;i<k;i++){const j=i+rng(p.length-i);[p[i],p[j]]=[p[j],p[i]];}return p.slice(0,k).sort((a,b)=>a-b);}
export function numbers(text,n){if(!text.trim())return [];const parts=text.trim().split(/[\s,;]+/);const a=parts.map(Number);if(a.some(x=>!Number.isInteger(x)||x<1||x>n))throw Error(`Enter whole numbers from 1 to ${n}, separated by spaces or commas.`);if(new Set(a).size!==a.length)throw Error('Remove repeated numbers from your input.');return a.sort((a,b)=>a-b);}
export function key(line){return line.main.join(',')+'|'+line.extra.join(',');}
export function validateLine(g,line){if(!line||!Array.isArray(line.main)||!Array.isArray(line.extra)||line.main.length!==g.k||line.extra.length!==g.j)throw Error('A saved line has the wrong number of balls.');for(const [a,n] of [[line.main,g.n],[line.extra,g.m]])if(a.some(x=>!Number.isInteger(x)||x<1||x>n)||new Set(a).size!==a.length)throw Error('A saved line contains invalid numbers.');return line;}
export function generate(g,count,{locked=[],excluded=[],strategy='random'}={},rng=randomInt){validateGame(g);if(!Number.isInteger(count)||count<1||count>200)throw Error('Choose 1–200 lines.');if(locked.length>g.k||locked.some(x=>excluded.includes(x)))throw Error('Locked numbers cannot be excluded or exceed the line size.');for(const a of [locked,excluded])if(new Set(a).size!==a.length||a.some(x=>!Number.isInteger(x)||x<1||x>g.n))throw Error('Invalid locked or excluded numbers.');const pool=Array.from({length:g.n},(_,i)=>i+1).filter(x=>!locked.includes(x)&&!excluded.includes(x));const take=g.k-locked.length;const available=choose(pool.length,take)*(g.m?choose(g.m,g.j):1n);if(BigInt(count)>available)throw Error(`These settings allow only ${available} unique lines.`);const extraPool=Array.from({length:g.m},(_,i)=>i+1),lines=[],seen=new Set();const candidate=()=>({main:[...locked,...sample(pool,take,rng)].sort((a,b)=>a-b),extra:sample(extraPool,g.j,rng)});
if(strategy==='match4'){
  if(g.n!==36||g.k!==5||g.m!==0||g.j!==0)throw Error('Four-match coverage is currently available for Daily Lotto 5/36 only.');
  if(locked.length||excluded.length)throw Error('Four-match coverage needs the full 1–36 pool. Remove locked/excluded numbers.');
  if(count>100)throw Error('Four-match coverage supports up to 100 lines.');
  while(lines.length<count){
    let found=false;
    for(let trial=0;trial<25000;trial++){
      const c=candidate();
      if(seen.has(key(c))||lines.some(l=>l.main.filter(x=>c.main.includes(x)).length>2))continue;
      seen.add(key(c));lines.push(c);found=true;break;
    }
    if(!found)throw Error('Could not complete low-overlap coverage. Try fewer lines.');
  }
  return lines;
}
let attempts=0;while(lines.length<count){let best=null,bestScore=Infinity;for(let t=0;t<(strategy==='spread'?32:1);t++){const c=candidate();if(seen.has(key(c)))continue;const score=strategy==='spread'?lines.reduce((a,l)=>a+Math.pow(l.main.filter(x=>c.main.includes(x)).length,2),0):0;if(score<bestScore){best=c;bestScore=score;}}if(best){seen.add(key(best));lines.push(best);}if(++attempts>100000)throw Error('Selection space is almost exhausted. Request fewer lines or remove locks.');}return lines;}
export function* combinations(pool,k,start=0,prefix=[]){if(k===0){yield prefix;return;}for(let i=start;i<=pool.length-k;i++)yield* combinations(pool,k-1,i+1,[...prefix,pool[i]]);}
export function wheel(g,pool,extra=[]){if(pool.length<g.k||pool.length>14)throw Error(`Choose ${g.k}–14 wheel numbers.`);if(new Set(pool).size!==pool.length||pool.some(x=>!Number.isInteger(x)||x<1||x>g.n))throw Error('Invalid wheel pool.');const total=choose(pool.length,g.k);if(total>200n)throw Error(`This full wheel needs ${total} lines. Maximum is 200; reduce the pool.`);const lines=[...combinations(pool,g.k)].map(main=>({main,extra:[...extra]}));lines.forEach(l=>validateLine(g,l));return lines;}
export function uniqueLines(lines){return [...new Map(lines.map(l=>[key(l),l])).values()];}
export function portfolioStats(g,lines){const unique=uniqueLines(lines),frequency=Array(g.n).fill(0);unique.forEach(l=>l.main.forEach(n=>frequency[n-1]++));let overlap=0,pairs=0;for(let i=0;i<unique.length;i++)for(let j=i+1;j<unique.length;j++){overlap+=unique[i].main.filter(x=>unique[j].main.includes(x)).length;pairs++;}return {unique:unique.length,jackpot:unique.length/Number(outcomes(g)),frequency,coverage:frequency.filter(Boolean).length,overlap:pairs?overlap/pairs:0};}
export function checkDraw(g,lines,main,extra=[],bonus=null){validateLine(g,{main,extra});if(g.bonus&&bonus!==null&&(!Number.isInteger(bonus)||bonus<1||bonus>g.n||main.includes(bonus)))throw Error('Bonus must be in range and different from the main draw.');return lines.map(l=>({main:l.main.filter(x=>main.includes(x)).length,extra:l.extra.filter(x=>extra.includes(x)).length,bonus:g.bonus&&bonus!==null&&l.main.includes(bonus)}));}
export function analyzeHistory(g,text){const rows=text.trim().split(/\r?\n/).filter(x=>x.trim());if(!rows.length||rows.length>10000)throw Error('Paste 1–10,000 draws.');const freq=Array(g.n).fill(0);for(let i=0;i<rows.length;i++){const a=numbers(rows[i],g.n);if(a.length!==g.k)throw Error(`Row ${i+1}: expected ${g.k} main numbers, without dates or bonus balls.`);a.forEach(x=>freq[x-1]++);}const q=g.k/g.n,mean=rows.length*q,sd=Math.sqrt(rows.length*q*(1-q));return {draws:rows.length,mean,values:freq.map((count,i)=>({n:i+1,count,z:sd?(count-mean)/sd:0})).sort((a,b)=>b.count-a.count||a.n-b.n)};}
export function formatPct(p){if(p===0)return '0%';if(p<0.00000001)return (p*100).toExponential(3)+'%';return (p*100).toLocaleString('en-ZA',{maximumFractionDigits:8})+'%';}
export function odds(p){return p? '1 in '+(1/p).toLocaleString('en-ZA',{maximumFractionDigits:2}):'Impossible';}
