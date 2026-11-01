package org.pocketsurf.browser

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class StartPageStoreTest {
 private fun prefs()=RuntimeEnvironment.getApplication().getSharedPreferences("home-test",0)
 @Test fun newHomeHasNoPinnedSitesAndSettingsPersist(){val p=prefs();val s=StartPageStore(p);assertTrue(s.shortcuts().isEmpty());assertEquals("pocket",s.homeMode);s.homeUrl="https://example.com/start";s.homeMode="custom";assertEquals("custom",StartPageStore(p).homeMode);assertEquals("https://example.com/start",StartPageStore(p).homeUrl)}
 @Test fun shortcutsCanBeEditedReorderedAndRemoved(){val s=StartPageStore(prefs());val a=s.put("First","https://first.test/");val b=s.put("Second","https://second.test/");s.move(b.id,0);assertEquals(listOf(b.id,a.id),s.shortcuts().map{it.id});s.put("Renamed","https://first.test/new",a.id);assertEquals("Renamed",s.shortcuts()[1].title);assertTrue(s.remove(b.id));assertEquals(a.id,s.shortcuts().single().id)}
 @Test fun maxSixAndDuplicatesDoNotSilentlyOverwrite(){val s=StartPageStore(prefs());repeat(6){s.put("$it","https://site$it.test/")};try{s.put("Seventh","https://seventh.test/");fail("Expected limit")}catch(_:IllegalArgumentException){};try{s.put("Duplicate","https://site0.test/");fail("Expected duplicate rejection")}catch(_:IllegalArgumentException){};assertEquals(6,s.shortcuts().size)}
 @Test fun suggestionsUseLocalFrequencyThenRecencyAndNeverAutoPin(){val s=StartPageStore(prefs());val history=listOf(Visit("https://frequent.test/one","One",1),Visit("https://frequent.test/one","One",2),Visit("https://recent.test/","Recent",100));val list=s.suggestions(history);assertEquals("https://frequent.test/one",list.first().url);assertTrue(s.shortcuts().isEmpty());s.put("Pinned","https://frequent.test/one");assertTrue(s.suggestions(history).none{it.url.contains("frequent.test")})}
 @Test fun fallbackIsAnUnpinnedPopularPickerAndInvalidHistoryIsIgnored(){val s=StartPageStore(prefs());assertTrue(s.suggestions(emptyList()).size>=6);assertEquals(s.suggestions(emptyList()),s.suggestions(listOf(Visit("javascript:alert(1)","bad",0))));assertTrue(s.shortcuts().isEmpty())}
 @Test fun onlyHttpAddressesWithoutEmbeddedCredentialsAreAccepted(){for(url in listOf("javascript:alert(1)","file:///etc/passwd","https://user:pass@example.com/","https://example.com/\nfoo")){try{StartPageStore.requireUrl(url);fail(url)}catch(_:IllegalArgumentException){}};assertEquals("https://example.com",StartPageStore.origin("https://EXAMPLE.com:443/path?q=1"))}
}
