'use strict';
/*
 * Stella Rap Studio — no third-party Node dependencies.
 * Supply ACESTEP_URL and optionally ACESTEP_API_KEY to connect an ACE-Step v1.5 server.
 */
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');

const PORT = Number(process.env.PORT || 3000);
const BASE = (process.env.ACESTEP_URL || '').replace(/\/+$/, '');
const TOKEN = process.env.ACESTEP_API_KEY || '';
const HTML = path.join(__dirname, 'index.html');
const quotas = new Map();

function json(res, code, payload) {
  res.writeHead(code, {'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});
  res.end(JSON.stringify(payload));
}
function safeError(err) { return String(err?.message || err).slice(0,350); }
function throttle(ip) {
  const now = Date.now();
  const record = quotas.get(ip) || [];
  const times = record.filter(t => now-t < 20*60*1000);
  if (times.length >= 3) return false;
  times.push(now);
  quotas.set(ip, times);
  if (quotas.size > 5000) quotas.clear();
  return true;
}
async function bodyJson(req) {
  let str = '';
  for await(const chunk of req) {
    str += chunk;
    if(str.length > 24000) throw Error('Request is too large');
  }
  try {return JSON.parse(str)} catch {throw Error('Invalid JSON')}
}
async function upstream(endpoint, payload, method='POST') {
  if (!BASE) throw Error('Music model is not configured: set ACESTEP_URL');
  const options = {
    method,
    headers: {
      ...(payload ? {'Content-Type':'application/json'} : {}),
      ...(TOKEN ? {'Authorization':'Bearer '+TOKEN} : {})
    },
    signal: AbortSignal.timeout(45000),
    ...(payload ? {body:JSON.stringify(payload)} : {})
  };
  const response = await fetch(BASE+endpoint,options);
  if (!response.ok) throw Error('ACE-Step returned HTTP '+response.status);
  const result = await response.json();
  if(result.code && result.code !== 200) throw Error(result.error || 'Model request rejected');
  return result.data;
}
const server = http.createServer(async (req,res) => {
  const pathname = new URL(req.url,'http://localhost').pathname;
  try {
    if(req.method === 'GET' && pathname === '/') {
      res.writeHead(200,{'Content-Type':'text/html; charset=utf-8','Cache-Control':'no-store'});
      return fs.createReadStream(HTML).pipe(res);
    }
    if(req.method === 'GET' && pathname === '/api/health') {
      if(!BASE) return json(res,200,{configured:false,online:false});
      try {
        const result=await upstream('/health',null,'GET');
        return json(res,200,{configured:true,online:result?.status==='ok'});
      } catch(e) {return json(res,200,{configured:true,online:false,message:safeError(e)})}
    }
    if(req.method === 'POST' && pathname === '/api/generate') {
      if (!BASE) return json(res,503,{error:'Connect an ACE-Step 1.5 server with ACESTEP_URL before generating AI music.'});
      if (!throttle(req.socket.remoteAddress||'unknown')) return json(res,429,{error:'Three songs per 20 minutes allowed from this connection'});
      const input=await bodyJson(req);
      const prompt=String(input.prompt || '').trim().slice(0,1600);
      const lyrics=String(input.lyrics || '').trim().slice(0,14000);
      const duration=Number(input.duration ?? 60);
      const bpm=Number(input.bpm ?? 94);
      if(!prompt || !lyrics || ![30,60,90].includes(duration) || !Number.isInteger(bpm) || bpm<50 || bpm>180)
        return json(res,400,{error:'Supply a prompt, lyrics, valid duration and BPM between 50 and 180'});
      const data=await upstream('/release_task',{
        prompt,lyrics, audio_duration:duration,bpm,vocal_language:'en',
        audio_format:'mp3',time_signature:'4',thinking:false
      });
      const taskId=data?.task_id || data?.taskId;
      if(!taskId) throw Error('Model server did not return a task ID');
      return json(res,200,{taskId});
    }
    if(req.method === 'GET' && pathname === '/api/status') {
      const taskId=new URL(req.url,'http://localhost').searchParams.get('id')||'';
      if(!/^[a-z0-9-]{6,100}$/i.test(taskId)) return json(res,400,{error:'Invalid task ID'});
      const result=await upstream('/query_result',{task_id_list:[taskId]});
      const task=Array.isArray(result)?result[0]:null;
      if(!task) return json(res,200,{status:'queued'});
      if(task.status === 2) return json(res,200,{status:'failed',error:task.error||'Generation failed'});
      if(task.status !== 1) return json(res,200,{status:'working'});
      let tracks=typeof task.result==='string'?JSON.parse(task.result):task.result;
      if(!Array.isArray(tracks)) tracks=[];
      const file=tracks.find(x=>typeof x.file==='string' && x.file.startsWith('/v1/audio?path='));
      if(!file) throw Error('The model finished but did not provide a downloadable audio file');
      return json(res,200,{status:'done',audio:'/api/audio?src='+encodeURIComponent(file.file)});
    }
    if(req.method === 'GET' && pathname === '/api/audio') {
      const src = new URL(req.url,'http://localhost').searchParams.get('src') || '';
      const target=new URL(src,'http://acestep.invalid');
      if(target.origin !== 'http://acestep.invalid' || target.pathname !== '/v1/audio' || !target.searchParams.has('path'))
        return json(res,400,{error:'Invalid audio path'});
      if(!BASE) return json(res,503,{error:'ACE-Step is not configured'});
      const r=await fetch(BASE+target.pathname+target.search,{headers:TOKEN?{Authorization:'Bearer '+TOKEN}:{},signal:AbortSignal.timeout(120000)});
      if(!r.ok) return json(res,502,{error:'Could not retrieve generated audio'});
      res.writeHead(200,{'Content-Type':'audio/mpeg','Content-Disposition':'attachment; filename="rap-studio-song.mp3"','Cache-Control':'private, no-store'});
      const {Readable}=require('node:stream');
      Readable.fromWeb(r.body).pipe(res);
      return;
    }
    json(res,404,{error:'Not found'});
  } catch(err) {
    if(!res.headersSent) json(res,502,{error:safeError(err)});
    else res.destroy(err);
  }
});
server.listen(PORT,()=>console.log('Stella Rap Studio listening on port '+PORT));
