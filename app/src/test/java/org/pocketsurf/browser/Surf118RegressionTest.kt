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
class Surf118RegressionTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0)
 @Before fun reset(){prefs.edit().clear().putBoolean(SetupActivity.COMPLETE,true).commit();RecordingWebViewShadow.clearEvidence()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun views(a:BrowserActivity)=all(a.window.decorView)
 private fun field(a:Any,n:String):Any?=a.javaClass.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun call(a:BrowserActivity,n:String,vararg args:Any?):Any?=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun settle(a:BrowserActivity){repeat(3){views(a).forEach{it.forceLayout()};a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));layout(0,0,960,540)};TestFrameClock.advance(Duration.ofMillis(16))}}
 private fun start(count:Int=1)=Robolectric.buildActivity(BrowserActivity::class.java).also{val array=JSONArray();repeat(count){array.put(JSONObject().put("url","https://fixture$it.test/").put("title","Site $it").put("home",false))};prefs.edit().putString("sessions_v2",array.toString()).putInt("active",0).commit()}.setup().visible().also{settle(it.get())}
 private fun web(a:BrowserActivity)=call(a,"getWeb") as WebView
 private fun click(a:BrowserActivity,label:String){if(BrowserTestActions.settingsClick(a,label))return;var v=views(a).first{it.isShown&&(it.contentDescription?.toString()==label||(it is TextView&&it.text.toString()==label))};while(!v.hasOnClickListeners())v=v.parent as View;v.performClick();settle(a)}
 private fun shot(a:BrowserActivity,name:String){TestFrameClock.advance(Duration.ofMillis(360));settle(a);val v=a.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));File("build/reports/surf113/$name.png").apply{parentFile.mkdirs();outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}};b.recycle()}

 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun desc(a:BrowserActivity,label:String)=views(a).first{it.contentDescription==label&&it.isShown}
 private fun count(a:BrowserActivity)=(field(a,"tabs") as List<*>).size
 private fun texts(v:View)=all(v).filterIsInstance<TextView>().map{it.text.toString()}

 @Test fun addressHasNoSelectHintAndHistoryKeepsDimIcons(){val c=start();val a=c.get();assertTrue(all(field(a,"addressHost") as View).none{it.tag==0x7f0f0108});BrowserTestActions.newTab(a);for(name in listOf("Back","Forward")){val button=views(a).first{it.contentDescription==name};assertTrue(button.isShown);assertFalse(button.isEnabled);assertTrue(button.alpha<.5f);assertTrue(all(button).filter{it.tag==0x7f0f0108}.none{it.isShown})};shot(a,"dim-history-no-hints");c.pause().stop().destroy()}
 @Test fun compactTabsOnlyShowActiveTouchClose(){val c=start(30);val a=c.get();val row=field(a,"tabRow") as ViewGroup;val chips=all(row).filter{it.getTag(0x7f0f0101)!=null};assertTrue(chips.isNotEmpty());assertEquals(1,all(row).count{it.getTag(0x7f0f0107)!=null});val close=all(row).single{it.getTag(0x7f0f0107)!=null};assertTrue(close.isShown);assertFalse(close.isFocusable);assertNull(close.background);assertTrue((field(a,"jumpHints") as List<*>).isEmpty());shot(a,"compact-active-close-only");c.pause().stop().destroy()}
 @Test fun overviewStartsOnCurrentTabAndRevealsItRatherThanNewTab(){val c=start(9);val a=c.get();call(a,"switchTab",7);BrowserTestActions.openTabs(a);TestFrameClock.advance(Duration.ofMillis(360));settle(a);val focus=a.window.decorView.findFocus()!!;assertEquals(7,focus.getTag(0x7f0f0101));assertFalse(focus.contentDescription=="New tab");val horizontal=views(a).filterIsInstance<HorizontalScrollView>().single{it.contentDescription=="Horizontal tab overview"};assertTrue(horizontal.scrollX>0);shot(a,"overview-current-tab-selected");c.pause().stop().destroy()}
 @Test fun bUsesHistoryAndXClosesAfterShoulderSelection(){val c=start(3);val a=c.get();key(a,105);val current=(field(a,"tabs") as List<*>)[1];assertTrue((field(a,"legendHost") as View).contentDescription.toString().contains("B Back"));key(a,97);assertEquals(3,count(a));assertTrue((field(a,"tabs") as List<*>).contains(current));key(a,99);assertEquals(2,count(a));assertFalse((field(a,"tabs") as List<*>).contains(current));c.pause().stop().destroy()}
}
