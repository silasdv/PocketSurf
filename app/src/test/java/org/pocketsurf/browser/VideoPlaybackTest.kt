package org.pocketsurf.browser

import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Duration

/** Host fullscreen lifecycle tests. Robolectric cannot decode or play real video. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
class VideoPlaybackTest {
 @org.junit.Before fun skipFirstRunForBrowserFixture(){org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().putBoolean("setup_complete",true).commit()}
 private class Callback:WebChromeClient.CustomViewCallback {var hidden=0;override fun onCustomViewHidden(){hidden++}}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun start()=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://video.fixture.test"))).setup().visible().also{settle(it.get())}
 private fun settle(a:BrowserActivity){a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));layout(0,0,960,540)};TestFrameClock.advance(Duration.ofMillis(16))}
 private fun web(a:BrowserActivity)=all(a.window.decorView).filterIsInstance<WebView>().single()
 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,k));a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,k));settle(a)}
 @Test fun fullscreenOwnsFocusAndAwakeStateWithoutAReservedLegendStrip(){val c=start();val a=c.get();val view=FrameLayout(a);val callback=Callback();web(a).webChromeClient!!.onShowCustomView(view,callback);settle(a);assertTrue(view.isShown);assertTrue("Fullscreen must keep the display awake",view.keepScreenOn);assertTrue("Fullscreen controls need focus",view.hasFocus());val legends=all(a.window.decorView).filterIsInstance<HorizontalScrollView>();assertTrue("Fullscreen must hide tab and legend hosts",legends.all{!it.isShown});assertEquals(0,callback.hidden);c.pause().stop().destroy()}
 @Test fun backExitsFullscreenOnceAndRetainsTheSameWebView(){val c=start();val a=c.get();val original=web(a);val view=FrameLayout(a);val callback=Callback();original.webChromeClient!!.onShowCustomView(view,callback);settle(a);key(a,97);assertNull(view.parent);assertFalse(view.keepScreenOn);assertEquals(1,callback.hidden);assertSame(original,web(a));assertFalse(a.isFinishing);original.webChromeClient!!.onHideCustomView();assertEquals("Repeated hide must not notify twice",1,callback.hidden);c.pause().stop().destroy()}
 @Test fun duplicateFullscreenRequestRejectsOnlyTheNewRequest(){val c=start();val a=c.get();val original=web(a);val first=FrameLayout(a);val second=FrameLayout(a);val firstCallback=Callback();val secondCallback=Callback();original.webChromeClient!!.onShowCustomView(first,firstCallback);original.webChromeClient!!.onShowCustomView(second,secondCallback);assertNotNull(first.parent);assertNull(second.parent);assertEquals(0,firstCallback.hidden);assertEquals(1,secondCallback.hidden);key(a,97);assertEquals(1,firstCallback.hidden);c.pause().stop().destroy()}
 @Test fun destructionReleasesFullscreenCallback(){val c=start();val a=c.get();val view=FrameLayout(a);val callback=Callback();web(a).webChromeClient!!.onShowCustomView(view,callback);c.pause().stop().destroy();assertEquals(1,callback.hidden);assertNull(view.parent);assertFalse(view.keepScreenOn)}
 @Test fun videoPlaybackStillRequiresAnExplicitUserGesture(){val c=start();assertTrue(web(c.get()).settings.mediaPlaybackRequiresUserGesture);c.pause().stop().destroy()}
}
