package org.pocketsurf.browser

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class CookieConsentTest {
 private val context:Context=RuntimeEnvironment.getApplication()
 @Before fun clear(){context.getSharedPreferences("cookie-consent",Context.MODE_PRIVATE).edit().clear().commit()}
 @Test fun enabledDefaultsToTrueAndPersists(){val c=CookieConsent(context);assertTrue(c.enabled);c.enabled=false;assertFalse(CookieConsent(context).enabled);assertTrue(c.script("https://example.test").endsWith("configure(false);"))}
 @Test fun siteExceptionIsNormalizedAndHostSpecific(){val c=CookieConsent(context);c.setSiteExcluded("https://EXAMPLE.test./path",true);assertTrue(CookieConsent(context).isSiteExcluded("https://example.test/else"));assertFalse(c.isSiteExcluded("https://sub.example.test"));assertTrue(c.script("https://example.test").endsWith("configure(false);"));assertTrue(c.script("https://another.test").endsWith("configure(true);"));assertFalse(c.toggleSiteException("https://example.test"));assertTrue(c.script("https://example.test").endsWith("configure(true);"))}
 @Test fun invalidOrNonWebUrlsNeverActivateAutomation(){val c=CookieConsent(context);for(url in listOf("file:///tmp/test","javascript:alert(1)","about:blank","not a url","https://")){c.setSiteExcluded(url,true);assertFalse(c.isSiteExcluded(url));assertTrue(c.script(url).endsWith("configure(false);"))}}
}
