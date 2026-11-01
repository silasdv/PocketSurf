const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.resolve(__dirname, '../../main/assets/cookie-consent.js'), 'utf8');

// Controlled CMP markup and time; no network, real CMP execution or browser engine is claimed.
function fixture() {
  let now=0, id=0, observer, disconnected=0;
  const timers=new Map(), roots=new Map(), listeners=new Map();
  const document={hidden:false,documentElement:{},
    querySelectorAll(selector){return roots.get(selector)||[];},
    addEventListener(kind,fn){listeners.set(kind,fn);}};
  const window={addEventListener(kind,fn){listeners.set(kind,fn);}};
  const context={window,document,WeakSet,Date:{now:()=>now},
    getComputedStyle:e=>({display:e.hidden?'none':'block',visibility:'visible',opacity:'1',...e.style}),
    setTimeout(fn,ms){const token=++id;timers.set(token,{fn,at:now+ms});return token;},
    clearTimeout(token){timers.delete(token);},
    MutationObserver:class{constructor(fn){observer=fn;}observe(){}disconnect(){disconnected++;}}};
  function element(options={}){return {nodeType:1,isConnected:true,hidden:false,disabled:false,parentElement:null,style:{},clicks:0,
    getAttribute(){return null;},getBoundingClientRect(){return {width:200,height:40};},
    click(){this.clicks++;},querySelectorAll(){return [];},...options};}
  function banner(selector,buttonSelector,options={}){const button=element(options);const root=element({querySelectorAll:s=>s===buttonSelector?[button]:[]});button.parentElement=root;roots.set(selector,[root]);return {button,root};}
  function tick(ms){now+=ms;for(const [token,timer]of [...timers])if(timer.at<=now){timers.delete(token);timer.fn();}}
  vm.runInNewContext(source,context);
  return {document,window,api:window.__pocketSurfCookies,banner,element,tick,
    mutate:()=>observer?.(),event:kind=>listeners.get(kind)?.(),disconnected:()=>disconnected,
    pending:()=>timers.size,reinject:()=>vm.runInNewContext(source,context)};
}

test('known first-layer reject controls are clicked once, never accept or save',()=>{
  const selectors=[['#onetrust-banner-sdk','#onetrust-reject-all-handler'],['#CybotCookiebotDialog','#CybotCookiebotDialogBodyButtonDecline'],['.cky-consent-container','.cky-btn-reject'],['#iubenda-cs-banner','.iubenda-cs-reject-btn']];
  for(const [root,selector]of selectors){const f=fixture();const {button}=f.banner(root,selector);f.api.configure(true);f.tick(150);assert.equal(button.clicks,1);f.api.scan();f.mutate();f.tick(150);f.reinject();f.api.configure(true);f.tick(150);assert.equal(button.clicks,1);assert.equal(f.api.status().actions,1);}
});
test('unknown, accept-only and generic save dialogs are untouched',()=>{
  for(const [root,selector]of [['#unknown-cookie-dialog','button'],['#onetrust-banner-sdk','#onetrust-accept-btn-handler'],['.cky-consent-container','.cky-btn-preferences'],['#iubenda-cs-banner','.iubenda-cs-accept-btn']]){const f=fixture();const {button}=f.banner(root,selector);f.api.configure(true);f.tick(150);assert.equal(button.clicks,0);assert.equal(f.api.status().actions,0);}
});
test('disabled helper takes no action and cancels queued dynamic work',()=>{
  const f=fixture();const {button}=f.banner('#onetrust-banner-sdk','#onetrust-reject-all-handler');f.tick(1000);assert.equal(button.clicks,0);f.api.configure(true);f.api.configure(false);f.tick(1000);f.api.scan();assert.equal(button.clicks,0);assert.equal(f.pending(),0);assert.equal(f.api.status().observing,false);
});
test('late-loaded banners are handled through a throttled observer',()=>{
  const f=fixture();f.api.configure(true);f.tick(150);const {button}=f.banner('.cky-consent-container','.cky-btn-reject');for(let n=0;n<100;n++)f.mutate();assert.equal(f.pending(),2);f.tick(149);assert.equal(button.clicks,0);f.tick(1);assert.equal(button.clicks,1);
});
test('hidden, disabled, detached and zero-size controls cannot be clicked',()=>{
  for(const options of [{hidden:true},{disabled:true},{isConnected:false},{style:{opacity:'0'}},{getBoundingClientRect:()=>({width:0,height:0})},{getAttribute:name=>name==='aria-disabled'?'true':null}]){const f=fixture();const {button}=f.banner('#onetrust-banner-sdk','#onetrust-reject-all-handler',options);f.api.configure(true);f.tick(150);assert.equal(button.clicks,0);}
  const f=fixture();const {button,root}=f.banner('#onetrust-banner-sdk','#onetrust-reject-all-handler');root.hidden=true;f.api.configure(true);f.tick(150);assert.equal(button.clicks,0);
});
test('Cookiebot explicit fallback denies all three optional purposes only for unanswered visible banner',()=>{
  const f=fixture();f.banner('#CybotCookiebotDialog','unused');const calls=[];f.window.Cookiebot={hasResponse:false,submitCustomConsent(...args){calls.push(args);}};f.api.configure(true);f.tick(150);assert.deepEqual(calls,[[false,false,false]]);f.api.scan();assert.equal(calls.length,1);
  const answered=fixture();answered.banner('#CybotCookiebotDialog','unused');answered.window.Cookiebot={hasResponse:true,submitCustomConsent(){throw Error('Must not override previous choice');}};answered.api.configure(true);answered.tick(150);assert.equal(answered.api.status().actions,0);
});
test('Cookiebot fallback never bypasses a disabled reject control',()=>{
  const f=fixture();const {button}=f.banner('#CybotCookiebotDialog','#CybotCookiebotDialogBodyButtonDecline',{disabled:true});let calls=0;f.window.Cookiebot={hasResponse:false,submitCustomConsent(){calls++;}};f.api.configure(true);f.tick(150);assert.equal(button.clicks,0);assert.equal(calls,0);
});
test('video buttons and unsupported Google consent controls remain untouched',()=>{
  const f=fixture();const {button:video}=f.banner('video','button');const {button:google}=f.banner('form[action="https://consent.google.com/save"]','button');f.api.configure(true);f.tick(150);f.mutate();f.tick(150);assert.equal(video.clicks,0);assert.equal(google.clicks,0);assert.equal(f.api.status().actions,0);
});
test('observer lifetime and maximum actions are bounded',()=>{
  const f=fixture();f.api.configure(true);f.tick(120000);assert.equal(f.api.status().observing,false);assert.equal(f.pending(),0);const {button}=f.banner('#onetrust-banner-sdk','#onetrust-reject-all-handler');f.api.scan();assert.equal(button.clicks,0);
  const g=fixture();g.api.configure(true);for(let n=0;n<12;n++){g.banner('#onetrust-banner-sdk','#onetrust-reject-all-handler');g.api.scan();}assert.equal(g.api.status().actions,8);assert.equal(g.api.status().observing,false);
});
test('page hide stops the helper and document hiding prevents actions',()=>{
  const f=fixture();const {button}=f.banner('#onetrust-banner-sdk','#onetrust-reject-all-handler');f.document.hidden=true;f.api.configure(true);f.tick(150);assert.equal(button.clicks,0);f.document.hidden=false;f.event('visibilitychange');f.tick(150);assert.equal(button.clicks,1);f.event('pagehide');assert.equal(f.api.status().enabled,false);assert.equal(f.pending(),0);
});

// Twitch variants exercise domain-scoped rejection, not broad accept/save heuristics.
test('Twitch legacy reject works while accept and unrelated hosts stay untouched',()=>{
 for(const [host,label,expected]of [['www.twitch.tv','Reject All',1],['m.twitch.tv','Alle ablehnen',1],['evil-twitch.tv','Reject All',0],['www.twitch.tv','Accept All',0]]){const f=fixture();f.window.location={hostname:host};const {button}=f.banner('[data-a-target="consent-banner"]','button',{textContent:label});f.api.configure(true);f.tick(150);assert.equal(button.clicks,expected);}
});
function twitchPreferences(labels){const f=fixture();f.window.location={hostname:'www.twitch.tv'};const modal=f.element();const boxes=labels.map(([text,checked])=>f.element({checked,labels:[{textContent:text}],click(){this.clicks++;this.checked=!this.checked}}));modal.querySelectorAll=()=>boxes;const save=f.element({closest:()=>modal,click(){this.clicks++;modal.hidden=true}});f.document.querySelectorAll=s=>s==='[data-a-target="consent-modal-save"]'?[save]:[];f.api.configure(true);return {...f,boxes,save,modal};}
test('Twitch preferences distinguish opt-out checks from opt-in checks before saving',()=>{const f=twitchPreferences([['Opt out of targeted advertising',false],['Personalized advertising',true]]);f.tick(150);assert.equal(f.boxes[0].checked,true);assert.equal(f.boxes[1].checked,false);assert.equal(f.save.clicks,1);f.api.scan();assert.equal(f.api.status().outcome,'banner-dismissed');});
test('unknown Twitch preference semantics never cause an automatic save',()=>{const f=twitchPreferences([['A new ambiguous preference',false]]);f.tick(150);assert.equal(f.boxes[0].clicks,0);assert.equal(f.save.clicks,0);assert.equal(f.api.status().outcome,'manual');});
test('Twitch rejects an excessive preference form without unbounded clicks',()=>{const f=twitchPreferences(Array.from({length:40},()=>['Opt out of advertising',false]));f.tick(150);assert.equal(f.save.clicks,0);assert.equal(f.boxes.reduce((n,b)=>n+b.clicks,0),0);});
test('additional known CMP reject controls work without accepting consent',()=>{for(const [root,button]of [['#didomi-host','#didomi-notice-disagree-button'],['.cmplz-cookiebanner','.cmplz-deny']]){const f=fixture();const b=f.banner(root,button);f.api.configure(true);f.tick(150);assert.equal(b.button.clicks,1);}});

test('current Twitch affirmative toggles are disabled and necessary controls remain untouched',()=>{const f=twitchPreferences([['',true],['',false],['',true]]);f.boxes.forEach(b=>b.getAttribute=k=>k==='data-a-target'?'tw-toggle':null);f.boxes[2].disabled=true;f.tick(150);assert.equal(f.boxes[0].checked,false);assert.equal(f.boxes[1].checked,false);assert.equal(f.boxes[2].checked,true);assert.equal(f.boxes[2].clicks,0);assert.equal(f.save.clicks,1)});
test('Usercentrics open shadow-root rejection works with a zero-sized host',()=>{const f=fixture();const {root,button}=f.banner('#usercentrics-root','unused');root.getBoundingClientRect=()=>({width:0,height:0});root.shadowRoot={querySelectorAll:s=>s==='[data-testid="uc-deny-all-button"]'?[button]:[]};f.api.configure(true);f.tick(150);assert.equal(button.clicks,1)});
