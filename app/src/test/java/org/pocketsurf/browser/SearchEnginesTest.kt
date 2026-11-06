package org.pocketsurf.browser

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class SearchEnginesTest {
 @Test fun supportedProviderHasValidEncodedSubmission(){assertEquals(listOf("Google"),SearchEngines.entries.map{it.name});val engine=SearchEngines.default;assertTrue(SearchEngines.validateTemplate(engine.template));assertEquals("https://www.google.com/search?q=a+b+%26+c",engine.search("a b & c"))}
 @Test fun legacyProvidersAndCustomPreferencesMigrateWithoutTouchingHistory(){
  val p=RuntimeEnvironment.getApplication().getSharedPreferences("engine-test",0)
  for(value in listOf("https://www.bing.com/search?q=","https://duckduckgo.com/?q=","https://search.brave.com/search?q=","https://www.ecosia.org/search?q=","https://www.startpage.com/sp/search?query=","https://search.yahoo.com/search?p=","https://en.wikipedia.org/w/index.php?search=","https://custom.test/?term={query}")){
   p.edit().putString("search_template",value).putString("search_name","Old").putString("history","keep-history").putString("bookmarks","keep-bookmarks").putString("search_queries","keep-searches").commit()
   assertEquals("google",SearchEngines.resolve(p).id);assertEquals(SearchEngines.default.template,p.getString("search_template",null));assertEquals("keep-history",p.getString("history",null));assertEquals("keep-bookmarks",p.getString("bookmarks",null));assertEquals("keep-searches",p.getString("search_queries",null))
  }
 }
 @Test fun dangerousOrAmbiguousTemplatesAreRejected(){for(t in listOf("javascript:{query}","file://host/{query}","https://{query}.example.com/","https://example.com/?q=no-placeholder","https://example.com/?q={query}&q2={query}","https://user:pass@example.com/?q={query}","https://example.com/#q={query}","https://example.com/?q={query}\n"))assertFalse(t,SearchEngines.validateTemplate(t))}
}
