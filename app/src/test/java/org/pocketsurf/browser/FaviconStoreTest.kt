package org.pocketsurf.browser

import android.graphics.Bitmap
import android.os.Looper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class FaviconStoreTest {
 @Test fun cacheUsesOriginRatherThanIndividualPage(){val store=FaviconStore(RuntimeEnvironment.getApplication());val icon=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);store.put("https://EXAMPLE.com:443/a",icon);assertSame(icon,store.get("https://example.com/b?q=1"));assertNull(store.get("https://other.test/"));store.close();assertNull(store.get("https://example.com/"))}
 @Test fun oversizedWebViewIconsAreBounded(){val store=FaviconStore(RuntimeEnvironment.getApplication());store.put("https://large.test",Bitmap.createBitmap(1024,512,Bitmap.Config.ARGB_8888));val icon=store.get("https://large.test/")!!;assertTrue(icon.width<=256&&icon.height<=256);assertEquals(2,icon.width/icon.height);store.close()}
 @Test fun faviconFetchNeverUsesAProxyOrPlainHttp(){assertEquals("https://example.com/favicon.ico",FaviconStore.fetchUrl("http://example.com/page"));assertEquals("https://example.com:8443/favicon.ico",FaviconStore.fetchUrl("https://example.com:8443/path"));assertNull(FaviconStore.fetchUrl("javascript:alert(1)"));assertNull(FaviconStore.fetchUrl("https://name:secret@example.com/"));assertNull(FaviconStore.originKey("file:///tmp/a"))}
 @Test fun invalidFetchCompletesWithoutAnyNetworkRequest(){val store=FaviconStore(RuntimeEnvironment.getApplication());var called=false;store.fetch("not a url"){assertNull(it);called=true};Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(called);store.close()}
}
