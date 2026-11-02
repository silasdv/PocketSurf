package org.pocketsurf.browser

import android.graphics.Rect
import android.view.*
import android.webkit.WebView
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Focus103RegressionTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0)
 @Before fun fixture(){prefs.edit().clear().putBoolean(SetupActivity.COMPLETE,true).commit();RecordingWebViewShadow.clearEvidence()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun views(a:BrowserActivity)=all(a.window.decorView)
 private fun settle(a:BrowserActivity){repeat(3){views(a).forEach{it.forceLayout()};a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));layout(0,0,960,540)};TestFrameClock.advance(Duration.ofMillis(16))}}
 private fun start(count:Int=1,home:Boolean=false):org.robolectric.android.controller.ActivityController<BrowserActivity>{val sessions=JSONArray();repeat(count){sessions.put(JSONObject().put("url",if(home)""else"https://fixture$it.test/").put("title","Site $it").put("home",home))};prefs.edit().putString("sessions_v2",sessions.toString()).putInt("active",0).commit();return Robolectric.buildActivity(BrowserActivity::class.java).setup().visible().also{settle(it.get())}}
 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun desc(a:BrowserActivity,s:String)=views(a).first{it.contentDescription?.toString()==s&&it.isShown}
 private fun click(a:BrowserActivity,s:String){var v:View=views(a).firstOrNull{it.contentDescription?.toString()==s&&it.isShown}?:views(a).filterIsInstance<TextView>().first{it.text.toString()==s&&it.isShown};while(!v.hasOnClickListeners())v=v.parent as View;v.performClick();settle(a)}
 private fun stick(a:BrowserActivity,y:Float){val p=MotionEvent.PointerProperties().apply{id=0};val xy=MotionEvent.PointerCoords().apply{setAxisValue(MotionEvent.AXIS_RZ,y)};val e=MotionEvent.obtain(0,0,MotionEvent.ACTION_MOVE,1,arrayOf(p),arrayOf(xy),0,0,1f,1f,0,0,InputDevice.SOURCE_JOYSTICK,0);a.dispatchGenericMotionEvent(e);e.recycle()}
 private fun rect(v:View)=Rect().also{assertTrue("Bounds ${v.contentDescription}: ${v.width}x${v.height}, visibility=${v.visibility}, parent=${v.parent}",v.getGlobalVisibleRect(it))}
 @Test fun closingWithRSelectsNextThenPreviousThenNewTabWhenEmpty(){val c=start(3,true);val a=c.get();BrowserTestActions.openTabs(a);desc(a,"Tab: Site 1").requestFocus();key(a,99);assertEquals("Tab: Site 2",a.window.decorView.findFocus()?.contentDescription);desc(a,"Tab: Site 2").requestFocus();key(a,99);assertEquals("Current tab: Site 0",a.window.decorView.findFocus()?.contentDescription);desc(a,"Current tab: Site 0").requestFocus();key(a,99);assertEquals("New tab",a.window.decorView.findFocus()?.contentDescription);assertTrue(views(a).filterIsInstance<TextView>().any{it.text=="0 tabs · choose a page to continue"});c.pause().stop().destroy()}
 @Test fun touchClosingACardSelectsItsSuccessor(){val c=start(3,true);val a=c.get();BrowserTestActions.openTabs(a);click(a,"Close Site 1");assertEquals("Tab: Site 2",a.window.decorView.findFocus()?.contentDescription);c.pause().stop().destroy()}
 @Test fun closingSelectedCardIsImmediateAndUndoRestoresIt(){val c=start(2);val a=c.get();BrowserTestActions.openTabs(a);desc(a,"Current tab: Site 0").requestFocus();key(a,99);assertFalse(views(a).filterIsInstance<TextView>().any{it.text=="Close this tab?"});click(a,"Undo");assertTrue(views(a).any{it.contentDescription=="Current tab: Site 0"});c.pause().stop().destroy()}
 @Test fun bookmarkBarKeepsAdditionalSpaceBeforeWebContent(){prefs.edit().putBoolean("favorites_bar",true).commit();BrowserData(prefs).record("https://saved.test/","Saved","bookmarks");val c=start();val a=c.get();val bar=rect(desc(a,"Bookmarks bar"));val page=rect(views(a).filterIsInstance<WebView>().single().parent as View);assertEquals(3,page.top-bar.bottom);assertTrue(desc(a,"Bookmark: Saved").isShown);c.pause().stop().destroy()}
 @Test fun aBusyRouteProbeCannotBlockUpwardDocumentScrollingUntilLoadingFinishes(){val c=start();val a=c.get();val web=views(a).filterIsInstance<WebView>().single();web.scrollTo(0,1500);web.webChromeClient!!.onProgressChanged(web,35);key(a,20);RecordingWebViewShadow.clearEvidence();stick(a,-1f);TestFrameClock.advance(Duration.ofMillis(16));val shadow=Shadows.shadowOf(web);assertTrue(shadow.lastEvaluatedJavascript.contains(".canNativeScroll("));val pending=shadow.lastEvaluatedJavascriptCallback;assertTrue(RecordingWebViewShadow.nativeScroll.isEmpty());TestFrameClock.advance(Duration.ofMillis(144));assertTrue("Busy JS must not block native upward scrolling",RecordingWebViewShadow.nativeScroll.any{it.second<0});val count=RecordingWebViewShadow.nativeScroll.size;pending.onReceiveValue("false");TestFrameClock.advance(Duration.ofMillis(32));assertEquals(count,RecordingWebViewShadow.nativeScroll.size);assertTrue(RecordingWebViewShadow.scripts.any{it.contains(".setScroll(0.0,-1800.0)")});stick(a,0f);c.pause().stop().destroy()}
 @Test fun loadCallbacksPreserveHeldRightStickAndAllowReversingBeforeCompletion(){val c=start();val a=c.get();val web=views(a).filterIsInstance<WebView>().single();web.scrollTo(0,1500);stick(a,1f);TestFrameClock.advance(Duration.ofMillis(48));web.webViewClient!!.onPageStarted(web,web.url!!,null);RecordingWebViewShadow.nativeScroll.clear();TestFrameClock.advance(Duration.ofMillis(64));assertTrue("A page start must preserve the held right stick",RecordingWebViewShadow.nativeScroll.any{it.second>0});web.webChromeClient!!.onProgressChanged(web,45);RecordingWebViewShadow.nativeScroll.clear();stick(a,-1f);TestFrameClock.advance(Duration.ofMillis(64));assertTrue("Reverse scrolling must work below 100% progress",RecordingWebViewShadow.nativeScroll.any{it.second<0});stick(a,0f);c.pause().stop().destroy()}
 @Test fun githubDefaultsMigrateWithoutChangingRepositoryLinksOrSavedTimes(){prefs.edit().putString("home_shortcuts",JSONArray().put(JSONObject().put("id","original-pin").put("title","GitHub").put("url","https://github.com/")).toString()).commit();val data=BrowserData(prefs);data.record("https://github.com/","GitHub","bookmarks",123L);data.record("https://github.com/silasdv/PadBoard","PadBoard","bookmarks",456L);val store=StartPageStore(prefs);assertEquals(StartPageStore.GITHUB_DEFAULT,store.shortcuts().single().url);assertEquals("original-pin",store.shortcuts().single().id);assertTrue(data.entries("bookmarks").contains(Visit(StartPageStore.GITHUB_DEFAULT,"GitHub",123L)));assertTrue(data.entries("bookmarks").contains(Visit("https://github.com/silasdv/PadBoard","PadBoard",456L)));assertEquals(StartPageStore.GITHUB_DEFAULT,StartPageStore.popular.first{it.title=="GitHub"}.url)}
 @Test fun githubQuickAccessNavigatesToTheCheckedExploreAddress(){val site=StartPageStore.popular.first{it.title=="GitHub"};StartPageStore(prefs).put(site);val c=start(0,true);val a=c.get();click(a,"Pinned bookmark: GitHub");assertEquals(StartPageStore.GITHUB_DEFAULT,views(a).filterIsInstance<WebView>().single().url);c.pause().stop().destroy()}

}
