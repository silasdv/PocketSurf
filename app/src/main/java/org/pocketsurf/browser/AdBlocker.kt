package org.pocketsurf.browser

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection

/** Working limited filter layer for WebView; not the Brave/uBlock/EasyList reference engine. */
class AdBlocker(context:Context) {
 private val app=context.applicationContext
 private val prefs=app.getSharedPreferences("adblock",Context.MODE_PRIVATE)
 private val filterFile=AtomicFile(java.io.File(app.filesDir,"adblock-easylist.txt"))
 private val executor=Executors.newSingleThreadExecutor{r->Thread(r,"PocketSurf-filter-update").apply{isDaemon=true}}
 private val main=Handler(Looper.getMainLooper())
 private val busy=AtomicBoolean(false)
 private val blocked=AtomicInteger(0)
 private val siteCounts=java.util.concurrent.ConcurrentHashMap<String,AtomicInteger>()
 fun blockedFor(url:String):Int {val host=AdBlockRules.host(url)?:return 0;return siteCounts[host]?.get()?:0}
 @Volatile private var rules=AdBlockRules.parse(BUILTIN_RULES)
 @Volatile private var usingDownloaded=false
 @Volatile private var updateError:String?=null
 init {
  executor.execute {try{if(filterFile.baseFile.exists()&&filterFile.baseFile.length()<=MAX_BYTES){val saved=filterFile.readFully();val text=saved.toString(Charsets.UTF_8);if(text.take(512).contains("[Adblock",true)){val parsed=AdBlockRules.parse(BUILTIN_RULES+"\n"+text);if(parsed.supportedRules>100){rules=parsed;usingDownloaded=true}}}}catch(_:Exception){updateError="Saved filters could not be read. Built-in protection remains active."}}
 }
 var enabled:Boolean
  get()=prefs.getBoolean("enabled",true)
  set(value){prefs.edit().putBoolean("enabled",value).apply()}
 fun isSiteAllowed(pageUrl:String):Boolean {
  val host=AdBlockRules.host(pageUrl)?:return false
  return host in prefs.getStringSet("allowed",emptySet()).orEmpty()
 }
 fun setSiteAllowed(pageUrl:String,allowed:Boolean){val host=AdBlockRules.host(pageUrl)?:return;val sites=prefs.getStringSet("allowed",emptySet()).orEmpty().toMutableSet();if(allowed)sites.add(host)else sites.remove(host);prefs.edit().putStringSet("allowed",sites).apply()}
 fun toggleSiteException(pageUrl:String):Boolean {val next=!isSiteAllowed(pageUrl);setSiteAllowed(pageUrl,next);return next}
 fun toggleSite(pageUrl:String)=toggleSiteException(pageUrl)
 fun shouldBlock(resourceUrl:String,topPageUrl:String,isMainFrame:Boolean=false):Boolean {
  if(isMainFrame||!enabled||isSiteAllowed(topPageUrl))return false
  val result=rules.blocks(resourceUrl,topPageUrl);if(result){blocked.incrementAndGet();AdBlockRules.host(topPageUrl)?.let{h->if(siteCounts.size<500||siteCounts.containsKey(h))siteCounts.getOrPut(h){AtomicInteger(0)}.incrementAndGet()}};return result
 }
 fun cosmeticScript(pageUrl:String):String {
  if(!enabled||isSiteAllowed(pageUrl))return "document.getElementById('pocketsurf-adblock-style')?.remove();"
  val selectors=JSONArray(rules.selectors(pageUrl)).toString()
  return """(()=>{const old=document.getElementById('pocketsurf-adblock-style');if(old)old.remove();const style=document.createElement('style');style.id='pocketsurf-adblock-style';(document.head||document.documentElement).appendChild(style);const rules=$selectors;let index=0;function batch(){const end=Math.min(index+80,rules.length);for(;index<end;index++){try{style.sheet.insertRule(rules[index]+'{display:none!important}',style.sheet.cssRules.length)}catch(e){}}if(index<rules.length)setTimeout(batch,0)}batch()})();"""
 }
 data class Status(val enabled:Boolean,val updating:Boolean,val source:String,val supportedRules:Int,val skippedRules:Int,val updatedAt:Long,val error:String?,val blockedRequests:Int)
 fun status()=Status(enabled,busy.get(),if(usingDownloaded)"EasyList + PocketSurf starter rules"else"PocketSurf starter rules",rules.supportedRules,rules.skippedRules,prefs.getLong("updated_at",0),updateError,blocked.get())
 /** Parser/commit boundary, shared by the background downloader and deterministic regression tests. */
 internal fun installDownloadedFilters(bytes:ByteArray){
  if(bytes.size>MAX_BYTES)throw java.io.IOException("Filter response too large")
  val text=bytes.toString(Charsets.UTF_8)
  if(!text.take(512).contains("[Adblock",true)||text.contains("<html",true))throw java.io.IOException("Invalid filter response")
  val parsed=AdBlockRules.parse(BUILTIN_RULES+"\n"+text)
  if(parsed.supportedRules<100)throw java.io.IOException("Not enough usable filter rules")
  var stream:java.io.FileOutputStream?=null
  try{stream=filterFile.startWrite();stream.write(bytes);filterFile.finishWrite(stream);stream=null}catch(e:Exception){filterFile.failWrite(stream);throw e}
  rules=parsed;usingDownloaded=true;updateError=null;prefs.edit().putLong("updated_at",System.currentTimeMillis()).apply()
 }
 fun close(){executor.shutdown()}
 /** User-initiated only. Old filters remain live until a valid replacement has been parsed and committed. */
 fun updateFilters(callback:(Boolean,String)->Unit) {
  if(!busy.compareAndSet(false,true)){main.post{callback(false,"A filter update is already running.")};return}
  updateError=null
  executor.execute {
   var connection:HttpsURLConnection?=null
   try {
    connection=URL(FILTER_URL).openConnection() as HttpsURLConnection
    connection.instanceFollowRedirects=false;connection.connectTimeout=15_000;connection.readTimeout=20_000
    connection.setRequestProperty("User-Agent","PocketSurf filter updater")
    if(connection.responseCode!=200)throw java.io.IOException("Filter server unavailable")
    if(connection.contentLengthLong>MAX_BYTES)throw java.io.IOException("Filter response too large")
    val bytes=connection.inputStream.use{input->val output=ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val count=input.read(buffer);if(count<0)break;if(output.size()+count>MAX_BYTES)throw java.io.IOException("Filter response too large");output.write(buffer,0,count)};output.toByteArray()}
    installDownloadedFilters(bytes);busy.set(false)
    main.post{callback(true,"Filters updated. Reload the page to apply protection to already loaded content.")}
   }catch(_:Exception){updateError="Filters could not be updated. Check your connection and try again; existing protection is unchanged.";busy.set(false);main.post{callback(false,updateError!!)}}
   finally{connection?.disconnect()}
  }
 }
 companion object {
  const val FILTER_URL="https://easylist.to/easylist/easylist.txt"
  private const val MAX_BYTES=8*1024*1024
  // Original minimal seed, MIT like PocketSurf. Not copied from a third-party list.
  val BUILTIN_RULES="""
! PocketSurf starter rules — MIT, copyright 2026 SilasDV
||doubleclick.net^
||googlesyndication.com^
||googleadservices.com^
||adservice.google.com^
||adsystem.com^
||adnxs.com^
||adsrvr.org^
||taboola.com^
||outbrain.com^
||criteo.com^
||criteo.net^
||advertising.com^
||rubiconproject.com^
||pubmatic.com^
||openx.net^
##[data-ad-slot]
##ins.adsbygoogle
##.ad-banner
""".trimIndent()
 }
}
