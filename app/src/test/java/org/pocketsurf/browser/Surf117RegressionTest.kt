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
class Surf117RegressionTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0)
 @Before fun reset(){prefs.edit().clear().putBoolean("setup_complete",true).commit();RecordingWebViewShadow.clearEvidence()}
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

 @Test fun touchTabsKeepTheirBordersAcrossModes(){val c=start(3);val a=c.get();for(name in listOf("Settings","Menu","Open tabs","Back"))assertNull(desc(a,name).foreground);val strip=field(a,"tabRow") as ViewGroup;assertTrue(all(strip).filter{it.getTag(0x7f0f0101)!=null}.all{it.background!=null&&!it.isFocusable});call(a,"setInputMode",true,true);call(a,"setInputMode",false,true);assertTrue(all(strip).filter{it.getTag(0x7f0f0101)!=null}.all{it.background!=null&&!it.isFocusable});assertTrue(all(strip).filter{it.getTag(0x7f0f0107)!=null}.all{it.background==null});shot(a,"stable-touch-only-tabs");c.pause().stop().destroy()}
 @Test fun largeTabOverviewScrollsSidewaysWithShoulderSelection(){val c=start(7);val a=c.get();BrowserTestActions.openTabs(a);val horizontal=views(a).filterIsInstance<HorizontalScrollView>().single{it.contentDescription=="Horizontal tab overview"};repeat(6){key(a,105);TestFrameClock.advance(Duration.ofMillis(360))};settle(a);assertEquals(6,field(a,"selected"));assertTrue("Selected offscreen tab must scroll into view",horizontal.scrollX>0);val cards=all(horizontal).filter{it.getTag(0x7f0f0101)!=null};assertEquals(7,cards.size);val y=cards.first().top;assertTrue(cards.all{it.top==y});assertTrue(all(horizontal).filter{it.getTag(0x7f0f0107)!=null}.all{it.background==null&&!it.isFocusable});shot(a,"tabs-horizontal-last-selected");c.pause().stop().destroy()}
 @Test fun openingTransitionsFinishAndShoulderSwitchDoesNotInheritAFade(){val c=start(2);val a=c.get();BrowserTestActions.newTab(a);TestFrameClock.advance(Duration.ofMillis(360));val content=field(a,"content") as View;assertEquals(1f,content.alpha,.001f);assertEquals(0f,content.translationY,.001f);key(a,104);assertEquals(1f,content.alpha,.001f);key(a,108);TestFrameClock.advance(Duration.ofMillis(360));val page=(content as ViewGroup).getChildAt(0);assertEquals(1f,page.alpha,.001f);assertEquals(0f,page.translationY,.001f);c.pause().stop().destroy()}
}
