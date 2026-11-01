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
 @Test fun eightProvidersHaveValidTemplates(){assertTrue(SearchEngines.entries.size>=8);assertEquals("Google",SearchEngines.default.name);SearchEngines.entries.forEach{assertTrue(it.name,SearchEngines.validateTemplate(it.template));assertFalse(it.search("a b & c").contains("{query}"));assertTrue(it.search("a b & c").contains("a+b+%26+c"))}}
 @Test fun legacyPrefixAndCustomSuffixArePreserved(){val p=RuntimeEnvironment.getApplication().getSharedPreferences("engine-test",0);p.edit().putString("search","https://www.bing.com/search?q=").apply();assertEquals("Bing",SearchEngines.name(p));val custom=SearchEngines.custom("Custom","https://example.com/search?term={query}&mode=all");SearchEngines.select(p,custom);assertEquals("https://example.com/search?term=a+b&mode=all",SearchEngines.resolve(p).search("a b"));assertEquals("Custom",SearchEngines.name(p))}
 @Test fun dangerousOrAmbiguousTemplatesAreRejected(){for(t in listOf("javascript:{query}","file://host/{query}","https://{query}.example.com/","https://example.com/?q=no-placeholder","https://example.com/?q={query}&q2={query}","https://user:pass@example.com/?q={query}","https://example.com/#q={query}","https://example.com/?q={query}\n"))assertFalse(t,SearchEngines.validateTemplate(t))}
}
