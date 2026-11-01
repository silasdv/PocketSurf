import {createRequire} from "node:module"; const require=createRequire(import.meta.url);
const fs=require('node:fs');const path=require('node:path');const http=require('node:http');const {spawn}=require('node:child_process');
const base=path.resolve(import.meta.dirname,'..');const work=path.join(base,'build','desktop-scroll');fs.mkdirSync(work,{recursive:true});
const html=`<!doctype html><meta charset="utf-8"><title>PocketSurf large feed fixture</title><style>html{scroll-behavior:auto}body{margin:0;background:#101820;color:white}article{height:120px;contain:layout paint;border-bottom:1px solid #345}</style><main id="feed"></main><script>function appendPosts(count){const fragment=document.createDocumentFragment();for(let i=0;i<count;i++){const p=document.createElement('article');const a=document.createElement('a');a.href='#post-'+i;a.textContent='Post '+i;p.append(a);fragment.append(p)}document.getElementById('feed').append(fragment)}appendPosts(20000)</script>`;
const server=http.createServer((req,res)=>{res.setHeader('content-type','text/html');res.end(html)});await new Promise(r=>server.listen(0,'127.0.0.1',r));
const profile=path.join(work,'profile');fs.mkdirSync(profile,{recursive:true});const stalePort=path.join(profile,'DevToolsActivePort');if(fs.existsSync(stalePort))fs.unlinkSync(stalePort);const chrome=spawn(process.env.CHROME_BINARY||'C:/Program Files/Google/Chrome/Application/chrome.exe',['--headless=new','--no-sandbox','--disable-gpu','--no-first-run','--no-default-browser-check','--disable-background-networking','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank'],{windowsHide:true,stdio:['ignore','ignore','pipe']});
chrome.stderr.on("data",x=>process.stderr.write(x));let ws;try{
 const portFile=path.join(profile,'DevToolsActivePort');for(let i=0;i<100&&!fs.existsSync(portFile);i++)await new Promise(r=>setTimeout(r,100));if(!fs.existsSync(portFile))throw Error('Chrome debugging port unavailable');const port=fs.readFileSync(portFile,'utf8').split('\n')[0];
 const targets=await fetch(`http://127.0.0.1:${port}/json/list`).then(r=>r.json());const target=targets.find(t=>t.type==='page');ws=new WebSocket(target.webSocketDebuggerUrl);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j});let seq=0;const pending=new Map();ws.onmessage=e=>{const v=JSON.parse(e.data);if(v.id){const cb=pending.get(v.id);pending.delete(v.id);if(v.error)cb.reject(Error(JSON.stringify(v.error)));else cb.resolve(v.result)}};
 const send=(method,params={})=>new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}))});
 const evaluate=async expression=>{const r=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true,userGesture:true});if(r.exceptionDetails)throw Error(JSON.stringify(r.exceptionDetails));return r.result.value};
 await send('Page.enable');await send('Page.navigate',{url:`http://127.0.0.1:${server.address().port}/`});for(let i=0;i<60;i++){if(await evaluate('document.readyState==="complete"&&document.getElementById("feed").children.length===20000'))break;await new Promise(r=>setTimeout(r,50))}
 await evaluate(fs.readFileSync(path.join(base,'app/src/main/assets/navigation.js'),'utf8'));
 const result=await evaluate(`(async()=>{
 const sleep=ms=>new Promise(r=>setTimeout(r,ms));
 // Install probes after fixture creation and shipped-script injection. Count all root scopes.
 let selectorCalls=0,styleCalls=0;const originalStyle=window.getComputedStyle;
 window.getComputedStyle=function(...args){styleCalls++;return originalStyle.apply(this,args)};
 for(const type of [Document,Element,ShadowRoot]){const original=type.prototype.querySelectorAll;type.prototype.querySelectorAll=function(...args){selectorCalls++;return original.apply(this,args)}}
 const samples=[];let sampling=true;function sample(){samples.push({ms:performance.now(),y:scrollY});if(sampling)requestAnimationFrame(sample)}
 requestAnimationFrame(sample);const started=performance.now();const startY=scrollY;
 window.__pocketSurf.setScroll(0,1800);await sleep(500);appendPosts(1000);await sleep(500);
 window.__pocketSurf.setScroll(0,0);const stopY=scrollY,elapsedMs=performance.now()-started;
 await sleep(150);sampling=false;const finalY=scrollY;
 const deltas=samples.slice(1).map((s,i)=>s.ms-samples[i].ms);
 return {userAgent:navigator.userAgent,scope:'Desktop Chrome real wall-clock RAF and layout; synthetic growing feed, not Reddit or Android device FPS',posts:document.getElementById('feed').children.length,elapsedMs,startY,stopY,finalY,distance:stopY-startY,selectorCalls,styleCalls,sampleCount:samples.length,maxSampleGapMs:Math.max(0,...deltas),stopStable:Math.abs(finalY-stopY)<1,progressMonotonic:samples.every((s,i)=>!i||s.y>=samples[i-1].y),samples};})()`);
 fs.writeFileSync(path.join(work,'scroll-result.json'),JSON.stringify(result,null,2));
 const {samples,...summary}=result;console.log(JSON.stringify(summary,null,2));
 if(result.posts!==21000||result.selectorCalls!==0||result.distance<=300||!result.stopStable||!result.progressMonotonic)throw Error('Continuous scroll semantic checks failed');
 await send('Browser.close');
}finally{if(ws)ws.close();chrome.kill();server.close()}
