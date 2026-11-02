const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.resolve(__dirname, '../../main/assets/navigation.js'), 'utf8');

// Controlled geometry and virtual animation clock execute the shipped source, not a second algorithm.
function fixture() {
  const state = {elements:[], focused:null, pageScroll:0, scrollX:0, scrollY:0, now:0, ring:null, queries:0, styles:0, hits:0};
  const listeners=new Map(), frames=new Map();let frameId=0;
  function listen(kind,fn){const list=listeners.get(kind)||[];list.push(fn);listeners.set(kind,list);}
  const document = {
    activeElement:null, hidden:false,
    documentElement:{appendChild(){},scrollHeight:2000},body:{appendChild(){}},
    createElement(){const e={style:{},setAttribute(){}};state.ring=e;return e;},
    querySelectorAll(selector){state.queries++;return selector==='*'?state.elements:state.elements.filter(e=>e.interactive)},
    elementFromPoint(x,y){state.hits++;return [...state.elements].reverse().find(e=>e.isConnected&&e.visible&&x>=e.rect.x&&x<=e.rect.right&&y>=e.rect.y&&y<=e.rect.bottom)||null},
    addEventListener:listen
  };
  const window={scrollBy({left=0,top=0}){state.pageScroll++;state.scrollX+=left;state.scrollY+=top;}};
  const context={window,document,innerWidth:800,innerHeight:500,performance:{now:()=>state.now},
    MutationObserver:class{observe(){}},addEventListener:listen,
    requestAnimationFrame(fn){const id=++frameId;frames.set(id,fn);return id;},cancelAnimationFrame(id){frames.delete(id);},
    getComputedStyle:e=>(state.styles++,{display:e.visible?'block':'none',visibility:'visible',opacity:'1',overflowY:e.scrollable?'auto':'visible',overflowX:e.horizontal?'auto':'visible'})};
  vm.runInNewContext(source,context);
  function element(name,x,y,width=100,height=40,options={}) {
    const e={name,tagName:'BUTTON',textContent:name,type:'',interactive:true,isConnected:true,visible:true,disabled:false,readOnly:false,parentElement:null,
      rect:{x,y,left:x,top:y,width,height,right:x+width,bottom:y+height},scrollLeft:0,scrollTop:0,scrollHeight:0,clientHeight:0,scrollWidth:0,clientWidth:0,
      getBoundingClientRect(){return this.rect},getAttribute(){return null},contains(other){return other===this||other.parentElement===this},
      closest(s){if(s.startsWith('[inert]'))return null;if(s==='a[href]')return this.href?this:null;return this.interactive?this:null},
      matches(s){return s==='img'&&this.image},querySelector(){return null},querySelectorAll(){return []},getRootNode(){return document},
      focus(){state.focused=this.name;document.activeElement=this;},blur(){if(document.activeElement===this)document.activeElement=null;},
      scrollIntoView(){state.pageScroll++},scrollBy({left=0,top=0}){this.scrollTop=Math.max(0,Math.min(this.scrollHeight-this.clientHeight,this.scrollTop+top));this.scrollLeft=Math.max(0,Math.min(this.scrollWidth-this.clientWidth,this.scrollLeft+left));},...options};
    state.elements.push(e);return e;
  }
  function event(kind,e={}){for(const fn of listeners.get(kind)||[])fn(e);}
  function step(ms){state.now+=ms;const scheduled=[...frames.values()];frames.clear();for(const fn of scheduled)fn(state.now);}
  return {state,document,element,api:window.__pocketSurf,event,step,pending:()=>frames.size};
}

test('spatial right selection ignores DOM ordering',()=>{const f=fixture();f.element('start',10,10);f.element('below',10,120);f.element('right',180,10);f.api.move('down');assert.equal(f.state.focused,'start');f.api.move('right');assert.equal(f.state.focused,'right')});
test('disabled, hidden and occluded controls are skipped',()=>{const f=fixture();f.element('start',10,10);f.element('disabled',130,10,100,40,{disabled:true});f.element('hidden',250,10,100,40,{visible:false});f.element('covered',370,10);f.element('overlay',370,10,100,40,{interactive:false});f.element('target',500,10);f.api.move('right');f.api.move('right');assert.equal(f.state.focused,'target')});
test('no target never turns element navigation into free scrolling',()=>{const f=fixture();f.api.move('down');assert.equal(f.state.pageScroll,0);f.element('only',10,10);f.api.move('down');f.api.move('down');assert.equal(f.state.pageScroll,0)});
test('dynamically added and removed controls are handled',()=>{const f=fixture();const a=f.element('first',10,10);f.api.move('down');f.element('new',150,10);f.api.move('right');assert.equal(f.state.focused,'new');f.state.elements[1].isConnected=false;f.api.move('left');assert.equal(f.state.focused,'first');a.disabled=true;assert.equal(f.api.move('down'),false)});
test('nested scroll area consumes scroll before handing overflow to page',()=>{const f=fixture();const parent=f.element('panel',0,0,300,300,{interactive:false,scrollable:true,scrollHeight:800,clientHeight:300});f.element('link',20,20,100,40,{parentElement:parent});f.api.move('down');f.api.scroll(0,25);assert.equal(parent.scrollTop,25);assert.equal(f.state.pageScroll,0);parent.scrollTop=490;f.api.scroll(0,25);assert.equal(parent.scrollTop,500);assert.equal(f.state.scrollY,15)});
test('pointer target uses native viewport scale and exposes target identity',()=>{const f=fixture();f.element('left',20,20,100,40,{href:'https://left.test'});f.element('right',300,20,100,40,{href:'https://right.test'});f.api.point([650,60,1600]);const t=f.api.target();assert.equal(t.link,'https://right.test');assert.equal(t.title,'right');assert.equal(t.rect.left,300);assert.equal(t.rect.width,100);assert.equal(t.viewportWidth,800);f.api.point(null);f.api.move('left');assert.equal(f.api.target().link,'https://left.test')});
test('activation exposes a real touch coordinate',()=>{const f=fixture();f.element('button',100,80,120,40);const p=f.api.activationPoint();assert.equal(p.x,160);assert.equal(p.y,100);assert.equal(p.width,800);assert.equal(p.editable,false)});
test('selecting text field never focuses it; activation locks navigation until released',()=>{const f=fixture();const input=f.element('input',20,20,200,40,{tagName:'INPUT',type:'text'});f.element('next',20,100);assert.equal(f.api.move('down'),true);assert.equal(f.state.focused,null);assert.equal(f.api.editing(),false);assert.equal(f.api.activationPoint().editable,true);assert.equal(f.api.editing(),true);assert.equal(f.api.move('down'),false);assert.equal(f.api.target().title,'input');input.focus();f.api.releaseEditing();assert.equal(f.api.editing(),false);assert.equal(f.api.move('down'),true);assert.equal(f.state.focused,'next')});
test('actual active editable element locks movement even without activation callback',()=>{const f=fixture();const input=f.element('input',20,20,200,40,{tagName:'TEXTAREA'});input.focus();assert.equal(f.api.editing(),true);assert.equal(f.api.move('down'),false);f.api.releaseEditing();assert.equal(f.api.editing(),false)});
test('touch immediately hides controller ring and does not refocus tapped control',()=>{const f=fixture();const a=f.element('one',20,20),b=f.element('two',200,20);f.api.move('down');assert.equal(f.state.ring.style.display,'block');f.event('pointerdown',{target:b,pointerType:'touch'});assert.equal(f.api.mode(),'touch');assert.equal(f.state.ring.style.display,'none');assert.equal(f.state.focused,'one');assert.equal(f.api.target().title,'two');f.api.mode('controller');assert.equal(f.state.ring.style.display,'block')});
test('prepared native controller clicks do not switch to touch mode',()=>{const f=fixture();const a=f.element('one',20,20);f.api.move('down');f.api.prepareClick();f.event('pointerdown',{target:a,pointerType:'touch'});assert.equal(f.api.mode(),'controller');f.api.mode('touch');assert.equal(f.state.ring.style.display,'none')});
test('scroll loop uses elapsed time and changing direction does not restart its clock',()=>{const f=fixture();f.api.setScroll(0,600);for(let i=0;i<5;i++)f.step(20);assert.equal(f.state.scrollY,60);assert.equal(f.pending(),1);f.api.setScroll(0,-600);f.step(20);assert.equal(f.state.scrollY,48);assert.equal(f.pending(),1)});
test('scroll stop cancels future motion immediately',()=>{const f=fixture();f.api.setScroll(0,600);f.step(20);const y=f.state.scrollY;f.api.setScroll(0,0);assert.equal(f.pending(),0);f.step(200);assert.equal(f.state.scrollY,y)});
test('editing, page blur and document hiding stop scroll',()=>{for(const state of ['editing','blur','hidden']){const f=fixture();f.api.setScroll(0,600);f.step(20);if(state==='editing'){const e=f.element('field',20,20,100,40,{tagName:'INPUT',type:'text'});e.focus();f.step(20);}else if(state==='blur')f.event('blur');else{f.document.hidden=true;f.event('visibilitychange');}const y=f.state.scrollY;f.step(200);assert.equal(f.state.scrollY,y);assert.equal(f.pending(),0)}});
test('dirty tracking records only a boolean for trusted form edits and never reads values',()=>{const f=fixture();const input=f.element('password',20,20,100,40,{tagName:'INPUT',type:'password'});Object.defineProperty(input,'value',{get(){throw Error('private value accessed')}});f.event('input',{target:input,isTrusted:false});assert.equal(f.api.hasUnsavedChanges(),false);f.event('input',{target:input,isTrusted:true});assert.equal(f.api.hasUnsavedChanges(),true)});
test('checkbox change is an unsaved form edit',()=>{const f=fixture();const e=f.element('check',20,20,100,40,{tagName:'INPUT',type:'checkbox'});f.event('change',{target:e,isTrusted:true});assert.equal(f.api.hasUnsavedChanges(),true)});

test('open shadow roots remain selectable without admitting controls covered by the host',()=>{const f=fixture();f.element('covered behind host',20,20);const host=f.element('host',0,0,300,200,{interactive:false});const child=f.element('shadow link',20,20,100,40,{href:'https://shadow.test'});f.state.elements.pop();const root={host,activeElement:null,elementFromPoint:()=>child,querySelectorAll:()=>[child]};host.shadowRoot=root;child.getRootNode=()=>root;f.api.move('down');assert.equal(f.state.focused,'shadow link');f.api.point([60,40,800]);assert.equal(f.api.target().link,'https://shadow.test')});

test('clearing selection removes the controller ring and target without choosing another element',()=>{const f=fixture();f.element('link',20,20,100,40,{href:'https://example.test'});f.api.move('down');f.api.clearSelection();assert.equal(f.state.ring.style.display,'none');assert.equal(f.api.target().link,undefined);});

test('continuous scroll caches ancestor styles and never queries the page DOM',()=>{
 const f=fixture();const parent=f.element('panel',0,0,300,300,{interactive:false,scrollable:true,scrollHeight:800,clientHeight:300});
 f.element('link',20,20,100,40,{parentElement:parent});f.api.move('down');
 const queries=f.state.queries,styles=f.state.styles;f.api.setScroll(0,600);
 for(let i=0;i<10;i++)f.step(20);
 assert.equal(f.state.queries,queries);assert.equal(f.state.styles-styles,2);assert.equal(parent.scrollTop,120);
 parent.scrollHeight=1800;parent.scrollTop=1490;f.step(20);assert.equal(parent.scrollTop,1500);assert.equal(f.state.scrollY,2);
});
test('detached cached scrollers are replaced without waiting for cache expiry',()=>{
 const f=fixture();const parent=f.element('panel',0,0,300,300,{interactive:false,scrollable:true,scrollHeight:800,clientHeight:300});
 const link=f.element('link',20,20,100,40,{parentElement:parent});f.api.move('down');f.api.setScroll(0,600);f.step(20);
 parent.isConnected=false;link.isConnected=false;f.state.elements=[];f.step(20);assert.equal(f.state.scrollY,12);
});

test('large growing feed does not enter global DOM enumeration during continuous scroll',()=>{
 const f=fixture();f.element('feed',0,0,800,500,{interactive:false});
 for(let i=0;i<20000;i++)f.element('post '+i,0,1000+i*100,100,40);
 const queries=f.state.queries;f.api.setScroll(0,600);
 for(let i=0;i<60;i++){if(i===30)for(let j=0;j<1000;j++)f.element('loaded '+j,0,3000000+j*100);f.step(16);}
 assert.equal(f.state.queries,queries);assert.ok(f.state.styles<=4);assert.ok(Math.abs(f.state.scrollY-576)<.0001);
});


test('native root route is available for ordinary page content',()=>{
 const f=fixture();const link=f.element('link',20,20);f.api.move('down');assert.equal(f.api.canNativeScroll(),true);assert.equal(f.api.canNativeScroll(0,1800),true);
});
test('nested scroll route respects requested axis and remains nested at boundaries',()=>{
 const f=fixture();const panel=f.element('panel',0,0,300,300,{interactive:false,scrollable:true,scrollHeight:800,clientHeight:300});
 f.element('link',20,20,100,40,{parentElement:panel});f.api.move('down');
 assert.equal(f.api.canNativeScroll(),false);assert.equal(f.api.canNativeScroll(0,1800),false);assert.equal(f.api.canNativeScroll(1800,0),true);
 panel.scrollTop=500;assert.equal(f.api.canNativeScroll(0,1800),false);assert.equal(f.api.canNativeScroll(0,-1800),false);
 panel.scrollHeight=300;assert.equal(f.api.canNativeScroll(0,1800),true);
});
test('horizontal nested overflow retains JS routing only for horizontal gestures',()=>{
 const f=fixture();const panel=f.element('panel',0,0,300,300,{interactive:false,horizontal:true,scrollWidth:800,clientWidth:300});
 f.element('link',20,20,100,40,{parentElement:panel});f.api.move('down');assert.equal(f.api.canNativeScroll(1800,0),false);assert.equal(f.api.canNativeScroll(0,1800),true);
});
test('root scrolling element does not count as nested overflow',()=>{
 const f=fixture();const body=f.element('body',0,0,800,500,{tagName:'BODY',interactive:false,scrollable:true,scrollHeight:5000,clientHeight:500});f.document.body=body;
 f.element('link',20,20,100,40,{parentElement:body});f.api.move('down');assert.equal(f.api.canNativeScroll(0,1800),true);
});
test('iframe under pointer conservatively retains JS route without accessing its document',()=>{
 const f=fixture();const iframe=f.element('frame',20,20,300,300,{tagName:'IFRAME',interactive:false});Object.defineProperty(iframe,'contentDocument',{get(){throw Error('cross-origin access')}});
 f.api.point([50,50,800]);assert.equal(f.api.canNativeScroll(0,1800),false);
});

test('iframe at viewport center is guarded even before any selection',()=>{
 const f=fixture();f.element('frame',0,0,800,500,{tagName:'IFRAME',interactive:false});assert.equal(f.api.canNativeScroll(0,1800),false);
});

test('clearing controller selection removes its DOM focus and ring',()=>{const f=fixture();f.element('old link',10,10);f.api.move('down');assert.equal(f.state.ring.style.display,'block');f.api.clearSelection();assert.equal(f.document.activeElement,null);assert.equal(f.state.ring.style.display,'none');f.api.mode('controller');assert.equal(f.state.ring.style.display,'none')});
test('hidden pages suspend and restore selection without stealing DOM focus',()=>{const f=fixture();f.element('old link',10,10);f.api.move('down');f.document.hidden=true;f.event('visibilitychange');f.document.hidden=false;f.event('visibilitychange');f.api.mode('controller');assert.equal(f.state.ring.style.display,'block');assert.equal(f.document.activeElement,null)});
test('clearing selection preserves editable field text and focus',()=>{const f=fixture();const input=f.element('editor',10,10,100,40,{tagName:'INPUT',type:'text',value:'draft'});input.focus();f.api.clearSelection();assert.equal(f.document.activeElement,input);assert.equal(input.value,'draft')});

test('mouse mode clears element selection and prevents D-pad selection until explicitly restored',()=>{const f=fixture();f.element('old',20,20);f.element('next',20,100);f.api.move('down');assert.equal(f.state.ring.style.display,'block');f.api.mode('mouse');assert.equal(f.state.ring.style.display,'none');assert.equal(f.document.activeElement,null);assert.equal(f.api.move('down'),false);assert.equal(f.state.ring.style.display,'none');f.api.mode('controller');f.api.move('down');assert.equal(f.state.ring.style.display,'block')});
test('touch-to-mouse transition clears stale selection and keeps mouse highlighting disabled',()=>{const f=fixture();f.element('link',20,20);f.api.move('down');f.api.mode('touch');f.api.mode('mouse');f.api.resumeSelection();assert.equal(f.state.ring.style.display,'none');assert.equal(f.api.move('right'),false)});

test('touch context uses its coordinates and restores the mouse target without activating a link',()=>{const f=fixture();f.element('pointer link',20,20,100,40,{href:'https://pointer.test/'});f.element('touch link',200,100,100,40,{href:'https://touch.test/'});f.api.mode('mouse');f.api.point([40,30,800]);const result=f.api.targetAt([240,120,800]);assert.equal(result.link,'https://touch.test/');assert.equal(f.api.target().link,'https://pointer.test/');assert.equal(f.document.activeElement,null);});

test('controller selection follows the chosen accent and rejects invalid CSS',()=>{const f=fixture();assert.equal(f.api.accent('#ff96a0'),true);assert.equal(f.state.ring.style.borderColor,'#ff96a0');assert.equal(f.api.accent('red;display:none'),false);assert.equal(f.state.ring.style.borderColor,'#ff96a0')});
