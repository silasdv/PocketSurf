import {readFileSync} from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
import test from 'node:test';

// Execute the shipped Kotlin template, replacing only its two quoted arguments.
const source=readFileSync(new URL('../app/src/main/java/org/pocketsurf/browser/GeneratedFileTransfer.kt',import.meta.url),'utf8');
const template=source.match(/fun script\(url:String,token:String\):String="""([\s\S]*?)"""/)[1];
function fixture(read,fail=false){
 const token='test-token';const messages=[];let listener,cancelled=false,closed=false;
 const port={onmessage:null,postMessage(raw){const item=JSON.parse(raw);messages.push(item);if(messages.length>10)messages.shift()},close(){closed=true}};
 const window={addEventListener(_,callback){listener=callback},removeEventListener(_,callback){if(listener===callback)listener=null}};
 const fetch=async()=>{if(fail)throw Error('Network error');return {ok:true,body:{getReader:()=>({read,cancel:async()=>{cancelled=true}})}}};
 vm.runInNewContext(template.replace('${JSONObject.quote(token)}',JSON.stringify(token)).replace('${JSONObject.quote(url)}',JSON.stringify('blob:https://fixture.test/generated')),{window,fetch,Uint8Array,setTimeout(){},btoa:x=>Buffer.from(x,'binary').toString('base64')});
 return {port,messages,get listener(){return listener},get cancelled(){return cancelled},get closed(){return closed},receive(value=token){listener?.({data:value,ports:[port]})}};
}
const flush=()=>new Promise(resolve=>setImmediate(resolve));
test('generated download uses a bounded acknowledged stream with exact bytes',async()=>{
 const bytes=Uint8Array.from({length:70000},(_,i)=>i%251);let readCount=0;
 const f=fixture(async()=>readCount++?{done:true}:{done:false,value:bytes});f.receive();await flush();
 assert.equal(f.messages.length,1);assert.equal(readCount,1);assert.equal(Buffer.from(f.messages[0].chunk,'base64').length,32768);
 await flush();assert.equal(f.messages.length,1,'No second chunk before native acknowledgement');
 await f.port.onmessage();await f.port.onmessage();assert.equal(f.messages.length,3);
 assert.deepEqual(Buffer.concat(f.messages.map(m=>Buffer.from(m.chunk,'base64'))),Buffer.from(bytes));
 await f.port.onmessage();assert.deepEqual(f.messages.at(-1),{done:true});assert.ok(f.closed);assert.equal(f.listener,null);
});
test('a message with a different token cannot start a transfer',async()=>{let reads=0;const f=fixture(async()=>{reads++;return {done:true}});f.receive('another-token');await flush();assert.equal(reads,0);assert.equal(f.messages.length,0);f.receive();await flush();assert.deepEqual(f.messages,[{done:true}])});
test('fetch failure becomes an explicit failed result',async()=>{const f=fixture(async()=>({done:true}),true);f.receive();await flush();assert.ok(f.messages.at(-1).error);assert.ok(f.closed);assert.ok(!f.messages.some(m=>m.done))});
test('an unbounded website stream is cancelled at the 64 MiB limit',async()=>{let reads=0;const piece=new Uint8Array(32768);const f=fixture(async()=>{reads++;return {done:false,value:piece}});f.receive();await flush();for(let i=0;i<2050&&!f.closed;i++)await f.port.onmessage();assert.equal(reads,2049);assert.equal(f.messages.at(-1).error,'limit');assert.ok(f.cancelled);assert.ok(f.closed);assert.ok(f.messages.at(-1).error);assert.ok(!f.messages.some(m=>m.done))});
