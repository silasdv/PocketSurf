package org.pocketsurf.browser
import android.content.Intent
import android.graphics.*
import android.net.Uri
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
class Surf130RegressionTest{
 private val app get()=RuntimeEnvironment.getApplication()
 @Before fun reset(){app.getSharedPreferences("browser",0).edit().clear().putBoolean("setup_complete",true).commit();RecordingWebViewShadow.clearEvidence()}
 private fun field(a:BrowserActivity,n:String)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun put(a:BrowserActivity,n:String,v:Any?)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.set(a,v)
 private fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun layout(a:BrowserActivity){val d=a.resources.displayMetrics;a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(d.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(d.heightPixels,View.MeasureSpec.EXACTLY));layout(0,0,measuredWidth,measuredHeight)}}
 private fun settle(a:BrowserActivity){repeat(3){layout(a);TestFrameClock.advance(Duration.ofMillis(120))}}
 private fun start(page:Boolean=true)=Robolectric.buildActivity(BrowserActivity::class.java,if(page)Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))else Intent(Intent.ACTION_MAIN)).setup().visible().also{settle(it.get())}
 private fun key(a:BrowserActivity,k:Int,wait:Boolean=true){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));if(wait)settle(a)}
 private fun desc(a:BrowserActivity,n:String)=all(a.window.decorView).first{it.contentDescription==n&&it.isShown}
 private fun shot(a:BrowserActivity,n:String){settle(a);val v=a.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));java.io.File("build/reports/surf130/$n.png").apply{parentFile.mkdirs();outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}};b.recycle()}
 @Test fun returningFromHistoryKeepsItsSurfActionThenBackRestoresPageTarget(){val c=start();val a=c.get();put(a,"webHasSelection",true);val trigger=desc(a,"Previous tab shortcut") as ViewGroup;val label=trigger.getChildAt(0);val arrow=trigger.getChildAt(1);assertTrue(arrow.right<=label.left);key(a,100);repeat(3){key(a,21)};assertEquals("History",(field(a,"focused") as View).contentDescription);key(a,96);assertEquals("history",field(a,"screen"));key(a,97);assertEquals("page",field(a,"screen"));assertEquals(true,field(a,"surfBarActive"));assertEquals("History",(field(a,"focused") as View).contentDescription);assertNotNull((field(a,"focused") as View).foreground);shot(a,"surf-focus");key(a,97);assertEquals(false,field(a,"surfBarActive"));assertEquals(true,field(a,"webHasSelection"));assertTrue(RecordingWebViewShadow.scripts.any{it.contains("resumeSelection(true)")});c.pause().stop().destroy()}
 @Test fun zoomIsGlobalSurvivesReloadAndIsBackedUp(){val c=start();val a=c.get();call(a,"changePageZoom",125);val first=call(a,"getTab") as BrowserActivity.Tab;call(a,"openUrl","https://second.test/",true);settle(a);val second=call(a,"getTab") as BrowserActivity.Tab;assertEquals(125,second.zoomPercent);assertEquals(125,first.zoomPercent);val web=second.view!!;web.webViewClient.onPageStarted(web,"https://second.test/",null);web.webViewClient.onPageFinished(web,"https://second.test/");assertEquals(125,second.zoomPercent);assertTrue(RecordingWebViewShadow.scripts.any{it.contains("setZoom(125,")});assertEquals(125,BackupCodec.validate(BackupCodec.export(a.getSharedPreferences("browser",0))).getInt("page_zoom"));c.pause().stop().destroy();val fresh=start();assertEquals(125,(call(fresh.get(),"getTab") as BrowserActivity.Tab).zoomPercent);fresh.pause().stop().destroy()}
 @Test fun androidBackAndBCloseNewTabWithLayersFirst(){val c=start(false);val a=c.get();call(a,"cancelAddress",false);call(a,"leaveSurfBar");settle(a);val tabs=field(a,"tabs") as List<*>;assertEquals(1,tabs.size);call(a,"showPageZoom");key(a,4);assertEquals(1,tabs.size);assertNull(field(a,"popupLayer"));key(a,4);assertEquals(0,tabs.size);assertFalse(a.isFinishing);call(a,"newTab");assertEquals(1,tabs.size);key(a,97);assertEquals(0,tabs.size);key(a,97);assertTrue(a.isFinishing);c.pause().stop().destroy()}
 @Test fun closingMenuSlidesAtConstantOpacityAndMouseEntryRemovesEveryBackdrop(){val c=start();val a=c.get();key(a,109);val layer=field(a,"popupLayer") as ViewGroup;val panel=desc(a,"Menu panel");assertEquals(1f,panel.alpha,.001f);key(a,97,false);assertNull(field(a,"popupLayer"));assertNotNull(layer.parent);TestFrameClock.advance(Duration.ofMillis(90));layout(a);assertTrue(panel.translationX<0f);assertEquals(1f,panel.alpha,.001f);key(a,106);assertNull(layer.parent);assertTrue((field(a,"closingPopups") as Set<*>).isEmpty());assertEquals(true,field(a,"mouse"));c.pause().stop().destroy()}
 @Test @Config(qualifiers="w640dp-h360dp-land-mdpi") fun sixCategoryRootAndEveryCategoryFitWithoutScrolling(){val c=start();val a=c.get();val u=DashboardChecks;u.headings(a);c.pause().stop().destroy()}
 @Test @Config(qualifiers="w640dp-h360dp-land-mdpi") fun optionPagesAndControllerGroupsUseCompactLayouts(){val c=start();val a=c.get();val u=DashboardChecks;u.open(a,"controller");assertEquals(16,Control.entries.count{control->u.all(a).any{it.isShown&&it.contentDescription==control.title}});u.fits(a);for(category in SettingsDashboard.categories.keys){u.open(a,category);u.fits(a)};c.pause().stop().destroy()}
 @Test fun largeTabCardsKeepCurrentBadgeSeparateFromVisibleAddresses(){val c=start();val a=c.get();call(a,"openUrl","https://second.test/path",true);call(a,"openUrl","https://third.test/path",true);settle(a);call(a,"showTabs");settle(a);val strip=desc(a,"Horizontal tab overview") as HorizontalScrollView;val cards=all(strip).filterIsInstance<ViewGroup>().filter{it.contentDescription?.startsWith("Tab: ")==true||it.contentDescription?.startsWith("Current tab: ")==true};assertEquals(3,cards.size);assertTrue(strip.width.toFloat()/cards.first().width in 2.2f..2.7f);assertEquals(1,all(strip).count{it.contentDescription=="Current tab"});val active=cards.first{it.contentDescription!!.startsWith("Current tab:")};assertFalse(active.isSelected);assertTrue(all(active).filterIsInstance<TextView>().any{it.text.toString().contains("third.test/path")});assertTrue(all(active).filterIsInstance<TextView>().all{!it.isTextSelectable});shot(a,"open-tabs");c.pause().stop().destroy()}
 @Test @Config(qualifiers="w640dp-h360dp-land-mdpi") fun mouseRowsHaveGapsAndMetalhandUsesPixelEdges(){val c=start();val a=c.get();val u=DashboardChecks;u.open(a,"mouse");u.click(a,"Pointer style: Rock hand");val sample=u.all(a).filterIsInstance<MousePointer>().first{it.previewStyle=="rock"};sample.draw(Canvas(Bitmap.createBitmap(24,32,Bitmap.Config.ARGB_8888)));val p=MousePointer::class.java.getDeclaredField("p").apply{isAccessible=true}.get(sample) as Paint;assertFalse(p.isAntiAlias);u.fits(a);c.pause().stop().destroy()}
}
