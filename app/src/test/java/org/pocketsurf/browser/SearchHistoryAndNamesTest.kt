package org.pocketsurf.browser

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class SearchHistoryAndNamesTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("search-history-test",0)
 @Before fun clear(){prefs.edit().clear().commit()}
 @Test fun intentionalQueriesAreRecentDeduplicatedAndPersistent(){
  val data=BrowserData(prefs);data.record("https://www.google.com/search?q=untracked","Search page")
  assertTrue(data.recentSearches().isEmpty())
  data.recordSearch("Android WebView","https://www.google.com/search?q=Android+WebView",10)
  data.recordSearch("Git commands","https://www.google.com/search?q=Git+commands",20)
  data.recordSearch("android webview","https://www.google.com/search?q=android+webview",30)
  assertEquals(listOf("android webview","Git commands"),BrowserData(prefs).recentSearches().map{it.title})
 }
 @Test fun deletingAssociatedHistoryAndClearingHistoryRemoveRecentQueries(){
  val data=BrowserData(prefs);val url="https://www.google.com/search?q=android+webview"
  data.recordSearch("android webview",url,10);data.record(url+"&source=app","Results",time=20);data.record("https://other.test/","Other",time=30)
  data.write("history",data.entries("history").filterNot{it.url.contains("google.com")});assertTrue(data.recentSearches().isEmpty())
  data.recordSearch("New query","https://www.google.com/search?q=New+query",40)
  data.write("history",emptyList());assertTrue(data.recentSearches().isEmpty())
 }
 @Test fun removingOneVisitDoesNotEraseAStillVisitedSearch(){
  val data=BrowserData(prefs);val url="https://www.google.com/search?q=android"
  data.recordSearch("android",url,10);data.record(url,"Android",time=20);data.record(url,"Android",time=60_000)
  data.write("history",data.entries("history").take(1));assertEquals("android",data.recentSearches().single().title)
 }
 @Test fun bookmarkClearingDoesNotClearHistoryOrRecentSearches(){
  val data=BrowserData(prefs);data.record("https://example.com/","Example");data.recordSearch("android","https://www.google.com/search?q=android")
  repeat(20){data.record("https://bookmark$it.test/","Bookmark $it","bookmarks")}
  assertEquals(20,data.entries("bookmarks").size);data.write("bookmarks",emptyList());assertEquals(1,data.entries("history").size);assertEquals(1,data.recentSearches().size)
 }
 @Test fun siteNamesPreferCachedMetadataOverDocumentTitlesAndPersist(){
  val visit=Visit("https://www.example.com/repository?id=42","Long SEO document title - Example",0)
  val names=SiteNames(prefs);assertEquals("Example",names.name(visit))
  val payload=JSONObject.quote(JSONObject().put("name","Example App").toString())
  assertTrue(names.remember(visit.url,SiteNames.decode(payload)));assertEquals("Example App",SiteNames(prefs).name(visit))
  assertEquals("GitHub",names.name(Visit("https://github.com/u/repo","My repository - GitHub",0)))
  assertEquals("Wikipedia",names.name(Visit("https://en.wikipedia.org/wiki/Android","Android - Wikipedia",0)))
 }
 @Test fun untrustedLabelsAndVisibleUrlsAreRejected(){
  for(name in listOf("<b>Fake</b>","https://evil.test/path","www.example.com","example.com","x".repeat(100),""))assertNull(name,SiteNames.clean(name))
  assertEquals("Safe Name",SiteNames.clean(" Safe\n Name "))
  assertTrue(SiteNames.SCRIPT.contains("og:site_name"));assertTrue(SiteNames.SCRIPT.contains("application-name"))
 }
}
