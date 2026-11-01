package org.pocketsurf.browser

import android.app.DownloadManager
import android.content.Context
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDownloadManager.ShadowRequest

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class SurfDownloadsTest {
 @Test fun enqueueProgressCancelAndRetry(){
  val context=RuntimeEnvironment.getApplication();val service=SurfDownloads(context);val manager=context.getSystemService(DownloadManager::class.java);val shadow=Shadows.shadowOf(manager)
  val item=service.start("https://example.com/file.zip","PocketSurf test",mime="application/zip")
  assertEquals(1,shadow.requestCount);val request=Shadow.extract<ShadowRequest>(shadow.getRequest(item.id));assertTrue(request.destination.toString().contains("Download"));assertEquals("file.zip",request.title.toString())
  request.status=DownloadManager.STATUS_RUNNING;request.totalSize=1000;request.bytesSoFar=500
  val progress=service.items().single();assertEquals(500L,progress.received);assertTrue(SurfDownloads.status(progress).startsWith("50%"))
  service.cancel(item.id);assertEquals(SurfDownloads.CANCELLED,service.items().single().status)
  service.retry(item,"test");assertEquals(1,service.items().size);assertNotEquals(item.id,service.items().single().id)
  assertNull(Shadows.shadowOf(context).nextStartedActivity)
 }
 @Test fun unsupportedDownloadFailsWithoutLaunchingAnApp(){val service=SurfDownloads(RuntimeEnvironment.getApplication());try{service.start("blob:https://example.com/1","test");fail("Expected rejection")}catch(_:IllegalArgumentException){};assertTrue(service.items().isEmpty())}
}
