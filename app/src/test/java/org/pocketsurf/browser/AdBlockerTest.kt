package org.pocketsurf.browser

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class AdBlockerTest {
 @Test fun defaultProtectionBlocksResourcesButNotMainNavigation(){val a=AdBlocker(RuntimeEnvironment.getApplication());assertTrue(a.enabled);assertTrue(a.shouldBlock("https://doubleclick.net/ad.js","https://news.test"));assertFalse(a.shouldBlock("https://doubleclick.net/ad.js","https://news.test",true));assertTrue(a.status().blockedRequests>=1);a.close()}
 @Test fun globalAndSiteOverridesPersistAndRemoveCosmetics(){val c=RuntimeEnvironment.getApplication();val a=AdBlocker(c);a.setSiteAllowed("https://news.test/path",true);assertTrue(a.isSiteAllowed("https://news.test/other"));assertFalse(a.shouldBlock("https://doubleclick.net/ad.js","https://news.test"));assertTrue(a.cosmeticScript("https://news.test").contains("remove"));a.setSiteAllowed("https://news.test",false);assertTrue(a.shouldBlock("https://doubleclick.net/ad.js","https://news.test"));a.enabled=false;val b=AdBlocker(c);assertFalse(b.enabled);assertFalse(b.shouldBlock("https://doubleclick.net/ad.js","https://news.test"));a.close();b.close()}
 @Test fun invalidUpdatePreservesTheLastKnownGoodFilters(){val a=AdBlocker(RuntimeEnvironment.getApplication());val valid="[Adblock Plus 2.0]\n"+(1..120).joinToString("\n"){"||ad$it.example^"};a.installDownloadedFilters(valid.toByteArray());assertTrue(a.shouldBlock("https://ad40.example/banner","https://news.test"));val before=a.status().supportedRules;try{a.installDownloadedFilters("<html>error page</html>".toByteArray());fail("Invalid download must be rejected")}catch(_:java.io.IOException){};assertEquals(before,a.status().supportedRules);assertTrue(a.shouldBlock("https://ad40.example/banner","https://news.test"));assertTrue(a.status().source.contains("EasyList"));a.close()}
 @Test fun cosmeticSelectorsAreEncodedAsDataNotJavascript(){val a=AdBlocker(RuntimeEnvironment.getApplication());val script=a.cosmeticScript("https://news.test");assertTrue(script.contains("insertRule"));assertTrue(script.contains("[data-ad-slot]"));assertTrue(script.contains("setTimeout"));a.close()}
}
