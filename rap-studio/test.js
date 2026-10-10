'use strict';
const {spawn} = require('node:child_process');
const http=require('node:http');
const assert=require('node:assert/strict');

const mock = http.createServer(async(req,res)=>{
  if(req.url==='/health') return reply({data:{status:'ok'},code:200});
  if(req.url==='/release_task'){
    let payload='';for await(const chunk of req)payload+=chunk;
    const reqBody=JSON.parse(payload);
    assert.match(reqBody.prompt,/hip.hop/i);assert.equal(reqBody.bpm,94);
    return reply({data:{task_id:'demo-job-abc123'},code:200});
  }
  if(req.url==='/query_result'){
    let payload='';for await(const chunk of req)payload+=chunk;
    assert.deepEqual(JSON.parse(payload).task_id_list,['demo-job-abc123']);
    return reply({data:[{task_id:'demo-job-abc123',status:1,result:JSON.stringify([{file:'/v1/audio?path=%2Ftmp%2Fdemo.mp3'}])}],code:200});
  }
  if(req.url.startsWith('/v1/audio?path=')){
    res.writeHead(200,{'Content-Type':'audio/mpeg'});return res.end(Buffer.from([73,68,51,3,0,0]));
  }
  return reply({error:'Not found'},404);
  function reply(data,status=200){res.writeHead(status,{'Content-Type':'application/json'});res.end(JSON.stringify(data));}
});
async function run(){
  await new Promise(r=>mock.listen(44918,'127.0.0.1',r));
  const child=spawn(process.execPath,['server.js'],{
    env:{...process.env,PORT:'44919',ACESTEP_URL:'http://127.0.0.1:44918',ACESTEP_API_KEY:'test-secret'},
    stdio:'pipe', cwd:__dirname
  });
  const origin='http://127.0.0.1:44919';
  try{
    let health;
    for(let i=0;i<40;i++){
      try{health=await(await fetch(origin+'/api/health')).json();break}catch{await new Promise(r=>setTimeout(r,100))}
    }
    assert.deepEqual(health,{configured:true,online:true});
    const home=await(await fetch(origin+'/')).text();assert.match(home,/Stella Rap Studio/);
    const song=await(await fetch(origin+'/api/generate',{
      method:'POST',headers:{'Content-Type':'application/json'},
      body:JSON.stringify({prompt:'motivational hip-hop',lyrics:'Build from nothing',duration:60,bpm:94})
    })).json();
    assert.equal(song.taskId,'demo-job-abc123');
    const result=await(await fetch(origin+'/api/status?id='+song.taskId)).json();
    assert.equal(result.status,'done');
    const audio=await fetch(origin+result.audio);
    assert.equal(audio.status,200);
    assert.equal(audio.headers.get('content-type'),'audio/mpeg');
    assert.equal((await audio.arrayBuffer()).byteLength,6);
    const invalid=await fetch(origin+'/api/status?id=!!');
    assert.equal(invalid.status,400);
    console.log('PASS: landing page, live engine, music generation, job polling, audio stream, validation');
  }finally{child.kill();mock.close()}
}
run().catch(e=>{console.error(e);mock.close();process.exitCode=1});
