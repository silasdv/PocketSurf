package org.pocketsurf.browser

import android.content.SharedPreferences
import java.net.URI
import java.net.URLEncoder
import java.util.Locale

data class SearchEngine(val id:String,val name:String,val template:String) {
 fun search(query:String)=SearchEngines.search(query,template)
 val prefix:String get()=template.substringBefore("{query}")
}

/** Query templates do not make any request until the user submits a search. */
object SearchEngines {
 val entries=listOf(SearchEngine("google","Google","https://www.google.com/search?q={query}"))
 val default:SearchEngine get()=entries.first()
 fun find(value:String?):SearchEngine?=entries.firstOrNull{it.id==value||it.name.equals(value,true)||it.template==value||it.prefix==value}
 fun resolve(prefs:SharedPreferences):SearchEngine {
  val value=prefs.getString("search_template",null)?:prefs.getString("search",null)
  if(value!=default.template||prefs.getString("search",null)!=default.prefix||prefs.getString("search_name",null)!=default.name)
   prefs.edit().putString("search_template",default.template).putString("search",default.prefix).putString("search_name",default.name).apply()
  return default
 }
 fun select(prefs:SharedPreferences,engine:SearchEngine){require(validateTemplate(engine.template)){"Use an HTTP(S) address with one {query} placeholder"};prefs.edit().putString("search_template",engine.template).putString("search",engine.prefix).putString("search_name",engine.name).apply()}
 fun name(prefs:SharedPreferences)=resolve(prefs).name
 fun validateTemplate(value:String):Boolean {
  if(value.length>4096||value.any{it.isISOControl()}||Regex("\\{query\\}").findAll(value).count()!=1)return false
  if(value.substringAfter("://","").substringBefore('/').substringBefore('?').contains("{query}"))return false
  return try{val uri=URI(value.replace("{query}","pocketsurf"));uri.scheme?.lowercase(Locale.ROOT) in listOf("http","https")&&!uri.host.isNullOrBlank()&&uri.userInfo==null&&uri.port in -1..65535&&uri.fragment==null}catch(_:Exception){false}
 }
 fun custom(name:String,template:String):SearchEngine {require(name.trim().isNotBlank()){ "Give the search engine a name" };require(validateTemplate(template)){"Use an HTTP(S) address with one {query} placeholder"};return SearchEngine("custom",name.trim().take(60),template)}
 fun search(query:String,template:String=default.template):String {require(validateTemplate(template)){"Invalid search template"};return template.replace("{query}",URLEncoder.encode(query,"UTF-8"))}
}
