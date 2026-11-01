package org.pocketsurf.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.*
import android.webkit.WebView
import android.widget.EditText
import android.widget.TextView
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.*
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
class BrowserActivityTest {
 @org.junit.Before fun skipFirstRunForBrowserFixture(){org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().putBoolean("setup_complete",true).commit()}
 @org.junit.Before fun clearScriptHistory(){RecordingWebViewShadow.clearEvidence()}
 private fun start(url:String?=null)=Robolectric.buildActivity(BrowserActivity::class.java,Intent().apply{if(url!=null)data=Uri.parse(url)}).setup().visible().also{c->c.get().window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));layout(0,0,960,540)}}
 private fun press(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,k));a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,k));settle(a)}
 private fun settle(a:BrowserActivity){val v=a.window.decorView;v.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));v.layout(0,0,960,540);TestFrameClock.advance(Duration.ofMillis(if(views(a).any{it.contentDescription?.toString()?.endsWith(" popup")==true})360 else 16))}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap {all(v.getChildAt(it))}else emptyList()
 private fun views(a:BrowserActivity)=all(a.window.decorView)
 private fun label(a:BrowserActivity,s:String)=views(a).filterIsInstance<TextView>().firstOrNull{it.text.toString()==s}
 private fun click(a:BrowserActivity,s:String){if(s in listOf("Settings","History")){if(views(a).any{it.contentDescription=="Browser menu popup"})press(a,97);views(a).first{it.contentDescription==s}.performClick();settle(a);return};val v=label(a,s)?:error("Missing $s");var target:View=v;while(!target.hasOnClickListeners()){target=target.parent as? View?:error("No action for $s")};target.performClick();settle(a)}
 private fun motion(vararg values:Pair<Int,Float>):MotionEvent {val p=MotionEvent.PointerProperties().apply{id=0;toolType=MotionEvent.TOOL_TYPE_UNKNOWN};val c=MotionEvent.PointerCoords();values.forEach{(axis,v)->c.setAxisValue(axis,v)};return MotionEvent.obtain(0,0,MotionEvent.ACTION_MOVE,1,arrayOf(p),arrayOf(c),0,0,1f,1f,0,0,InputDevice.SOURCE_JOYSTICK,0)}
 @Test fun emptyBrowserBackExits(){val c=start();press(c.get(),97);assertTrue(c.get().isFinishing);c.pause().stop().destroy()}
 @Test fun androidBackWithNoTabsOpensMenuInsteadOfClosingTheApp(){val c=start();press(c.get(),4);assertFalse(c.get().isFinishing);assertNotNull(label(c.get(),"Browser menu"));c.pause().stop().destroy()}
 @Test fun dpadCanOperateHomeAndMenu(){
  val c=start();val a=c.get();press(a,4);assertNotNull("Menu missing",label(a,"Browser menu"));press(a,96)
  assertNotNull("Controller did not open bookmarks",label(a,"Bookmarks"));assertNotNull(label(a,"0 bookmarks · up to six pinned on your start page"));press(a,97);assertNull(label(a,"Bookmarks"));assertFalse(a.isFinishing);c.pause().stop().destroy()
 }
 @Test fun hatDpadNavigatesBeforeChildren(){
  val c=start();val a=c.get();press(a,4);val e=motion(MotionEvent.AXIS_HAT_Y to 1f);assertTrue(a.dispatchGenericMotionEvent(e));e.recycle();val neutral=motion();a.dispatchGenericMotionEvent(neutral);neutral.recycle();settle(a);press(a,96)
  assertNotNull("Hat did not select Downloads",label(a,"Downloads"));c.pause().stop().destroy()
 }
 @Test fun leftStickNeverLeaksToWebViewNativeScrolling(){val c=start("https://example.com");val a=c.get();val web=views(a).filterIsInstance<WebView>().single();var leaked=false;web.setOnGenericMotionListener{_,_->leaked=true;true};val e=motion(MotionEvent.AXIS_Y to 1f);assertTrue(a.dispatchGenericMotionEvent(e));e.recycle();TestFrameClock.advance(Duration.ofMillis(40));assertFalse(leaked);assertTrue(RecordingWebViewShadow.scripts.any{it.contains(".move('down')")});c.pause().stop().destroy()}
 @Test fun rightStickScrollsAndNeutralStops(){val c=start("https://example.com");val a=c.get();val e=motion(MotionEvent.AXIS_RZ to 1f);assertTrue(a.dispatchGenericMotionEvent(e));e.recycle();TestFrameClock.advance(Duration.ofMillis(50));assertTrue(RecordingWebViewShadow.nativeScroll.any{it.second>0});val neutral=motion();a.dispatchGenericMotionEvent(neutral);neutral.recycle();TestFrameClock.advance(Duration.ofMillis(20));val count=RecordingWebViewShadow.nativeScroll.size;TestFrameClock.advance(Duration.ofMillis(50));assertEquals(count,RecordingWebViewShadow.nativeScroll.size);c.pause().stop().destroy()}
 @Test fun rightRxRyAxesAlsoScroll(){val c=start("https://example.com");val a=c.get();val web=views(a).filterIsInstance<WebView>().single();val e=motion(MotionEvent.AXIS_RY to .6f);a.dispatchGenericMotionEvent(e);e.recycle();TestFrameClock.advance(Duration.ofMillis(60));assertTrue("Root pages must use native scrolling",RecordingWebViewShadow.nativeScroll.any{it.second>0});c.pause().stop().destroy()}
 @Test fun menusBlockPageShouldersAndAnalogScroll(){val c=start("https://example.com");val a=c.get();val web=views(a).filterIsInstance<WebView>().single();press(a,4);web.evaluateJavascript("menu-sentinel",null);press(a,102);val e=motion(MotionEvent.AXIS_RZ to 1f);a.dispatchGenericMotionEvent(e);e.recycle();TestFrameClock.advance(Duration.ofMillis(60));assertEquals("menu-sentinel",Shadows.shadowOf(web).lastEvaluatedJavascript);assertNotNull(label(a,"Browser menu"));c.pause().stop().destroy()}
 @Test fun touchSettingsAreFullPageAndBackReachable(){val c=start();val a=c.get();click(a,"Settings");assertNotNull(label(a,"Settings"));assertNotNull(label(a,"Appearance"));assertTrue(views(a).none{it is WebView});views(a).first{it.contentDescription=="Back"&&it.isShown}.performClick();assertNull(label(a,"Appearance"));assertNotNull(label(a,"Pinned bookmarks" )?:label(a,"PocketSurf · Pinned bookmarks"));c.pause().stop().destroy()}
 @Test fun touchTabStripSwitchesAndCloses(){val c=start("https://example.com");val a=c.get();press(a,99);press(a,100);assertEquals(2,views(a).count{it.contentDescription?.startsWith("Tab ")==true});views(a).first{it.contentDescription?.startsWith("Tab 1:")==true}.performClick();assertEquals("https://example.com",views(a).filterIsInstance<WebView>().single().url);c.pause().stop().destroy()}
 @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE) fun tabTriggersSelectWithoutOpening(){val c=start("https://example.com");val a=c.get();press(a,99);press(a,100);press(a,99);press(a,104);assertNotNull(label(a,"Open tabs"));assertTrue(views(a).none{it is WebView});press(a,96);assertTrue(views(a).any{it is WebView});c.pause().stop().destroy()}
 @Test fun closingLastTabLeavesZeroTabsAndBackExits(){val c=start("https://example.com");val a=c.get();press(a,99);views(a).first{it.contentDescription=="Current tab: example.com"&&it.isShown}.requestFocus();press(a,103);assertNotNull(label(a,"0 tabs · choose a page to continue"));assertNull(label(a,"Close this tab?"));press(a,97);assertFalse(a.isFinishing);press(a,97);assertTrue(a.isFinishing);c.pause().stop().destroy()}
 @Test fun downloadsStayInsidePocketSurf(){val c=start();val a=c.get();press(a,4);click(a,"Downloads");assertNotNull(label(a,"Downloads"));assertNotNull(label(a,"No downloads yet"));assertNull(Shadows.shadowOf(a).nextStartedActivity);c.pause().stop().destroy()}
 @Test fun historyHasSearchAndDayGroups(){val c=start();val a=c.get();val data=BrowserData(a.getSharedPreferences("browser",Context.MODE_PRIVATE));val now=System.currentTimeMillis();data.record("https://today.test","Today page",time=now);data.record("https://yesterday.test","Yesterday page",time=now-86_400_000);press(a,4);click(a,"History");assertNotNull(label(a,"TODAY"));assertNotNull(label(a,"YESTERDAY"));val field=views(a).filterIsInstance<EditText>().first{it.hint=="Search history"};field.setText("yesterday");assertNull(label(a,"Today page"));assertNotNull(label(a,"Yesterday page"));c.pause().stop().destroy()}
 @Test fun remappingSwapsBindingsAndPersists(){val c=start();val a=c.get();press(a,4);click(a,"Settings");click(a,"Controller");click(a,"Confirm / open");click(a,"Record a button");press(a,100);click(a,"Swap buttons");val p=a.getSharedPreferences("browser",Context.MODE_PRIVATE);val b=ControllerBindings(p);assertEquals(100,b.key(Control.CONFIRM));assertEquals(96,b.key(Control.CONTEXT));assertNotNull(label(a,"Controller"));c.pause().stop().destroy()}
 @Test fun playstationLabelChoicePersists(){val c=start();val a=c.get();press(a,4);click(a,"Settings");click(a,"Controller");click(a,"SNES labels");assertTrue(ControllerBindings(a.getSharedPreferences("browser",0)).playStation);assertNotNull(label(a,"PlayStation labels"));c.pause().stop().destroy()}
 @Test fun remappedHatAndKeyEventsDoNotToggleMenuTwice(){val c=start();val a=c.get();ControllerBindings(a.getSharedPreferences("browser",0)).assign(Control.MENU,19);val e=motion(MotionEvent.AXIS_HAT_Y to -1f);a.dispatchGenericMotionEvent(e);e.recycle();press(a,19);assertNotNull(label(a,"Settings"));c.pause().stop().destroy()}
}
