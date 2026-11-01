/* PocketSurf necessary-only consent helper. Original code, MIT.
 * Only known CMP rejection controls are used. Unknown dialogs remain untouched.
 * This selects a site's minimum-consent action; it is not a network cookie firewall.
 */
(() => {
  'use strict';
  if (window.__pocketSurfCookies) return;
  const providers = [
    ['OneTrust', '#onetrust-banner-sdk', '#onetrust-reject-all-handler'],
    ['Cookiebot', '#CybotCookiebotDialog', '#CybotCookiebotDialogBodyButtonDecline'],
    ['CookieYes', '.cky-consent-container', '.cky-btn-reject'],
    ['iubenda', '#iubenda-cs-banner', '.iubenda-cs-reject-btn'],
    ['Didomi', '#didomi-host', '#didomi-notice-disagree-button'],
    ['Complianz', '.cmplz-cookiebanner', '.cmplz-deny'],
    ['Usercentrics', '#usercentrics-root', '[data-testid="uc-deny-all-button"]']
  ];
  const clicked = new WeakSet();
  let enabled = false, observer = null, scheduled = 0, deadline = 0;
  let scans = 0, actions = 0, lastProvider = null, expires = 0, lastBanner = null, outcome = 'idle', twitchPreferencesOpened=false, lastChoice=null;
  const MAX_SCANS = 200, MAX_ACTIONS = 8, LIFETIME = 120000;
  function visible(element) {
    if (!element || !element.isConnected || element.hidden || element.disabled ||
        element.getAttribute('aria-disabled') === 'true') return false;
    for (let current = element; current && current.nodeType === 1; current = current.parentElement) {
      const style = getComputedStyle(current);
      if (current.hidden || style.display === 'none' || style.visibility === 'hidden' ||
          style.visibility === 'collapse' || Number(style.opacity) === 0) return false;
    }
    const bounds = element.getBoundingClientRect();
    return bounds.width > 0 && bounds.height > 0;
  }
  function detach() {
    if (observer) observer.disconnect();
    observer = null;
    if (scheduled) clearTimeout(scheduled);
    if (expires) clearTimeout(expires);
    scheduled = expires = 0;
  }
  function exhausted() {
    return scans >= MAX_SCANS || actions >= MAX_ACTIONS || Date.now() >= deadline;
  }
  function act(button,banner,name,step='choice-sent') {
    if(actions>=MAX_ACTIONS||!button||clicked.has(button)||!visible(button)||typeof button.click!=='function')return false;
    clicked.add(button);try{button.click();actions++;lastProvider=name;lastBanner=banner;outcome=step;schedule();return true;}catch(_){outcome='manual';return false;}
  }
  // Twitch's own consent UI has both direct rejection and a preferences/opt-out flow.
  // Domain + known data-a-target markers keep these actions away from ordinary page forms.
  function twitch() {
    const host=window.location&&window.location.hostname||'';
    if(!/(^|\.)twitch\.tv$/i.test(host))return false;
    for(const banner of document.querySelectorAll('[data-a-target="consent-banner"]')){
      if(!visible(banner))continue;
      for(const button of banner.querySelectorAll('button')){
        const text=(button.textContent||'').trim();const marker=button.getAttribute('data-a-target');
        if(marker==='consent-banner-reject'||/^(reject(?: all)?|decline(?: all)?|alle ablehnen|ablehnen|refuser(?: tout)?|tout refuser|rechazar(?: todo)?|rifiuta(?: tutti)?)$/i.test(text))if(act(button,banner,'Twitch'))return true;
      }
      if(!twitchPreferencesOpened)for(const button of banner.querySelectorAll('[data-a-target="consent-banner-manage-preferences"]'))if(act(button,banner,'Twitch','preferences-opened')){twitchPreferencesOpened=true;return true;}
    }
    for(const save of document.querySelectorAll('[data-a-target="consent-modal-save"]')){
      const modal=save.closest('[role="dialog"]')||save.closest('.ReactModalPortal');if(!modal||!visible(modal)||!visible(save))continue;
      const boxes=Array.from(modal.querySelectorAll('input[type="checkbox"][data-a-target="tw-checkbox"],input[type="checkbox"][data-a-target="tw-toggle"]')).filter(b=>!b.disabled);
      if(!boxes.length||boxes.length>32){outcome='manual';continue;}
      let understood=true;
      for(const box of boxes){
        if(actions>=MAX_ACTIONS){understood=false;break;}
        const label=((box.getAttribute('aria-label')||'')+' '+Array.from(box.labels||[]).map(l=>l.textContent||'').join(' ')+' '+(box.parentElement&&box.parentElement.textContent||'')).toLowerCase();
        const optOut=/opt[ -]?out|widersprech|ablehn|verzicht|opposition|rechazar|rifiut/.test(label);
        const optIn=!optOut&&(box.getAttribute('data-a-target')==='tw-toggle'||/personaliz|personalis|advertis|werb|marketing|analytics|analyse|measurement|messung|amazon ads/.test(label));
        if(!optOut&&!optIn){understood=false;continue;}
        const desired=optOut;if(box.checked!==desired){try{box.click();actions++;lastProvider='Twitch';outcome='preferences-updated';}catch(_){understood=false;}if(box.checked!==desired)understood=false;}
      }
      if(understood){lastChoice={provider:'Twitch',optionalControls:boxes.length,necessaryControlsUntouched:true};if(act(save,modal,'Twitch'))return true;}
      if(!understood)outcome='manual';
    }
    return false;
  }
  function scan() {
    scheduled = 0;
    if (!enabled) return false;
    if (exhausted()) { detach(); return false; }
    scans++;
    if(lastBanner&&!visible(lastBanner)&&outcome==='choice-sent')outcome='banner-dismissed';
    if (document.hidden) return false;
    if(twitch())return true;
    for (const [name, rootSelector, rejectSelector] of providers) {
      // Query only this provider's first-layer banner, never arbitrary page buttons.
      for (const banner of document.querySelectorAll(rootSelector)) {
        if (!banner.shadowRoot&&!visible(banner)) continue;
        const rejectButtons = (banner.shadowRoot||banner).querySelectorAll(rejectSelector);
        for (const button of rejectButtons) {
          if (clicked.has(button) || !visible(button) || typeof button.click !== 'function') continue;
          clicked.add(button);
          try { button.click(); actions++; lastProvider = name; lastBanner=banner;outcome='choice-sent';schedule();return true; }
          catch (_) { /* Do not retry a control with unknown side effects. */ }
        }
        // Cookiebot documents three explicit opt-in booleans. All remain false.
        const bot = window.Cookiebot;
        if (name === 'Cookiebot' && rejectButtons.length === 0 && !clicked.has(banner) && bot &&
            bot.hasResponse === false && typeof bot.submitCustomConsent === 'function') {
          clicked.add(banner);
          try { bot.submitCustomConsent(false, false, false); actions++; lastProvider = name; lastBanner=banner;outcome='choice-sent';schedule();return true; }
          catch (_) { /* Keep the banner available for manual choice. */ }
        }
      }
    }
    return false;
  }
  function schedule() {
    if (!enabled || scheduled) return;
    if (exhausted()) { detach(); return; }
    scheduled = setTimeout(scan, 150);
  }
  function configure(value) {
    const next = value === true;
    if (next === enabled) return;
    enabled = next;
    detach();
    if (!enabled) return;
    scans = 0; deadline = Date.now() + LIFETIME;
    if (typeof MutationObserver === 'function' && document.documentElement) {
      observer = new MutationObserver(schedule);
      observer.observe(document.documentElement, {childList:true, subtree:true, attributes:true,
        attributeFilter:['class', 'style', 'hidden', 'disabled', 'aria-disabled']});
    }
    expires = setTimeout(detach, LIFETIME);
    schedule();
  }
  document.addEventListener('visibilitychange', schedule);
  window.addEventListener('pagehide', () => { enabled = false; detach(); });
  window.__pocketSurfCookies = {configure, scan,
    status: () => ({enabled, actions, lastProvider, observing:observer !== null,outcome,lastChoice})};
})();
