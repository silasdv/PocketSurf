(() => {
  if (window.__pocketSurf) return;
  let selectionVisible = true;
  let current = null, pointer = null, inputMode = 'controller', editingTarget = null, dirty = false;
  let velocityX = 0, velocityY = 0, animation = null, previousTime = null, controllerClickUntil = 0;
  let scrollChain = null, scrollChainTime = 0;
  const ring = document.createElement('div');
  ring.setAttribute('aria-hidden', 'true');
  ring.style.cssText = 'position:fixed;pointer-events:none;z-index:2147483647;border:3px solid var(--pocketsurf-accent, #359cff);box-shadow:0 0 0 2px #071322,0 0 0 5px #ffffffb8;border-radius:5px;box-sizing:border-box;display:none;';
  (document.body || document.documentElement).appendChild(ring);
  const selector = 'a[href],button,input:not([type="hidden"]),textarea,select,summary,[tabindex],[role="button"],[role="link"],[role="checkbox"],[contenteditable="true"]';
  function isEditable(e) {
    if (!e || e.disabled || e.readOnly) return false;
    const tag = (e.tagName || '').toLowerCase();
    return tag === 'textarea' || e.isContentEditable ||
      (tag === 'input' && !/^(button|checkbox|radio|range|color|file|hidden|submit|reset|image)$/i.test(e.type || 'text'));
  }
  function deepActive() {
    let e = document.activeElement;
    while (e && e.shadowRoot && e.shadowRoot.activeElement) e = e.shadowRoot.activeElement;
    return e;
  }
  function editing() {
    if (editingTarget && (!editingTarget.isConnected || !isEditable(editingTarget))) editingTarget = null;
    return !!editingTarget || isEditable(deepActive());
  }
  function updateRing() {
    if (!selectionVisible || inputMode !== 'controller' || !current || !current.isConnected) { ring.style.display = 'none'; return; }
    const r = current.getBoundingClientRect();
    Object.assign(ring.style, { display:'block', left:`${r.left-3}px`, top:`${r.top-3}px`, width:`${r.width+6}px`, height:`${r.height+6}px` });
  }
  function collect(root, result = []) {
    root.querySelectorAll(selector).forEach(e => result.push(e));
    root.querySelectorAll('*').forEach(e => { if (e.shadowRoot) collect(e.shadowRoot, result); });
    return result;
  }
  function hitAt(x,y) {
    let hit=document.elementFromPoint(x,y);
    while(hit&&hit.shadowRoot&&typeof hit.shadowRoot.elementFromPoint==='function') {
      const inner=hit.shadowRoot.elementFromPoint(x,y);if(!inner||inner===hit)break;hit=inner;
    }
    return hit;
  }
  function composedContains(parent,node) {
    for(let e=node;e;e=e.parentElement||(e.getRootNode&&e.getRootNode().host))if(e===parent)return true;
    return false;
  }
  function anchorFor(node) {
    for(let e=node;e;e=e.getRootNode&&e.getRootNode().host){const a=e.closest('a[href]');if(a)return a;}
    return null;
  }
  function eligible(e) {
    if (!e || !e.isConnected || e.disabled || e.closest('[inert],[aria-hidden="true"]') || e.getAttribute('aria-disabled') === 'true') return false;
    const r = e.getBoundingClientRect(), s = getComputedStyle(e);
    if (r.width < 2 || r.height < 2 || s.display === 'none' || s.visibility !== 'visible' || Number(s.opacity) === 0) return false;
    if (r.bottom > 0 && r.top < innerHeight && r.right > 0 && r.left < innerWidth) {
      return [[r.x+r.width/2,r.y+r.height/2],[r.x+4,r.y+4],[r.right-4,r.bottom-4]].some(([x,y]) => {
        const hit = hitAt(Math.max(1,Math.min(innerWidth-1,x)),Math.max(1,Math.min(innerHeight-1,y)));
        return hit && (hit === e || e.contains(hit) || composedContains(e,hit));
      });
    }
    return true;
  }
  function select(e) {
    current = e; scrollChain = null;
    // Selecting a text field must not focus it and summon the IME. A/native touch activates it.
    if (!isEditable(e)) try { e.focus({preventScroll:true}); } catch (_) {}
    const r = e.getBoundingClientRect();
    if (r.top < 3 || r.bottom > innerHeight-3 || r.left < 3 || r.right > innerWidth-3) e.scrollIntoView({block:'nearest',inline:'nearest',behavior:'instant'});
    updateRing();
  }
  function move(direction) {
    if(inputMode==='mouse')return false;
    if (editing()) return false;
    const all = collect(document).filter(eligible);
    if (!all.length) return false; // No implicit free-scrolling from left-stick/D-pad movement.
    if (!current || !eligible(current)) {
      const visible = all.filter(e => {const r=e.getBoundingClientRect();return r.top>=0&&r.top<innerHeight&&r.left>=0&&r.left<innerWidth;});
      select((visible.length?visible:all).sort((a,b)=>{const x=a.getBoundingClientRect(),y=b.getBoundingClientRect();return x.top-y.top||x.left-y.left;})[0]);return true;
    }
    const a=current.getBoundingClientRect(),ax=a.x+a.width/2,ay=a.y+a.height/2;
    let best=null,score=Infinity;
    all.forEach(e => {
      if (e===current) return;
      const b=e.getBoundingClientRect(),dx=b.x+b.width/2-ax,dy=b.y+b.height/2-ay;
      const primary=direction==='right'?dx:direction==='left'?-dx:direction==='down'?dy:-dy;
      const cross=direction==='right'||direction==='left'?Math.abs(dy):Math.abs(dx);
      if(primary<2)return;
      const value=primary+cross*2.5+cross*cross/Math.max(primary,8);
      if(value<score){score=value;best=e;}
    });
    if(best)select(best);return !!best;
  }
  function activationPoint() {
    if (!current || !eligible(current)) move('down');
    if (!current) return null;
    controllerClickUntil=performance.now()+1000;
    const editable=isEditable(current);
    if (editable) { editingTarget=current; setScroll(0,0); }
    const r=current.getBoundingClientRect();
    return {x:Math.max(1,Math.min(innerWidth-1,r.x+r.width/2)),y:Math.max(1,Math.min(innerHeight-1,r.y+r.height/2)),width:innerWidth,editable};
  }
  function targetElement() { return pointer ? hitAt(pointer[0],pointer[1]) : current; }
  function target() {
    const e=targetElement();if(!e)return {title:'Page',viewportWidth:innerWidth};
    const a=anchorFor(e),image=e.matches('img')?e:e.querySelector('img');
    const outlined=image||a||e;current=outlined;updateRing();
    const r=outlined.getBoundingClientRect();
    const raw=outlined.getAttribute('aria-label')||outlined.getAttribute('title')||outlined.textContent||'';
    const title=raw.replace(/\s+/g,' ').trim().slice(0,160)||(isEditable(outlined)?'Text field':image?'Image':a?'Link':'Page element');
    return {link:a?a.href:'',image:image?(image.currentSrc||image.src):'',title,editable:isEditable(outlined),rect:{left:r.left,top:r.top,width:r.width,height:r.height},viewportWidth:innerWidth};
  }
  function scrollTarget() {
    const e=targetElement();
    if(e&&e.isConnected){const r=e.getBoundingClientRect();if(r.bottom>0&&r.top<innerHeight&&r.right>0&&r.left<innerWidth)return e;}
    return hitAt(innerWidth/2,innerHeight/2);
  }
  // Cache only the ancestor path, never page height: infinite feeds grow while scrolling.
  // A bounded refresh also catches CSS overflow changes without observing every feed mutation.
  function getScrollChain() {
    const now=performance.now();
    if(scrollChain && now-scrollChainTime<250 && scrollChain.every(item=>item.e.isConnected))return scrollChain;
    const chain=[],visited=new Set();
    for(let e=scrollTarget();e&&e!==document.documentElement&&!visited.has(e);e=e.parentElement||(e.getRootNode&&e.getRootNode().host)) {
      visited.add(e);const s=getComputedStyle(e);
      if(/(auto|scroll)/.test(s.overflowX)||/(auto|scroll)/.test(s.overflowY))chain.push({e,x:/(auto|scroll)/.test(s.overflowX),y:/(auto|scroll)/.test(s.overflowY)});
    }
    scrollChain=chain;scrollChainTime=now;return chain;
  }
  function canNativeScroll(x,y) {
    // Root WebView scrolling can proceed without scheduling work on the page's JS thread.
    // Keep nested containers on the JS path for the entire gesture, including at boundaries.
    const both=arguments.length===0, useX=both||Number.isFinite(x)&&x!==0, useY=both||Number.isFinite(y)&&y!==0;
    const target=scrollTarget();
    for(let e=target;e;e=e.parentElement||(e.getRootNode&&e.getRootNode().host)) {
      if(/^(iframe|object|embed)$/i.test(e.tagName||''))return false;
    }
    return !getScrollChain().some(({e,x,y})=>e!==document.body&&e!==document.documentElement&&e!==document.scrollingElement&&
      ((useX&&x&&e.scrollWidth>e.clientWidth+1)||(useY&&y&&e.scrollHeight>e.clientHeight+1)));
  }
  function scroll(dx,dy) {
    let remainingX=dx,remainingY=dy;
    // Read every ancestor's geometry before any scroll write. No whole-DOM traversal per frame.
    const metrics=getScrollChain().map(({e,x,y})=>({e,x,y,left:e.scrollLeft||0,top:e.scrollTop||0,maxX:Math.max(0,e.scrollWidth-e.clientWidth),maxY:Math.max(0,e.scrollHeight-e.clientHeight)}));
    for(const m of metrics) {
      const x=m.x?Math.max(-m.left,Math.min(m.maxX-m.left,remainingX)):0;
      const y=m.y?Math.max(-m.top,Math.min(m.maxY-m.top,remainingY)):0;
      if(x||y){m.e.scrollBy({left:x,top:y,behavior:'instant'});remainingX-=x;remainingY-=y;}
      if(Math.abs(remainingX)<.001&&Math.abs(remainingY)<.001)break;
    }
    if(Math.abs(remainingX)>=.001||Math.abs(remainingY)>=.001)window.scrollBy({left:remainingX,top:remainingY,behavior:'instant'});
    return true;
  }
  function animationFrame(now) {
    animation=null;
    if((velocityX===0&&velocityY===0)||editing()||document.hidden){setScroll(0,0);return;}
    const dt=previousTime===null?0:Math.max(0,Math.min((now-previousTime)/1000,.05));previousTime=now;
    if(dt>0)scroll(velocityX*dt,velocityY*dt);
    if(velocityX!==0||velocityY!==0)animation=requestAnimationFrame(animationFrame);
  }
  function setScroll(x,y) {
    velocityX=Number.isFinite(x)?Math.max(-5000,Math.min(5000,x)):0;
    velocityY=Number.isFinite(y)?Math.max(-5000,Math.min(5000,y)):0;
    if(velocityX===0&&velocityY===0){if(animation!==null)cancelAnimationFrame(animation);animation=null;previousTime=null;scrollChain=null;return;}
    if(animation===null){previousTime=typeof performance!=='undefined'?performance.now():null;animation=requestAnimationFrame(animationFrame);}
  }
  window.__pocketSurf={move,
    targetAt(p){const saved=pointer;pointer=p&&p[2]>0?[p[0]*innerWidth/p[2],p[1]*innerWidth/p[2]]:null;try{return target();}finally{pointer=saved;}},
    accent(value){if(!/^#[0-9a-f]{6}$/i.test(value))return false;ring.style.borderColor=value;return true;},
    suspendSelection(){selectionVisible=false;editingTarget=null;pointer=null;setScroll(0,0);const e=deepActive();if(e&&typeof e.blur==='function')e.blur();updateRing();},
    resumeSelection(){selectionVisible=true;updateRing();},activationPoint,target,scroll,setScroll,canNativeScroll,editing,
    clearSelection(){const previous=current;current=null;pointer=null;editingTarget=null;setScroll(0,0);if(previous&&deepActive()===previous&&!isEditable(previous))try{previous.blur();}catch(_){};updateRing();},
    releaseEditing(){editingTarget=null;const e=deepActive();if(isEditable(e))try{e.blur();}catch(_){};updateRing();},
    mode(value){if(value==='mouse'&&inputMode!=='mouse'){if(window.getSelection)try{window.getSelection().removeAllRanges();}catch(_){};current=null;editingTarget=null;pointer=null;setScroll(0,0);const active=deepActive();if(active&&typeof active.blur==='function')active.blur();}if(value==='touch'||value==='controller'||value==='mouse')inputMode=value;if(value==='touch')controllerClickUntil=0;updateRing();return inputMode;},
    prepareClick(){controllerClickUntil=performance.now()+1000;},
    point(p){scrollChain=null;pointer=p&&p[2]>0?[p[0]*innerWidth/p[2],p[1]*innerWidth/p[2]]:null;},
    hasUnsavedChanges(){return dirty||typeof window.onbeforeunload==='function';}
  };
  addEventListener('scroll',updateRing,true);addEventListener('resize',updateRing);
  addEventListener('blur',()=>setScroll(0,0));addEventListener('pagehide',()=>setScroll(0,0));
  document.addEventListener('visibilitychange',()=>{if(document.hidden)window.__pocketSurf.suspendSelection();else window.__pocketSurf.resumeSelection();});
  new MutationObserver(()=>{if(current&&!current.isConnected){current=null;updateRing();}if(editingTarget&&!editingTarget.isConnected)editingTarget=null;}).observe(document.documentElement,{childList:true,subtree:true});
  document.addEventListener('pointerdown',e=>{
    if(e.target===ring)return;
    const controllerClick=performance.now()<=controllerClickUntil&&controllerClickUntil>0;controllerClickUntil=0;
    if(!controllerClick&&(e.pointerType==='touch'||e.pointerType==='pen'))inputMode='touch';
    const path=typeof e.composedPath==='function'?e.composedPath():[e.target];
    const candidate=path.find(n=>n&&typeof n.closest==='function'&&n.closest(selector));
    const selected=candidate&&candidate.closest(selector);
    if(inputMode==='mouse'){current=null;editingTarget=isEditable(selected)?selected:null;updateRing();return;}
    if(selected&&eligible(selected)){scrollChain=null;current=selected;editingTarget=isEditable(selected)?selected:null;updateRing();}
    else editingTarget=null;
  },true);
  document.addEventListener('focusout',e=>{if(e.target===editingTarget)editingTarget=null;},true);
  for(const kind of ['input','change'])document.addEventListener(kind,e=>{const tag=(e.target.tagName||'').toLowerCase();if(e.isTrusted&&(isEditable(e.target)||tag==='select'||(tag==='input'&&!/^(button|hidden|submit|reset|image)$/i.test(e.target.type||'text'))))dirty=true;},true);
})();
