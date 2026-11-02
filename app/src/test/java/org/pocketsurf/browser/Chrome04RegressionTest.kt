package org.pocketsurf.browser

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import android.view.*
import android.webkit.WebView
import android.widget.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.Duration

/** Native geometry/input contracts only; no Chromium playback or actual webpage scrolling. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class Chrome04RegressionTest {
 @Before fun fixtures(){RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().putBoolean("setup_complete",true).commit();RecordingWebViewShadow.clearEvidence()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun views(a:BrowserActivity)=all(a.window.decorView)
 private fun desc(a:BrowserActivity,s:String)=views(a).first{it.contentDescription?.toString()==s}
 private fun settle(a:BrowserActivity){
  // Robolectric's WebView shadow can add a layout request after its parent traversal.
  // Traverse the real hierarchy repeatedly; do not assign synthetic dimensions to the WebView.
  repeat(3){all(a.window.decorView).forEach{it.forceLayout()};a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));layout(0,0,960,540)};TestFrameClock.advance(Duration.ofMillis(16))}
  all(a.window.decorView).forEach{it.forceLayout()};a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));layout(0,0,960,540)}
 }
 private fun geometry(v:View):String {val descriptions=mutableListOf<String>();var node:View?=v;while(node!=null){val current=node;descriptions.add("${current.javaClass.simpleName}[${current.left},${current.top},${current.width}x${current.height};measured=${current.measuredWidth}x${current.measuredHeight};visible=${current.visibility};requested=${current.isLayoutRequested}]");node=current.parent as? View};return descriptions.joinToString(" -> ")}
 private fun start(url:String="https://fixture.test/path?q=value")=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse(url))).setup().visible().also{settle(it.get())}
 private fun web(a:BrowserActivity)=views(a).filterIsInstance<WebView>().single()
 private fun legend(a:BrowserActivity)=views(a).filterIsInstance<HorizontalScrollView>().first{it.contentDescription?.contains(" controls:")==true}
 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun touch(a:BrowserActivity,v:View,drag:Boolean=false){val r=Rect();assertTrue("Touch target has no visible rectangle: ${geometry(v)}",v.getGlobalVisibleRect(r));val now=SystemClock.uptimeMillis();val actions=if(drag)listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP)else listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP);actions.forEachIndexed{i,action->val event=MotionEvent.obtain(now,now+16L*i,action,r.exactCenterX(),r.exactCenterY()-if(i>0&&drag)30f else 0f,0);event.source=InputDevice.SOURCE_TOUCHSCREEN;a.dispatchTouchEvent(event);event.recycle()};settle(a)}
 private fun motion(value:Float):MotionEvent{val p=MotionEvent.PointerProperties().apply{id=0};val c=MotionEvent.PointerCoords().apply{setAxisValue(MotionEvent.AXIS_RZ,value)};return MotionEvent.obtain(0,0,MotionEvent.ACTION_MOVE,1,arrayOf(p),arrayOf(c),0,0,1f,1f,0,0,InputDevice.SOURCE_JOYSTICK,0)}
 @Test fun legendStaysWhiteAlignedAndVerticallyUnclippedAfterTouchGestureInPageViewport(){val c=start();val a=c.get();
 // Robolectric's mock WebView provider does not implement its frame delegate. Dispatch the
 // real touchscreen gesture in the measured native page viewport; this tests Activity input
 // ownership and legend geometry, not Chromium scrolling (covered separately by JS fixtures).
 touch(a,web(a).parent as View,true);val host=legend(a);assertTrue(host.isShown);assertTrue(host.contentDescription.toString().startsWith("Touch controls:"));val row=host.getChildAt(0) as ViewGroup;val hints=(0 until row.childCount).map{row.getChildAt(it)};assertEquals(9,hints.size);val firstCenter=IntArray(2).also{hints.first().getLocationOnScreen(it)}[1]+hints.first().height/2;for(hint in hints){assertEquals("Legend opacity",1f,hint.alpha,.001f);val position=IntArray(2);hint.getLocationOnScreen(position);assertEquals("RS and button hints must share a vertical center",firstCenter,position[1]+hint.height/2);for(t in all(hint).filterIsInstance<TextView>()){assertEquals("Legend label must be white",Color.WHITE,t.currentTextColor);assertTrue("Legend glyphs must fit their height",t.height>=t.paint.fontMetricsInt.let{it.descent-it.ascent});val bounds=Rect();if(t.getGlobalVisibleRect(bounds))assertEquals("Legend is vertically clipped: ${t.text}",t.height,bounds.height())}};assertTrue(all(row).filterIsInstance<StickBadge>().any{it.contentDescription=="RS"});assertTrue(all(row).filterIsInstance<TextView>().any{it.text=="Scroll"});val backing=host.parent as View;val color=(backing.background as ColorDrawable).color;assertEquals("Legend must be opaque black",Color.BLACK,color);assertEquals(24,backing.height);c.pause().stop().destroy()}
 @Test fun settingsIsRightmostInSurfBarAndStartIsInLegend(){val c=start();val a=c.get();val u=DashboardChecks;val bar=u.field(a,"surfActions") as ViewGroup;assertEquals("Settings",bar.getChildAt(bar.childCount-1).contentDescription);u.key(a,108);assertEquals("settings",u.field(a,"screen"));u.fits(a);c.pause().stop().destroy()}
 @Test fun shareButtonOpensAndroidChooserWithTheCurrentPageUrl(){val c=start();val a=c.get();val url=web(a).url;touch(a,desc(a,"Share page"));val chooser=Shadows.shadowOf(a).nextStartedActivity;assertNotNull(chooser);assertEquals(Intent.ACTION_CHOOSER,chooser.action);@Suppress("DEPRECATION") val send=chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!;assertEquals(Intent.ACTION_SEND,send.action);assertEquals("text/plain",send.type);assertEquals(url,send.getStringExtra(Intent.EXTRA_TEXT));c.pause().stop().destroy()}
 @Test fun contextButtonIsReachableByTouchAndKeepsTheWebPageAttached(){val c=start();val a=c.get();val original=web(a);touch(a,desc(a,"Menu"));assertTrue(views(a).any{it.contentDescription=="Menu popup"});assertSame(original,web(a));assertTrue(views(a).any{it.contentDescription=="Menu"});c.pause().stop().destroy()}
 @Test fun addressActivationByRealTouchSelectsTheCompleteUrl(){val c=start();val a=c.get();val field=views(a).filterIsInstance<EditText>().first{it.contentDescription?.startsWith("Address or")==true};touch(a,field);assertEquals("https://fixture.test/path?q=value",field.text.toString());assertEquals(0,field.selectionStart);assertEquals(field.length(),field.selectionEnd);assertSame(field,a.window.decorView.findFocus());c.pause().stop().destroy()}
 @Test fun oneTabShoulderTriggersNeverDetachOrReinitializeItsWebView(){val c=start();val a=c.get();val original=web(a);val parent=original.parent;var detached=0;original.addOnAttachStateChangeListener(object:View.OnAttachStateChangeListener{override fun onViewAttachedToWindow(v:View){};override fun onViewDetachedFromWindow(v:View){detached++}});RecordingWebViewShadow.clearEvidence();repeat(2){key(a,104);key(a,105)};assertSame(original,web(a));assertSame(parent,original.parent);assertEquals("Same-tab switches must not rebuild the page",0,detached);assertTrue(RecordingWebViewShadow.scripts.none{it.contains("__pocketSurf =")||it.contains("__pocketSurf=")});assertEquals(1,views(a).count{it.contentDescription?.startsWith("Tab 1:")==true});c.pause().stop().destroy()}
 @Test fun mouseArrowHasACompact10By14DpFootprint(){val c=start();val a=c.get();key(a,106);val pointer=views(a).filterIsInstance<MousePointer>().single();assertTrue(pointer.isShown);assertEquals(10,pointer.width);assertEquals(14,pointer.height);c.pause().stop().destroy()}
 @Test fun rightStickRootScrollUsesNativeDeltasAndNeutralStops(){val c=start();val a=c.get();RecordingWebViewShadow.clearEvidence();val move=motion(1f);a.dispatchGenericMotionEvent(move);move.recycle();TestFrameClock.advance(Duration.ofMillis(64));assertTrue(RecordingWebViewShadow.nativeScroll.any{it.second>0});assertTrue("Root scrolling must not enqueue a JS animation",RecordingWebViewShadow.scripts.none{it.contains(".setScroll(")&&!it.contains(".setScroll(0.0,0.0)")});val neutral=motion(0f);a.dispatchGenericMotionEvent(neutral);neutral.recycle();TestFrameClock.advance(Duration.ofMillis(32));val count=RecordingWebViewShadow.nativeScroll.size;TestFrameClock.advance(Duration.ofMillis(100));assertEquals(count,RecordingWebViewShadow.nativeScroll.size);c.pause().stop().destroy()}
}
