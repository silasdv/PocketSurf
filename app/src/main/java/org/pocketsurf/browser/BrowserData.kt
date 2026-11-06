package org.pocketsurf.browser

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter

data class Visit(val url:String,val title:String,val time:Long)
class BrowserData(private val prefs:SharedPreferences){
 fun entries(key:String):List<Visit>{val values=mutableListOf<Visit>();runCatching{val a=JSONArray(prefs.getString(key,"[]"));for(i in 0 until a.length())runCatching{val o=a.getJSONObject(i);values.add(Visit(o.getString("url"),o.optString("title"),o.optLong("time")))}};if(key=="bookmarks")runCatching{val pins=JSONArray(prefs.getString("home_shortcuts","[]"));for(i in 0 until pins.length())runCatching{val o=pins.getJSONObject(i);val url=StartPageStore.requireUrl(o.getString("url"));if(values.none{it.url==url})values.add(Visit(url,o.optString("title"),0))}};return if(key=="bookmarks")values.distinctBy{it.url}else values}
 fun record(url:String,title:String,key:String="history",time:Long=System.currentTimeMillis()){
  if(!url.startsWith("http")||url.contains("pocketsurf.invalid"))return
  val old=entries(key);if(key=="history"&&old.firstOrNull()?.let {it.url==url&&time-it.time<30_000}==true)return
  write(key,(listOf(Visit(url,title,time))+if(key=="bookmarks")old.filter {it.url!=url}else old).take(if(key=="history")2000 else 500))
 }
 fun write(key:String,items:List<Visit>){val a=JSONArray();items.forEach {a.put(JSONObject().put("url",it.url).put("title",it.title).put("time",it.time))};val edit=prefs.edit().putString(key,a.toString());if(key=="history"){
  val removed=entries("history").filterNot{it in items}.map{searchDestination(it.url)}.toSet()-items.map{searchDestination(it.url)}.toSet()
  val searches=if(items.isEmpty())emptyList()else entries("search_queries").filterNot{searchDestination(it.url) in removed}
  val queries=JSONArray();searches.forEach{queries.put(JSONObject().put("url",it.url).put("title",it.title).put("time",it.time))};edit.putString("search_queries",queries.toString())
 };if(key=="bookmarks")runCatching{val pins=JSONArray(prefs.getString("home_shortcuts","[]"));val kept=JSONArray();for(i in 0 until pins.length()){val pin=pins.getJSONObject(i);if(items.any{it.url==pin.optString("url")})kept.put(pin)};edit.putString("home_shortcuts",kept.toString())};edit.apply()}
 /** Intentional searches only. Visited URLs are never reverse-engineered into recent queries. */
 fun recordSearch(query:String,url:String,time:Long=System.currentTimeMillis()){
  val text=query.filterNot{it.isISOControl()}.trim().take(1024);if(text.isBlank())return
  write("search_queries",(listOf(Visit(url,text,time))+entries("search_queries").filterNot{it.title.equals(text,true)}).take(200))
 }
 fun recentSearches()=entries("search_queries").sortedByDescending{it.time}.distinctBy{it.title.lowercase(java.util.Locale.ROOT)}
 private fun searchDestination(url:String):String=runCatching{
  val u=java.net.URI(url);val q=u.rawQuery.orEmpty().split('&').firstOrNull{it.substringBefore('=')=="q"}?.substringAfter('=',"")
  if(q==null)SearchDestinations.identity(url)else SearchDestinations.websiteKey(url)+u.path+"?q="+java.net.URLDecoder.decode(q,"UTF-8")
 }.getOrDefault(url)
 fun search(key:String,query:String)=entries(key).filter {it.title.contains(query,true)||it.url.contains(query,true)}.let {if(key=="history")it.sortedByDescending{v->v.time}else it}
 companion object {
  fun day(time:Long,now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()):String {
   if(time==0L)return "Earlier"
   val date=Instant.ofEpochMilli(time).atZone(zone).toLocalDate();val today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
   return when(date){today->"Today";today.minusDays(1)->"Yesterday";else->date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy",java.util.Locale.ENGLISH))}
  }
 }
}
