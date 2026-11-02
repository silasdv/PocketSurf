package org.pocketsurf.browser

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Explicit allowlist: no history, tabs, cookies, website storage or passwords. */
object BackupCodec {
 const val MAX_BYTES=2*1024*1024
 private val keys=setOf("bookmarks","home_shortcuts","theme","accent","favorites_bar","home_mode","home_url","search","search_template","search_name","ps_labels","comfort_touch","default_text_zoom","reader_size","site_text_zoom","pointer_size","pointer_style","pointer_fill","pointer_outline","mouse_speed","mouse_acceleration","controls_layout_version")
 fun allowed(k:String)=k in keys||Control.entries.any{k=="key_${it.name}"}
 fun export(p:SharedPreferences):String {ControllerBindings(p);val values=JSONObject().put("controls_layout_version",2);p.all.filterKeys{allowed(it)}.forEach{(k,v)->values.put(k,v)};Control.entries.forEach{values.put("key_${it.name}",p.getInt("key_${it.name}",it.defaultKey))};return JSONObject().put("format","PocketSurf backup").put("version",1).put("values",values).toString(2).also{require(it.toByteArray(Charsets.UTF_8).size<=MAX_BYTES){"Backup exceeds 2 MB"}}}
 fun validate(raw:String):JSONObject {
  require(raw.toByteArray(Charsets.UTF_8).size<=MAX_BYTES){"Backup is too large"};val root=JSONObject(raw);require(root.optString("format")=="PocketSurf backup"&&root.optInt("version")==1){"Unsupported backup format"};val source=root.getJSONObject("values");val clean=JSONObject()
  for(k in source.keys()){require(allowed(k)){"Unexpected backup field: $k"};val v=source.get(k);val valid=when{ k in setOf("favorites_bar","ps_labels","comfort_touch","mouse_acceleration")->v is Boolean;(k=="default_text_zoom"||k=="reader_size"||k=="mouse_speed"||k=="pointer_size"||k=="controls_layout_version")||k.startsWith("key_")->v is Int;else->v is String};require(valid){"Invalid backup field: $k"};clean.put(k,v)}
  for(k in listOf("bookmarks","home_shortcuts")){if(!clean.has(k))continue;val list=JSONArray(clean.getString(k));require(list.length()<=if(k=="bookmarks")500 else 6){"Too many $k"};for(i in 0 until list.length()){val entry=list.getJSONObject(i);StartPageStore.requireUrl(entry.getString("url"));require(entry.optString("title").length<=1024){"Title is too long"};if(k=="home_shortcuts")require(entry.getString("id").length in 1..80){"Invalid pin ID"}}}
  if(clean.has("home_url")&&clean.getString("home_url").isNotBlank())StartPageStore.requireUrl(clean.getString("home_url"))
  if(clean.has("search_template"))require(SearchEngines.validateTemplate(clean.getString("search_template"))){"Invalid search engine"}
  if(clean.has("controls_layout_version"))require(clean.getInt("controls_layout_version") in 1..2)
  if(clean.has("pointer_fill"))require(clean.getString("pointer_fill") in setOf("white","black","accent"))
  if(clean.has("pointer_outline"))require(clean.getString("pointer_outline") in setOf("white","black","none"))
  if(clean.has("pointer_size"))require(clean.getInt("pointer_size") in 50..220)
  if(clean.has("pointer_style"))require(clean.getString("pointer_style") in (MousePointer.styles.keys+MousePointer.legacy))
  if(clean.has("mouse_speed"))require(clean.getInt("mouse_speed") in 300..1800)
  if(clean.has("reader_size"))require(clean.getInt("reader_size") in 14..32){"Invalid reader size"}
  if(clean.has("default_text_zoom"))require(clean.getInt("default_text_zoom") in 75..200){"Invalid text size"}
  for(c in Control.entries){val k="key_${c.name}";if(clean.has(k))require(clean.getInt(k) in ((19..23)+(96..110)+(188..203))){"Invalid controller button"}}
  if(clean.has("site_text_zoom")){val zoom=JSONObject(clean.getString("site_text_zoom"));require(zoom.length()<=500);for(k in zoom.keys()){require(PageIdentity.origin(k)==k);require(zoom.getInt(k) in 75..200)}}
  if(Control.entries.any{clean.has("key_${it.name}")}){val assigned=Control.entries.map{val default=if(clean.optInt("controls_layout_version")<2&&it==Control.SEARCH)108 else if(clean.optInt("controls_layout_version")<2&&it==Control.MENU)109 else it.defaultKey;clean.optInt("key_${it.name}",default)};require(assigned.distinct().size==Control.entries.size){"Controller buttons conflict"};Control.entries.forEachIndexed{i,c->clean.put("key_${c.name}",assigned[i])}}
  return clean
 }
 fun apply(p:SharedPreferences,values:JSONObject){val edit=p.edit();for(k in values.keys())when(val v=values.get(k)){is Boolean->edit.putBoolean(k,v);is Int->edit.putInt(k,v);is Long->edit.putLong(k,v);is String->edit.putString(k,v)};if(!values.has("controls_layout_version")||values.optInt("controls_layout_version")<2)edit.putBoolean("inline_controls_v1",false)else edit.putBoolean("inline_controls_v1",true);edit.apply();ControllerBindings(p)}
 fun summary(v:JSONObject):String {val saved=JSONArray(v.optString("bookmarks","[]"));val pins=JSONArray(v.optString("home_shortcuts","[]"));val urls=mutableSetOf<String>();for(a in listOf(saved,pins))for(i in 0 until a.length())urls.add(a.getJSONObject(i).getString("url"));return "${urls.size} bookmarks · ${pins.length()} pinned"}
}
