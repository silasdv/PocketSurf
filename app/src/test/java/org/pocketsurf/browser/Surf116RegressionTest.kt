package org.pocketsurf.browser

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Looper
import android.view.*
import android.webkit.*
import android.widget.*
import org.json.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Surf116RegressionTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0)
 @Before fun reset(){prefs.edit().clear().putBoolean(SetupActivity.COMPLETE,true).commit();RecordingWebViewShadow.clearEvidence()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun views(a:BrowserActivity)=all(a.window.decorView)
 private fun field(a:Any,n:String):Any?=a.javaClass.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun call(a:BrowserActivity,n:String,vararg args:Any?):Any?=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun settle(a:BrowserActivity){repeat(3){views(a).forEach{it.forceLayout()};a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));layout(0,0,960,540)};TestFrameClock.advance(Duration.ofMillis(16))}}
 private fun start(count:Int=1)=Robolectric.buildActivity(BrowserActivity::class.java).also{val array=JSONArray();repeat(count){array.put(JSONObject().put("url","https://fixture$it.test/").put("title","Site $it").put("home",false))};prefs.edit().putString("sessions_v2",array.toString()).putInt("active",0).commit()}.setup().visible().also{settle(it.get())}
 private fun web(a:BrowserActivity)=call(a,"getWeb") as WebView
 private fun click(a:BrowserActivity,label:String){var v=views(a).first{it.isShown&&(it.contentDescription?.toString()==label||(it is TextView&&it.text.toString()==label))};while(!v.hasOnClickListeners())v=v.parent as View;v.performClick();settle(a)}
 private fun shot(a:BrowserActivity,name:String){TestFrameClock.advance(Duration.ofMillis(360));settle(a);val v=a.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));File("build/reports/surf113/$name.png").apply{parentFile.mkdirs();outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}};b.recycle()}

 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun desc(a:BrowserActivity,label:String)=views(a).first{it.contentDescription==label&&it.isShown}
 private fun count(a:BrowserActivity)=(field(a,"tabs") as List<*>).size
 private fun texts(v:View)=all(v).filterIsInstance<TextView>().map{it.text.toString()}

 @Test fun shouldersDoNotFocusCloseControlsAndRClosesOnlyTheSelectedOverviewCard(){val c=start(3);val a=c.get();key(a,105);val strip=field(a,"tabRow") as ViewGroup;assertTrue(all(strip).filter{it.getTag(0x7f0f0107)!=null}.all{!it.isFocusable&&!it.hasFocus()});assertEquals(false,field(a,"chromeFocus"));assertNull(field(a,"focused"));key(a,99);val card=views(a).first{it.isShown&&it.getTag(0x7f0f0101)==0};card.requestFocus();val old=field(a,"tabs") as List<*>;val remaining=old.drop(1).toList();key(a,103);assertEquals(2,count(a));assertEquals(remaining,field(a,"tabs"));assertTrue(views(a).none{it is TextView&&it.text=="Close this tab?"});click(a,"Undo");assertEquals(3,count(a));key(a,99);assertEquals("page",field(a,"screen"));c.pause().stop().destroy()}
 @Test fun allBookmarksIsAfterPinsAndReachableWithDpadAndA(){val c=start();val a=c.get();key(a,100);key(a,20);key(a,20);key(a,20);val target=a.window.decorView.findFocus();assertTrue(target?.contentDescription?.startsWith("All bookmarks (")==true);val last=desc(a,"Pin bookmark 6");val pos=IntArray(2);last.getLocationOnScreen(pos);val other=IntArray(2);target!!.getLocationOnScreen(other);assertTrue(other[1]>pos[1]);key(a,96);assertEquals("bookmarks",field(a,"screen"));c.pause().stop().destroy()}
 @Test fun inactiveControlsReserveTheirSpaceAndCornerHintsDoNotMoveIcons(){val c=start();val a=c.get();val back=desc(a,"Back");val frame=back as ViewGroup;val glyph=frame.getChildAt(0);assertEquals(frame.width/2f,glyph.left+glyph.width/2f,1f);assertEquals(frame.height/2f,glyph.top+glyph.height/2f,1f);for(name in listOf("Settings","Browser menu","Open tabs")){val button=desc(a,name) as ViewGroup;val hint=all(button).first{it.tag==0x7f0f0108};assertNull(hint.background);assertTrue(hint.left>=button.width/2-12);assertTrue(hint.bottom<=button.height)};val address=field(a,"addressHost") as View;val x=address.left;key(a,100);assertEquals(View.VISIBLE,back.visibility);assertFalse(back.isEnabled);assertTrue(back.alpha<1f);assertEquals(x,address.left);assertFalse(views(a).first{it.contentDescription=="Forward"}.isEnabled);val share=views(a).first{it.contentDescription=="Share page"};assertTrue(share.isShown);assertFalse(share.isEnabled);assertTrue(share.alpha<1f);shot(a,"new-tab-corner-hints");c.pause().stop().destroy()}
 @Test fun closeAllIsImmediateAndItsWholeBatchCanBeUndone(){val c=start(5);val a=c.get();call(a,"showTabs");click(a,"Close all tabs");assertEquals(0,count(a));assertFalse(views(a).any{it is TextView&&it.text=="Close all tabs?"});click(a,"Undo");assertEquals(5,count(a));shot(a,"tab-undo-no-prompts");c.pause().stop().destroy()}
 @Test fun blankStartPageDoesNotAdvertiseScrolling(){prefs.edit().putString("home_mode","blank").commit();val c=start();val a=c.get();key(a,100);assertFalse((field(a,"legendHost") as View).contentDescription.toString().contains("Right stick scroll"));assertFalse(views(a).first{it.contentDescription=="Back"}.isEnabled);c.pause().stop().destroy()}

}
