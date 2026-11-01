package org.pocketsurf.browser

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.Locale
import java.util.UUID

data class Shortcut(val id:String=UUID.randomUUID().toString(),val title:String,val url:String)

/** Local-only home settings. A new install deliberately has no pre-pinned websites. */
class StartPageStore(private val prefs:SharedPreferences) {
 init {
  if(!prefs.getBoolean("github_explore_v1",false)){
   fun replace(url:String,title:String)=if(url.trimEnd('/')=="https://github.com"&&title.equals("GitHub",true))GITHUB_DEFAULT else url
   val pins=shortcuts();val next=pins.map{it.copy(url=replace(it.url,it.title))}.distinctBy{it.url};if(next!=pins)write(next)
   val data=BrowserData(prefs);val saved=data.entries("bookmarks");val updated=saved.map{it.copy(url=replace(it.url,it.title))};if(updated!=saved)data.write("bookmarks",updated.distinctBy{it.url})
   prefs.edit().putBoolean("github_explore_v1",true).apply()
  }
  BrowserData(prefs).let{it.write("bookmarks",it.entries("bookmarks"))}
 }

 var homeMode:String
  get()=prefs.getString("home_mode","pocket").takeIf{it in MODES}?:"pocket"
  set(value){require(value in MODES){"Choose PocketSurf, blank or a custom address"};prefs.edit().putString("home_mode",value).apply()}
 var homeUrl:String
  get()=prefs.getString("home_url","").orEmpty()
  set(value){val url=if(value.isBlank())""else requireUrl(value);prefs.edit().putString("home_url",url).apply()}

 fun shortcuts():List<Shortcut> {
  val result=mutableListOf<Shortcut>()
  try {val a=JSONArray(prefs.getString("home_shortcuts","[]"));for(i in 0 until a.length()){
   try {val o=a.getJSONObject(i);val url=requireUrl(o.getString("url"));val id=o.getString("id").take(80)
    if(id.isNotBlank()&&result.none{it.id==id||it.url==url})result.add(Shortcut(id,cleanTitle(o.optString("title"),url),url))
   }catch(_:Exception){}
   if(result.size==MAX_SHORTCUTS)break
  }}catch(_:Exception){}
  return result
 }
 fun put(shortcut:Shortcut):Shortcut {
  val url=requireUrl(shortcut.url);val old=shortcuts();val byId=old.indexOfFirst{it.id==shortcut.id};val duplicate=old.firstOrNull{it.url==url&&it.id!=shortcut.id}
  require(duplicate==null){"This page is already in Pinned bookmarks"}
  require(byId>=0||old.size<MAX_SHORTCUTS){"Pinned bookmarks has six spaces. Remove a page before adding another."}
  val value=Shortcut(shortcut.id.take(80).ifBlank{UUID.randomUUID().toString()},cleanTitle(shortcut.title,url),url)
  val updated=old.toMutableList();if(byId>=0)updated[byId]=value else updated.add(value);write(updated);BrowserData(prefs).record(value.url,value.title,"bookmarks");return value
 }
 fun put(title:String,url:String,id:String?=null)=put(Shortcut(id?:UUID.randomUUID().toString(),title,url))
 fun remove(id:String):Boolean {val old=shortcuts();val next=old.filterNot{it.id==id};if(next.size==old.size)return false;write(next);return true}
 fun move(id:String,toIndex:Int):Boolean {val next=shortcuts().toMutableList();val old=next.indexOfFirst{it.id==id};if(old<0)return false;val item=next.removeAt(old);next.add(toIndex.coerceIn(0,next.size),item);write(next);return true}

 /** Frequency is counted per origin; the most frequently visited page represents each website. */
 fun suggestions(history:List<Visit>):List<Shortcut> {
  val pinned=shortcuts().mapNotNull{origin(it.url)}.toSet()
  val valid=history.mapNotNull {v->try{v.copy(url=requireUrl(v.url))}catch(_:Exception){null}}
  val ranked=valid.groupBy{origin(it.url)}.filterKeys{it!=null&&it !in pinned}.values
   .sortedWith(compareByDescending<List<Visit>>{it.size}.thenByDescending{list->list.maxOf{it.time}})
   .map {visits->val group=visits.groupBy{it.url}.values.maxWith(compareBy<List<Visit>>{it.size}.thenBy{group->group.maxOf{it.time}});val v=group.maxBy{it.time};Shortcut("suggested:"+v.url,cleanTitle(v.title,v.url),v.url)}
  return if(ranked.isNotEmpty())ranked.take(12)else popular.filter{origin(it.url) !in pinned}
 }
 private fun write(items:List<Shortcut>){val a=JSONArray();items.take(MAX_SHORTCUTS).forEach{a.put(JSONObject().put("id",it.id).put("title",it.title).put("url",it.url))};prefs.edit().putString("home_shortcuts",a.toString()).apply()}
 companion object {
  const val GITHUB_DEFAULT="https://github.com/explore"
  const val MAX_SHORTCUTS=6
  private val MODES=setOf("pocket","blank","custom")
  val popular=listOf("Google" to "https://www.google.com/","YouTube" to "https://www.youtube.com/","Wikipedia" to "https://www.wikipedia.org/","Reddit" to "https://www.reddit.com/","GitHub" to GITHUB_DEFAULT,"Twitch" to "https://www.twitch.tv/","Amazon" to "https://www.amazon.com/","Stack Overflow" to "https://stackoverflow.com/").map{(title,url)->Shortcut("popular:$url",title,url)}
  fun requireUrl(value:String):String {
   val s=value.trim();require(s.length<=4096&&!s.any{it.isISOControl()}){"Enter a valid HTTP or HTTPS address"}
   val u=try{URI(s)}catch(_:Exception){throw IllegalArgumentException("Enter a valid HTTP or HTTPS address")}
   require(u.scheme?.lowercase(Locale.ROOT) in listOf("http","https")&&!u.host.isNullOrBlank()&&u.userInfo==null&&u.port in -1..65535){"Enter a valid HTTP or HTTPS address"}
   return u.toASCIIString()
  }
  fun origin(value:String):String?=try{val u=URI(requireUrl(value));val scheme=u.scheme.lowercase(Locale.ROOT);val port=if((scheme=="https"&&u.port==443)||(scheme=="http"&&u.port==80)) -1 else u.port;URI(scheme,null,u.host.lowercase(Locale.ROOT),port,null,null,null).toASCIIString()}catch(_:Exception){null}
  private fun cleanTitle(value:String,url:String)=value.filterNot{it.isISOControl()}.trim().take(100).ifBlank{URI(url).host.removePrefix("www.")}
 }
}
