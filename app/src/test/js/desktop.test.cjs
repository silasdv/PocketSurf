const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const kotlin=fs.readFileSync(path.resolve(__dirname,'../../main/java/org/pocketsurf/browser/DesktopBrowsing.kt'),'utf8');
const script=kotlin.match(/val viewportScript="""([\s\S]*?)"""/)[1];
function fixture(withHead=true){let meta=null,notify;const document={head:withHead?{appendChild(v){meta=v;}}:null,querySelector(){return meta;},createElement(){return {name:'',content:''};}};const window={};window.top=window;const context=vm.createContext({window,document,MutationObserver:class{constructor(fn){notify=fn;}observe(){}}});vm.runInContext(script,context);return {window,document,meta:()=>meta,notify:()=>notify(),run:()=>vm.runInContext(script,context)};}
test('desktop layout starts wide and reflows to a smaller CSS viewport when enlarged',()=>{const f=fixture();assert.equal(f.meta().content,'width=1280');f.window.__pocketDesktop.setZoom(200,960);assert.equal(f.window.__pocketDesktop.width,640);assert.equal(f.meta().content,'width=640, initial-scale=1.5');});
test('zoom reset restores layout width instead of magnifying a fixed document',()=>{const f=fixture();f.window.__pocketDesktop.setZoom(150,960);assert.equal(f.window.__pocketDesktop.width,853);f.window.__pocketDesktop.setZoom(100,960);assert.equal(f.meta().content,'width=1280, initial-scale=0.75');});
test('website viewport mutations preserve the selected zoom and injection is idempotent',()=>{const f=fixture();f.window.__pocketDesktop.setZoom(125,800);f.meta().content='width=device-width';f.notify();assert.equal(f.meta().content,'width=1024, initial-scale=0.78125');f.run();assert.equal(f.window.__pocketDesktop.width,1024);});
test('document-start injection waits for the head and clamps zoom boundaries',()=>{const f=fixture(false);assert.equal(f.meta(),null);f.document.head={appendChild(v){f.saved=v;}};f.document.querySelector=()=>f.saved;f.notify();assert.equal(f.saved.content,'width=1280');f.window.__pocketDesktop.setZoom(999,960);assert.equal(f.window.__pocketDesktop.width,512);f.window.__pocketDesktop.setZoom(0,960);assert.equal(f.window.__pocketDesktop.width,2560);});
