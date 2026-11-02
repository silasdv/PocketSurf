package org.pocketsurf.browser

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import android.view.*
import android.webkit.WebView
import android.widget.*
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
class Polish101RegressionTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0)
 @Before fun setup(){prefs.edit().putBoolean(SetupActivity.COMPLETE,true).commit();RecordingWebViewShadow.clearEvidence()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun views(a:BrowserActivity)=all(a.window.decorView)
 private fun desc(a:BrowserActivity,s:String)=views(a).firstOrNull{it.contentDescription?.toString()==s&&it.isShown}?:views(a).first{it.contentDescription?.toString()==s}
 private fun text(a:BrowserActivity,s:String)=views(a).filterIsInstance<TextView>().first{it.text.toString()==s}
 private fun settle(a:BrowserActivity){repeat(3){views(a).forEach{it.forceLayout()};a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));layout(0,0,960,540)};TestFrameClock.advance(Duration.ofMillis(16))}}
 private fun start()=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible().also{settle(it.get())}
 private fun click(a:BrowserActivity,s:String){if(BrowserTestActions.settingsClick(a,s))return;var v:View=views(a).firstOrNull{it.isShown&&it.hasOnClickListeners()&&it.contentDescription==s}?:views(a).filterIsInstance<TextView>().firstOrNull{it.text.toString()==s}?:desc(a,s);while(!v.hasOnClickListeners()&&v !is CompoundButton)v=v.parent as View;v.performClick();settle(a)}
 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun bounds(v:View)=Rect().also{assertTrue(v.getGlobalVisibleRect(it))}
 private fun legend(a:BrowserActivity)=views(a).filterIsInstance<HorizontalScrollView>().first{it.contentDescription?.contains(" controls:")==true}.getChildAt(0) as ViewGroup
 private fun motion(value:Float):MotionEvent {val p=MotionEvent.PointerProperties().apply{id=0};val xy=MotionEvent.PointerCoords().apply{setAxisValue(MotionEvent.AXIS_RZ,value)};return MotionEvent.obtain(0,0,MotionEvent.ACTION_MOVE,1,arrayOf(p),arrayOf(xy),0,0,1f,1f,0,0,InputDevice.SOURCE_JOYSTICK,0)}
 @Test fun legendDistributesNaturalSizeHintsAcrossItsEntireWidth(){val c=start();val a=c.get();val row=legend(a);val children=(0 until row.childCount).map{row.getChildAt(it)};assertEquals(9,children.size);assertEquals(16,children.first().left);assertEquals(16,row.width-children.last().right);assertTrue(children.zipWithNext().all{(left,right)->right.left>left.right});assertTrue(children[6].left-children[5].right>=children[1].left-children[0].right);c.pause().stop().destroy()}
 @Test fun stickBadgesUseMatchingCapGeometry(){val c=start();val a=c.get();val badges=all(legend(a)).filterIsInstance<StickBadge>();assertEquals(listOf("L3","R3","RS"),badges.map{it.contentDescription.toString()});assertEquals(1,badges.map{it.width}.distinct().size);assertEquals(1,badges.map{it.height}.distinct().size);c.pause().stop().destroy()}
 @Test fun categoryHeadingsAndActionsShareOneGrid(){val c=start();val a=c.get();click(a,"Settings");val boxes=listOf("Browsing","Appearance & accessibility","Controls","Privacy & protection","Backup & restore","About & help").map{bounds(desc(a,it))};assertEquals(boxes[0].top,boxes[1].top);assertEquals(boxes[1].top,boxes[2].top);assertEquals(boxes[3].top,boxes[4].top);assertEquals(boxes[4].top,boxes[5].top);assertEquals(1,boxes.map{it.height()}.distinct().size);assertTrue(boxes[0].right<boxes[1].left);assertTrue(boxes[0].bottom<boxes[3].top);c.pause().stop().destroy()}
 @Test fun bookmarksBarSettingsOpenAndToggleFromEitherInitialState(){for(enabled in listOf(false,true)){prefs.edit().putBoolean("favorites_bar",enabled).commit();val c=start();val a=c.get();click(a,"Settings");click(a,"Bookmarks bar");val toggle=desc(a,"Show bookmarks bar while browsing") as Switch;assertTrue(toggle.isShown);assertEquals(enabled,toggle.isChecked);toggle.performClick();settle(a);assertEquals(!enabled,prefs.getBoolean("favorites_bar",enabled));key(a,97);key(a,97);key(a,97);assertEquals(!enabled,desc(a,"Bookmarks bar").isShown);c.pause().stop().destroy()}}
 @Test fun aboutSupportActionsUseHalfWidthCards(){val c=start();val a=c.get();click(a,"Settings");click(a,"About PocketSurf");var github:View=text(a,"GitHub");while(!github.hasOnClickListeners())github=github.parent as View;var kofi:View=text(a,"Support development");while(!kofi.hasOnClickListeners())kofi=kofi.parent as View;assertEquals(github.top,kofi.top);assertTrue(github.width<=480);assertEquals(github.width,kofi.width);c.pause().stop().destroy()}
 @Test fun bothDrawersCoverTheTopNavigationAndShareTheSameShape(){val c=start();val a=c.get();key(a,109);TestFrameClock.advance(Duration.ofMillis(360));settle(a);val browser=desc(a,"Menu panel");val b=bounds(browser);assertTrue(b.top<bounds(desc(a,"Address or Google search")).top);val width=browser.width;val radius=(browser.background as GradientDrawable).cornerRadius;key(a,97);a.dispatchKeyEvent(KeyEvent(0,96));Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600));a.dispatchKeyEvent(KeyEvent(1,96));val web=views(a).filterIsInstance<WebView>().single();Shadows.shadowOf(web).lastEvaluatedJavascriptCallback.onReceiveValue("{}");TestFrameClock.advance(Duration.ofMillis(360));settle(a);val context=desc(a,"Menu panel");val r=bounds(context);assertEquals(width,context.width);assertEquals(b.left,r.left);assertEquals(b.top,r.top);assertEquals(radius,(context.background as GradientDrawable).cornerRadius,.01f);c.pause().stop().destroy()}
 @Test fun vsyncDriverProduces120UpdatesWithoutChangingOneSecondDistance(){val c=start();val a=c.get();val callbackField=BrowserActivity::class.java.getDeclaredField("frame").apply{isAccessible=true};val callback=callbackField.get(a) as Choreographer.FrameCallback;val clock=BrowserActivity::class.java.getDeclaredField("lastFrame").apply{isAccessible=true};val scheduler=Choreographer.getInstance();val e=motion(1f);a.dispatchGenericMotionEvent(e);e.recycle();scheduler.removeFrameCallback(callback)
  fun drive(hz:Int):Pair<Int,Int>{clock.setLong(a,0L);callback.doFrame(1_000_000_000L);scheduler.removeFrameCallback(callback);RecordingWebViewShadow.nativeScroll.clear();for(i in 1..hz){callback.doFrame(1_000_000_000L+i*1_000_000_000L/hz);scheduler.removeFrameCallback(callback)};return RecordingWebViewShadow.nativeScroll.size to RecordingWebViewShadow.nativeScroll.sumOf{it.second}}
  val sixty=drive(60);val high=drive(120);assertEquals(60,sixty.first);assertEquals(120,high.first);assertEquals(1800f,sixty.second.toFloat(),2f);assertEquals(sixty.second.toFloat(),high.second.toFloat(),2f);assertTrue(a.window.attributes.preferredRefreshRate>0f);c.pause();RecordingWebViewShadow.nativeScroll.clear();callback.doFrame(3_000_000_000L);assertTrue(RecordingWebViewShadow.nativeScroll.isEmpty());c.stop().destroy()
 }
 @Test fun setupHintsHaveABlackSurfaceWithoutARoundedCard(){val c=Robolectric.buildActivity(SetupActivity::class.java).setup().visible();val a=c.get();a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));layout(0,0,960,540)};val host=all(a.window.decorView).filterIsInstance<HorizontalScrollView>().first{it.contentDescription?.startsWith("Setup controls:")==true};assertTrue(host.background is ColorDrawable);assertEquals(Color.BLACK,(host.background as ColorDrawable).color);val row=host.getChildAt(0) as ViewGroup;assertEquals(3,row.childCount);assertEquals(16,row.getChildAt(1).left);assertEquals(16,row.width-row.getChildAt(0).right);assertTrue(row.getChildAt(2).left>row.getChildAt(1).right);c.pause().stop().destroy()}
}
