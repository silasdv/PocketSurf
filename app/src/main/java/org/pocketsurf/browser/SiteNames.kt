package org.pocketsurf.browser

import android.content.SharedPreferences
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URI
import java.util.Locale

/** Names learned from already loaded pages. No background page requests. */
class SiteNames(private val prefs:SharedPreferences) {
 private val names=linkedMapOf<String,String>()
 init{runCatching{val json=JSONObject(prefs.getString("site_names","{}").orEmpty());json.keys().forEach{key->clean(json.optString(key))?.let{names[key]=it}}}}
 fun name(visit:Visit):String {
  names[SearchDestinations.websiteKey(visit.url)]?.let{return it}
  val fallback=fallback(visit.url)
  val title=clean(visit.title)
  return title?.takeIf{it.filter(Char::isLetterOrDigit).equals(fallback.filter(Char::isLetterOrDigit),true)}?:fallback
 }
 fun remember(url:String,value:String?):Boolean {
  val name=clean(value)?:return false;val key=SearchDestinations.websiteKey(url)
  if(names[key]==name)return false
  names.remove(key);names[key]=name;while(names.size>256)names.remove(names.keys.first())
  prefs.edit().putString("site_names",JSONObject(names.toMap()).toString()).apply();return true
 }
 companion object {
  // Fixed read-only DOM inspection at navigation completion, never per bubble or keystroke.
  const val SCRIPT="(function(){var og=document.querySelector('meta[property=\"og:site_name\" i]');var app=document.querySelector('meta[name=\"application-name\" i]');return JSON.stringify({name:(og&&og.content)||(app&&app.content)||''});})()"
  fun decode(raw:String):String?=runCatching{val decoded=JSONTokener(raw).nextValue() as? String?:return null;clean(JSONObject(decoded).optString("name"))}.getOrNull()
  fun clean(value:String?):String? {
   val s=value.orEmpty().filterNot{it.isISOControl()||it in '\u202a'..'\u202e'||it in '\u2066'..'\u2069'}.replace(Regex("\\s+")," ").trim()
   if(s.isEmpty()||s.length>60||s.any{it=='<'||it=='>'}||s.contains("://")||s.startsWith("www.",true)||Regex("^[^\\s]+\\.[a-zA-Z]{2,}(?:/.*)?$").matches(s))return null
   return s
  }
  fun fallback(url:String):String {
   val host=runCatching{URI(url).host.lowercase(Locale.ROOT).removePrefix("www.")}.getOrDefault("")
   val known=linkedMapOf("github.com" to "GitHub","gitlab.com" to "GitLab","youtube.com" to "YouTube","youtu.be" to "YouTube","reddit.com" to "Reddit","wikipedia.org" to "Wikipedia","stackoverflow.com" to "Stack Overflow","google.com" to "Google","bing.com" to "Bing","duckduckgo.com" to "DuckDuckGo","twitch.tv" to "Twitch","amazon.com" to "Amazon")
   known.entries.firstOrNull{host==it.key||host.endsWith("."+it.key)}?.let{return it.value}
   val parts=host.split('.');val suffix=parts.takeLast(2).joinToString(".")
   val token=parts.getOrNull(parts.size-if(suffix in setOf("co.uk","org.uk","com.au","co.jp","com.br","co.nz","co.in"))3 else 2)?:parts.firstOrNull().orEmpty()
   return token.split('-','_').filter{it.isNotBlank()}.joinToString(" "){it.replaceFirstChar{c->c.titlecase(Locale.ROOT)}}.ifBlank{"Website"}
  }
 }
}
