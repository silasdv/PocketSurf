package org.pocketsurf.browser

import android.content.Intent
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
class SurfBarRegressionTest {
 private val app get()=RuntimeEnvironment.getApplication()
 @Before fun reset(){app.getSharedPreferences("browser",0).edit().clear().putBoolean(SetupActivity.COMPLETE,true).commit();RecordingWebViewShadow.clearEvidence()}
 private fun get(a:BrowserActivity,n:String)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun put(a:BrowserActivity,n:String,v:Any?){BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.set(a,v)}
 private fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun settle(a:BrowserActivity){repeat(3){a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(a.resources.configuration.screenWidthDp,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(a.resources.configuration.screenHeightDp,View.MeasureSpec.EXACTLY));layout(0,0,measuredWidth,measuredHeight)};TestFrameClock.advance(Duration.ofMillis(120))}}
 private fun start(page:Boolean=true)=Robolectric.buildActivity(BrowserActivity::class.java,if(page)Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))else Intent(Intent.ACTION_MAIN)).setup().visible().also{settle(it.get())}
 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun tabCount(a:BrowserActivity)=(get(a,"tabs") as List<*>).size
 private fun focused(a:BrowserActivity)=get(a,"focused") as? View
 private fun shot(a:BrowserActivity,name:String){val v=a.window.decorView;val b=android.graphics.Bitmap.createBitmap(v.width,v.height,android.graphics.Bitmap.Config.ARGB_8888);v.draw(android.graphics.Canvas(b));java.io.File("build/reports/surf127/$name.png").apply{parentFile.mkdirs();outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}};b.recycle()}
 @Test fun yEntersEightOrderedActionsOnNewTabWithoutCreatingATab(){val c=start(false);val a=c.get();val before=tabCount(a);key(a,100);val actions=get(a,"surfActions") as ViewGroup;assertEquals(listOf("Reload page","History","Downloads","Page zoom","New tab","Open tabs","Share page","Menu","Settings"),(0 until actions.childCount).map{actions.getChildAt(it).contentDescription.toString()});assertEquals(before,tabCount(a));assertEquals("New tab",focused(a)?.contentDescription);assertEquals(true,get(a,"surfBarActive"));assertNotNull(focused(a)?.foreground);key(a,96);assertEquals(before+1,tabCount(a));c.pause().stop().destroy()}
 @Test fun barReturnsToOriginalPageSelectionWithoutSelectingAnotherElement(){val c=start();val a=c.get();put(a,"webHasSelection",true);RecordingWebViewShadow.clearEvidence();key(a,100);assertTrue(RecordingWebViewShadow.scripts.any{it.contains("suspendSelection")});shot(a,"page-surf-bar");key(a,97);assertEquals(false,get(a,"surfBarActive"));assertEquals(true,get(a,"webHasSelection"));assertTrue(RecordingWebViewShadow.scripts.any{it.contains("resumeSelection(true)")});assertTrue(RecordingWebViewShadow.scripts.none{it.contains(".move(")||it.contains("clearSelection()")});assertEquals(1,tabCount(a));c.pause().stop().destroy()}
 @Test fun leftReachesAddressAndStopsBeforeHistoryArrowsOrTabs(){val c=start();val a=c.get();key(a,100);repeat(5){key(a,21)};assertSame(get(a,"addressField"),focused(a));repeat(3){key(a,21)};assertSame(get(a,"addressField"),focused(a));val nodes=get(a,"focusNodes") as List<*>;val nodeViews=nodes.map{n->n!!.javaClass.getDeclaredField("view").apply{isAccessible=true}.get(n)};assertFalse(nodeViews.contains(get(a,"backButton")));assertFalse(nodeViews.contains(get(a,"forwardButton")));val tabs=get(a,"tabRow") as ViewGroup;assertTrue((0 until tabs.childCount).all{!tabs.getChildAt(it).isFocusable});key(a,22);assertSame(get(a,"reloadButton"),focused(a));c.pause().stop().destroy()}
 @Test fun bAndAndroidBackNavigateButOnlyXClosesTheTab(){val c=start();val a=c.get();key(a,97);assertEquals(1,tabCount(a));assertTrue((call(a,"getTab") as BrowserActivity.Tab).home);key(a,4);assertEquals(1,tabCount(a));assertNull(get(a,"popupLayer"));key(a,99);assertEquals(0,tabCount(a));c.pause().stop().destroy()}
 @Test fun selectOpensMenuAndStartOpensSettings(){val c=start();val a=c.get();key(a,109);assertEquals("menu",get(a,"popupKind"));key(a,97);key(a,108);assertEquals("settings",get(a,"screen"));c.pause().stop().destroy()}
 @Test fun mouseCentersOnEveryEntryButBarRoundtripKeepsItsPosition(){val c=start();val a=c.get();val content=get(a,"content") as View;key(a,106);assertEquals(content.width/2f,get(a,"pointerX") as Float,.01f);assertEquals(content.height/2f,get(a,"pointerY") as Float,.01f);put(a,"pointerX",123f);put(a,"pointerY",97f);key(a,100);key(a,97);assertEquals(123f,get(a,"pointerX") as Float,.01f);assertEquals(97f,get(a,"pointerY") as Float,.01f);key(a,106);assertTrue(RecordingWebViewShadow.scripts.any{it.contains("selectAt([123.0,97.0")});key(a,106);assertEquals(content.width/2f,get(a,"pointerX") as Float,.01f);c.pause().stop().destroy()}
 @Test fun onlyActiveTabHasTouchCloseAndLegendOrderIsStable(){val c=start();val a=c.get();repeat(7){call(a,"newTab")};call(a,"switchTab",3);settle(a);val tabs=get(a,"tabRow") as ViewGroup;assertEquals(1,all(tabs).count{it.contentDescription?.startsWith("Close ")==true});key(a,100);val host=get(a,"legendHost") as View;val words=host.contentDescription.toString();val labels=listOf("A Open","B Back","X Close","Y Surf Bar","Select Menu","Start Settings");assertTrue(labels.zipWithNext().all{(x,y)->words.indexOf(x)>=0&&words.indexOf(x)<words.indexOf(y)});shot(a,"eight-tabs-surf-bar");c.pause().stop().destroy()}
 @Test fun returningFromHistoryRestoresTheSamePageSelection(){val c=start();val a=c.get();put(a,"webHasSelection",true);key(a,100);key(a,21);key(a,21);key(a,21);assertEquals("History",focused(a)?.contentDescription);key(a,96);assertEquals("history",get(a,"screen"));key(a,97);assertEquals("page",get(a,"screen"));assertEquals(true,get(a,"webHasSelection"));c.pause().stop().destroy()}
 @Test fun tabBudgetUsesRecoveredCloseAndShortcutSpace(){for(w in listOf(240,360,540,820,960))for(n in 4..100){val p=SurfTabLayout.create(w,n,n/2);assertTrue(p.indices.contains(n/2));assertTrue(p.widths.all{it>0});assertEquals(w,p.widths.sum()+p.widths.size*4+if(p.hidden>0)40 else 0)};val p=SurfTabLayout.create(880,8,3);assertTrue(p.titles);assertEquals(8,p.indices.size);assertTrue(p.widths[3]-p.widths.last() in 28..29)}
 @Test fun pageNavigationInvalidatesTheOldSelectionReturnLease(){val c=start();val a=c.get();put(a,"webHasSelection",true);key(a,100);val web=call(a,"getWeb") as WebView;web.webViewClient.onPageStarted(web,"https://fixture.test/new",null);RecordingWebViewShadow.clearEvidence();key(a,97);assertEquals(false,get(a,"surfBarActive"));assertEquals(false,get(a,"webHasSelection"));assertTrue(RecordingWebViewShadow.scripts.none{it.contains(".move(")||it.contains("resumeSelection(true)")});c.pause().stop().destroy()}
 @Test @Config(qualifiers="w412dp-h800dp-port-mdpi") fun narrowSurfBarScrollsFocusedActionsIntoView(){val c=start(false);val a=c.get();key(a,100);repeat(4){key(a,22)};assertEquals("Settings",focused(a)?.contentDescription);val rect=android.graphics.Rect();val settings=focused(a)!!;assertTrue(settings.getGlobalVisibleRect(rect));assertEquals(settings.width,rect.width());assertTrue((get(a,"addressHost") as View).width>=80);val legend=get(a,"legend") as ViewGroup;for(hint in all(legend).filterIsInstance<TextView>()){val r=android.graphics.Rect();assertTrue(hint.getGlobalVisibleRect(r));assertEquals(hint.width,r.width());assertEquals(hint.height,r.height())};shot(a,"portrait-surf-bar");c.pause().stop().destroy()}

}
