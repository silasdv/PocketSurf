package org.pocketsurf.browser

import android.content.Context
import java.net.URI
import java.util.Locale

/** Necessary-only choices for recognized consent banners, with explicit user exceptions. */
class CookieConsent(context:Context) {
 private val app=context.applicationContext
 private val prefs=app.getSharedPreferences("cookie-consent",Context.MODE_PRIVATE)
 private val source by lazy {app.assets.open("cookie-consent.js").bufferedReader().use{it.readText()}}
 var enabled:Boolean
  get()=prefs.getBoolean("enabled",true)
  set(value){prefs.edit().putBoolean("enabled",value).apply()}
 fun isSiteExcluded(url:String):Boolean=host(url)?.let{it in prefs.getStringSet("excluded",emptySet()).orEmpty()}?:false
 fun setSiteExcluded(url:String,excluded:Boolean){val host=host(url)?:return;val sites=prefs.getStringSet("excluded",emptySet()).orEmpty().toMutableSet();if(excluded)sites.add(host)else sites.remove(host);prefs.edit().putStringSet("excluded",sites).apply()}
 fun toggleSiteException(url:String):Boolean {val next=!isSiteExcluded(url);setSiteExcluded(url,next);return next}
 /** Reinject safely on finished navigations; disabled configuration stops an existing helper too. */
 fun script(pageUrl:String):String {
  val active=enabled&&host(pageUrl)!=null&&!isSiteExcluded(pageUrl)
  return source+"\n;window.__pocketSurfCookies && window.__pocketSurfCookies.configure($active);"
 }
 companion object {
  private fun host(url:String):String?=try{val uri=URI(url);if(uri.scheme?.lowercase(Locale.ROOT) !in setOf("http","https"))null else uri.host?.lowercase(Locale.ROOT)?.trimEnd('.')?.takeIf{it.isNotBlank()}}catch(_:Exception){null}
 }
}
