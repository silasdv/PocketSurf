package org.pocketsurf.browser

import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.*
import android.webkit.WebView
import android.widget.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
class PageFocusRegressionTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0)
 @Before fun fixture(){prefs.edit().putBoolean("setup_complete",true).commit();RecordingWebViewShadow.clearEvidence()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));TestFrameClock.advance(Duration.ofMillis(32))}
 private fun neutralAndScroll(a:BrowserActivity){val views=all(a.window.decorView);val field=views.filterIsInstance<EditText>().first{it.contentDescription?.startsWith("Address or")==true};assertFalse("Opening a page must not focus the address",field.hasFocus());assertTrue("No native action should be focused",views.filter{it.hasOnClickListeners()}.none{it.isFocused});assertTrue(views.filterIsInstance<WebView>().isNotEmpty());RecordingWebViewShadow.clearEvidence();val p=MotionEvent.PointerProperties().apply{id=0};val xy=MotionEvent.PointerCoords().apply{setAxisValue(MotionEvent.AXIS_RZ,1f)};val e=MotionEvent.obtain(0,0,MotionEvent.ACTION_MOVE,1,arrayOf(p),arrayOf(xy),0,0,1f,1f,0,0,InputDevice.SOURCE_JOYSTICK,0);a.dispatchGenericMotionEvent(e);e.recycle();TestFrameClock.advance(Duration.ofMillis(64));assertTrue("Right stick must scroll immediately without choosing an element",RecordingWebViewShadow.nativeScroll.any{it.second>0})}
 @Test fun openingQuickAccessLeavesNoAddressSelectionAndAllowsImmediateScroll(){StartPageStore(prefs).put("Fixture","https://fixture.test/");val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible().also{TestFrameClock.advance(Duration.ofMillis(32))};val a=c.get();key(a,96);neutralAndScroll(a);c.pause().stop().destroy()}
 @Test fun openingABookmarkLeavesNoChromeSelection(){BrowserData(prefs).record("https://fixture.test/","Fixture bookmark","bookmarks");val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible().also{TestFrameClock.advance(Duration.ofMillis(32))};val a=c.get();key(a,109);var v:View=all(a.window.decorView).filterIsInstance<TextView>().first{it.text=="Bookmarks"};while(!v.hasOnClickListeners())v=v.parent as View;v.performClick();v=all(a.window.decorView).filterIsInstance<TextView>().first{it.text=="Fixture bookmark"};while(!v.hasOnClickListeners())v=v.parent as View;v.performClick();neutralAndScroll(a);c.pause().stop().destroy()}
 @Test fun externalNavigationIsNeutralButStartExplicitlyFocusesAddress(){val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible().also{TestFrameClock.advance(Duration.ofMillis(32))};val a=c.get();neutralAndScroll(a);BrowserTestActions.focusAddress(a);val field=all(a.window.decorView).filterIsInstance<EditText>().first{it.contentDescription?.startsWith("Address or")==true};assertTrue(field.hasFocus());assertEquals(0,field.selectionStart);assertEquals(field.length(),field.selectionEnd);c.pause().stop().destroy()}
}
