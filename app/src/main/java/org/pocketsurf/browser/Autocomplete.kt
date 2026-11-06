package org.pocketsurf.browser

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Per-screen memory cache; a shared client owns connections, never browser history. */
class Autocomplete(
 private val client:OkHttpClient=sharedClient,
 private val endpoint:String="https://suggestqueries.google.com/complete/search",
 private val now:()->Long={System.nanoTime()/1_000_000}
){
 companion object {
  const val DEBOUNCE_MS=150L
  private val sharedClient=OkHttpClient.Builder().connectTimeout(2,TimeUnit.SECONDS)
   .readTimeout(2,TimeUnit.SECONDS).callTimeout(3,TimeUnit.SECONDS).build()
 }
 data class Result(val items:List<String>,val cached:Boolean,val networkMs:Long,val parseMs:Long)
 private data class Cached(val items:List<String>,val time:Long)
 private val cache=object:LinkedHashMap<String,Cached>(32,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<String,Cached>?)=size>32}
 private var active:Call?=null
 private var activeKey:String?=null
 private var generation=0L
 private var deliver:((Result)->Unit)?=null
 @Synchronized fun cancel(){generation++;active?.cancel();active=null;activeKey=null;deliver=null}
 @Synchronized fun cached(query:String,locale:String=Locale.getDefault().toLanguageTag()):Result? {
  val key=locale+"\n"+query
  val hit=cache[key]?:return null
  if(now()-hit.time>300_000){cache.remove(key);return null}
  return Result(hit.items,true,0,0)
 }
 @Synchronized fun request(query:String,locale:String=Locale.getDefault().toLanguageTag(),callback:(Result)->Unit){
  cached(query,locale)?.let{callback(it);return}
  val key=locale+"\n"+query
  if(activeKey==key){deliver=callback;return}
  cancel();val token=generation
  val url=endpoint.toHttpUrl().newBuilder().addQueryParameter("client","chrome").addQueryParameter("q",query).addQueryParameter("hl",locale).build()
  val call=client.newCall(Request.Builder().url(url).header("User-Agent","Mozilla/5.0").build())
  active=call;activeKey=key;deliver=callback
  val start=now()
  fun finish(result:Result,remember:Boolean){
   synchronized(this@Autocomplete){
    if(token!=generation||active!==call)return
    if(remember)cache[key]=Cached(result.items,now())
    val notify=deliver;active=null;activeKey=null;deliver=null
    notify?.invoke(result)
   }
  }
  call.enqueue(object:Callback{
   override fun onFailure(call:Call,e:IOException){finish(Result(emptyList(),false,now()-start,0),false)}
   override fun onResponse(call:Call,response:Response){
    response.use{
     try{
      if(!it.isSuccessful){finish(Result(emptyList(),false,now()-start,0),false);return}
      val body=it.body?.string().orEmpty();val received=now()
      val a=JSONArray(body).getJSONArray(1)
      val items=(0 until a.length()).map{a.getString(it)}.filter{it.isNotBlank()}.distinctBy{it.lowercase(Locale.ROOT)}
      finish(Result(items,false,received-start,now()-received),true)
     }catch(_:Exception){finish(Result(emptyList(),false,now()-start,0),false)}
    }
   }
  })
 }
 @Synchronized fun clear(){cancel();cache.clear()}
}
