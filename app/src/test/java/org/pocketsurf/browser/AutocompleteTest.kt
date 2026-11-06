package org.pocketsurf.browser

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class) @Config(sdk=[33])
class AutocompleteTest {
 private fun response(value:String)=MockResponse().setBody("[\"q\",[\"$value\",\"$value\",\"\"]]")
 @Test fun cachedRepeatedQueryReusesResponseAndPreservesEncoding(){
  val server=MockWebServer();server.start()
  val service=Autocomplete(OkHttpClient(),server.url("/complete").toString())
  try{
   server.enqueue(response("café & games"));val done=CountDownLatch(1)
   service.request("café & games","de"){assertEquals(listOf("café & games"),it.items);done.countDown()}
   assertTrue(done.await(3,TimeUnit.SECONDS))
   val request=server.takeRequest(1,TimeUnit.SECONDS)!!
   assertEquals("café & games",request.requestUrl!!.queryParameter("q"));assertEquals("de",request.requestUrl!!.queryParameter("hl"))
   val hit=service.cached("café & games","de")!!;assertTrue(hit.cached)
   service.request("café & games","de"){assertTrue(it.cached)}
   assertEquals(1,server.requestCount)
  }finally{service.clear();server.shutdown()}
 }
 @Test fun newerRequestDoesNotWaitForSlowOldResponseAndOldCannotPublish(){
  val server=MockWebServer();server.start();val client=OkHttpClient.Builder().callTimeout(2,TimeUnit.SECONDS).build()
  val service=Autocomplete(client,server.url("/").toString());val old=AtomicInteger()
  try{
   server.enqueue(response("obsolete").setBodyDelay(1200,TimeUnit.MILLISECONDS));server.enqueue(response("current"))
   service.request("old"){old.incrementAndGet()};assertNotNull(server.takeRequest(1,TimeUnit.SECONDS))
   val done=CountDownLatch(1);val started=System.nanoTime()
   service.request("new"){assertEquals(listOf("current"),it.items);done.countDown()}
   assertTrue(done.await(1,TimeUnit.SECONDS));assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started)<1000)
   assertEquals(0,old.get())
  }finally{service.clear();server.shutdown()}
 }
 @Test fun identicalInflightRequestsAreCoalescedAndFailuresFinishEmpty(){
  val server=MockWebServer();server.start();val service=Autocomplete(OkHttpClient(),server.url("/").toString())
  try{
   server.enqueue(response("same").setBodyDelay(100,TimeUnit.MILLISECONDS))
   val done=CountDownLatch(1);service.request("same"){};service.request("same"){done.countDown()}
   assertTrue(done.await(2,TimeUnit.SECONDS));assertEquals(1,server.requestCount)
   server.enqueue(MockResponse().setResponseCode(503));val failed=CountDownLatch(1)
   service.request("offline"){assertTrue(it.items.isEmpty());failed.countDown()}
   assertTrue(failed.await(2,TimeUnit.SECONDS));assertNull(service.cached("offline"))
  }finally{service.clear();server.shutdown()}
 }
 @Test fun cacheIsBoundedExpiresAndCanBeCleared(){
  val server=MockWebServer();server.start();var now=0L
  val service=Autocomplete(OkHttpClient(),server.url("/").toString(),{now})
  try{
   repeat(34){i->server.enqueue(response("result$i"));val done=CountDownLatch(1);service.request("query$i"){done.countDown()};assertTrue(done.await(2,TimeUnit.SECONDS))}
   assertNull(service.cached("query0"));assertNotNull(service.cached("query33"));now=300_001;assertNull(service.cached("query33"))
   assertEquals(150L,Autocomplete.DEBOUNCE_MS);service.clear()
  }finally{service.clear();server.shutdown()}
 }
}
