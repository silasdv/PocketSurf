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
class Surf122RegressionTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0)
 @Before fun reset(){prefs.edit().clear().putBoolean(SetupActivity.COMPLETE,true).commit();RecordingWebViewShadow.clearEvidence()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun views(a:BrowserActivity)=all(a.window.decorView)
 private fun field(a:Any,n:String):Any?=a.javaClass.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun call(a:BrowserActivity,n:String,vararg args:Any?):Any?=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun settle(a:BrowserActivity){repeat(3){views(a).forEach{it.forceLayout()};a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));layout(0,0,960,540)};TestFrameClock.advance(Duration.ofMillis(120))}}
 private fun start(count:Int=1)=Robolectric.buildActivity(BrowserActivity::class.java).also{val array=JSONArray();repeat(count){array.put(JSONObject().put("url","https://fixture$it.test/").put("title","Site $it").put("home",false))};prefs.edit().putString("sessions_v2",array.toString()).putInt("active",0).commit()}.setup().visible().also{settle(it.get())}
 private fun web(a:BrowserActivity)=call(a,"getWeb") as WebView
 private fun click(a:BrowserActivity,label:String){var v=views(a).first{it.isShown&&(it.contentDescription?.toString()==label||(it is TextView&&it.text.toString()==label))};while(!v.hasOnClickListeners())v=v.parent as View;v.performClick();settle(a)}
 private fun shot(a:BrowserActivity,name:String){TestFrameClock.advance(Duration.ofMillis(360));settle(a);val v=a.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));File("build/reports/surf122/$name.png").apply{parentFile.mkdirs();outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}};b.recycle()}

 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun desc(a:BrowserActivity,label:String)=views(a).first{it.contentDescription==label&&it.isShown}
 private fun count(a:BrowserActivity)=(field(a,"tabs") as List<*>).size
 private fun texts(v:View)=all(v).filterIsInstance<TextView>().map{it.text.toString()}

 @Test fun sixTabsKeepTruncatedTitlesBeforeEnteringCompactMode(){val plan=TabStripPlan.create(760,6,5,82,102,34);assertTrue(plan.titles);assertEquals(6,plan.indices.size);for(comfort in listOf(false,true)){prefs.edit().putBoolean("comfort_touch",comfort).commit();val c=start(6);val a=c.get();val chips=all(field(a,"tabRow") as View).filter{it.getTag(0x7f0f0101)!=null};assertEquals(6,chips.size);for(chip in chips)assertTrue(texts(chip).any{it.startsWith("Site ")});shot(a,"six-tabs-with-titles-$comfort");c.pause().stop().destroy()}}
 @Test fun tabInsetsStayFixedWithActiveOnlyClose(){for(n in listOf(5,6,7,8,12,30)){val c=start(n);val a=c.get();val row=field(a,"tabRow") as View;val chips=all(row).filter{it.getTag(0x7f0f0101)!=null};assertEquals(1,all(row).count{it.getTag(0x7f0f0107)!=null});for(chip in chips){val line=(chip as ViewGroup).getChildAt(0) as ViewGroup;val favicon=line.getChildAt(0);assertEquals(8,favicon.left);assertEquals(16,favicon.width);val closes=all(chip).filter{it.getTag(0x7f0f0107)!=null};if(closes.isNotEmpty()){val close=closes.single();assertFalse(close.isFocusable);assertEquals(6,line.width-close.right)}};shot(a,"tabs-$n-stable-insets");c.pause().stop().destroy()}}
 @Test fun tabPlansFillAllAvailableSpaceAcrossTitleCompactAndOverflowTransitions(){for(width in listOf(412,540,640,760,960))for(n in 4..100){val plan=TabStripPlan.create(width,n,n-1,82,102,36);assertTrue(plan.indices.contains(n-1));assertEquals(n,plan.indices.size+plan.hidden);val used=plan.indices.indices.sumOf{plan.widthFor(it)+4}+(if(plan.showNewTabButton)36 else 0)+(if(plan.hidden>0)44 else 0);assertEquals("$width/$n leaves unused space",width,used);assertTrue(plan.indices.indices.all{plan.widthFor(it)>=82})}}
 @Test fun renderedTabRowsUseTheAvailableStripWithoutEmptyTrailingSpace(){for(n in listOf(4,5,6,7,8,9,12,30)){val c=start(n);val a=c.get();val row=field(a,"tabRow") as ViewGroup;val strip=field(a,"tabStrip") as View;assertEquals("$n tabs leave a gap",strip.width,row.width);assertEquals("$n final control leaves trailing space",strip.width,row.getChildAt(row.childCount-1).right+row.paddingRight+(row.getChildAt(row.childCount-1).layoutParams as ViewGroup.MarginLayoutParams).rightMargin);shot(a,"responsive-tabs-$n");c.pause().stop().destroy()}}
}
