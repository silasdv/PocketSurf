package org.pocketsurf.browser

import android.content.Intent
import android.graphics.Rect
import android.os.Looper
import android.view.*
import android.webkit.WebView
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
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
class Tabs102RegressionTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0)
 @Before fun fixture(){prefs.edit().clear().putBoolean(SetupActivity.COMPLETE,true).commit();RecordingWebViewShadow.clearEvidence()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun views(a:BrowserActivity)=all(a.window.decorView)
 private fun settle(a:BrowserActivity){val config=a.resources.configuration;repeat(3){views(a).forEach{it.forceLayout()};a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(config.screenWidthDp,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(config.screenHeightDp,View.MeasureSpec.EXACTLY));layout(0,0,config.screenWidthDp,config.screenHeightDp)};TestFrameClock.advance(Duration.ofMillis(16))}}
 private fun start(count:Int,active:Int=count-1):org.robolectric.android.controller.ActivityController<BrowserActivity>{val sessions=JSONArray();repeat(count){sessions.put(JSONObject().put("url","https://fixture$it.test/").put("title","Site $it").put("home",false))};prefs.edit().putString("sessions_v2",sessions.toString()).putInt("active",active).commit();return Robolectric.buildActivity(BrowserActivity::class.java).setup().visible().also{settle(it.get())}}
 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun desc(a:BrowserActivity,s:String)=views(a).first{it.contentDescription?.toString()==s&&it.isShown}
 private fun click(a:BrowserActivity,s:String){var v:View=views(a).firstOrNull{it.contentDescription?.toString()==s&&it.isShown}?:views(a).filterIsInstance<TextView>().first{it.text.toString()==s&&it.isShown};while(!v.hasOnClickListeners())v=v.parent as View;v.performClick();settle(a)}
 private fun active(a:BrowserActivity)=BrowserActivity::class.java.getDeclaredField("active").apply{isAccessible=true}.getInt(a)
 private fun snapshot(a:BrowserActivity,name:String){val view=a.window.decorView;val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height+18,android.graphics.Bitmap.Config.ARGB_8888);val canvas=android.graphics.Canvas(bitmap);view.draw(canvas);val p=android.graphics.Paint().apply{color=android.graphics.Color.BLACK};canvas.drawRect(0f,view.height.toFloat(),view.width.toFloat(),bitmap.height.toFloat(),p);p.color=android.graphics.Color.WHITE;p.textSize=10f;canvas.drawText("Robolectric native UI fixture - no Chromium page rendering",8f,view.height+12f,p);val out=java.io.File(requireNotNull(System.getProperty("pocketsurf.snapshots")),"adaptive-$name.png");out.parentFile.mkdirs();out.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
 @Test fun adaptiveStripKeepsEveryVisibleTabInsideTheViewport(){for(count in listOf(6,12,40)){val c=start(count);val a=c.get();snapshot(a,"tabs-$count");val chips=views(a).filter{it.contentDescription?.toString()?.matches(Regex("Tab \\d+:.*"))==true};assertTrue("Missing tab chips for $count",chips.isNotEmpty());assertTrue("Active tab hidden for $count",chips.any{it.contentDescription=="Tab $count: Site ${count-1}"});val row=chips.first().parent as View;val viewport=row.parent as View;assertTrue("Tab row must not overflow: $count",row.width<=viewport.width);for(chip in chips){val r=Rect();assertTrue("Tab not visible: ${chip.contentDescription}",chip.getGlobalVisibleRect(r));assertEquals(chip.width,r.width())};if(count==6)assertTrue(chips.all{it.width<155});if(count>=12)assertTrue(chips.all{(0 until (it as ViewGroup).childCount).map{index->it.getChildAt(index)}.filterIsInstance<TextView>().isEmpty()});if(count==40){val hidden=count-chips.size;val more=desc(a,"$hidden more tabs — open tab overview");assertEquals("+$hidden",all(more).filterIsInstance<TextView>().first().text.toString());more.performClick();settle(a);assertTrue(views(a).filterIsInstance<TextView>().any{it.text=="Open tabs"})};c.pause().stop().destroy()}}
 @Test fun noBookmarkPlusRemainsInTheBar(){prefs.edit().putBoolean("favorites_bar",true).commit();BrowserData(prefs).record("https://saved.test/","Saved","bookmarks");val c=start(1);val a=c.get();assertTrue(desc(a,"Bookmark: Saved").isShown);assertFalse(views(a).any{it.contentDescription=="Add bookmark"});c.pause().stop().destroy()}
 @Test fun r2AtTheLastTabPreservesTheChromeSelection(){val c=start(3);val a=c.get();key(a,19);val before=a.window.decorView.findFocus();assertNotNull(before);RecordingWebViewShadow.clearEvidence();key(a,105);assertSame(before,a.window.decorView.findFocus());assertEquals(2,active(a));assertTrue(RecordingWebViewShadow.scripts.none{it.contains(".clearSelection()")||it.contains(".mode(")});c.pause().stop().destroy()}
 @Test fun r2AtTheLastOverviewTabLeavesNewTabFocused(){val c=start(3);val a=c.get();key(a,99);val before=a.window.decorView.findFocus();assertTrue(before?.contentDescription?.startsWith("Current tab:")==true);key(a,105);assertSame(before,a.window.decorView.findFocus());assertTrue(views(a).filterIsInstance<WebView>().isEmpty());c.pause().stop().destroy()}
 @Test fun switchingTabsSuspendsAndRestoresTheirOwnSelection(){val c=start(2,0);val a=c.get();val old=views(a).filterIsInstance<WebView>().single();RecordingWebViewShadow.clearEvidence();key(a,105);assertEquals(1,active(a));assertTrue(Shadows.shadowOf(old).lastEvaluatedJavascript.contains(".suspendSelection()"));val current=views(a).filterIsInstance<WebView>().single();assertEquals("https://fixture1.test/",current.url);assertTrue(RecordingWebViewShadow.scripts.any{it.contains(".resumeSelection()")});assertFalse(RecordingWebViewShadow.scripts.any{it.contains(".clearSelection()")});assertFalse(views(a).any{it.hasOnClickListeners()&&it.isFocused});c.pause().stop().destroy()}
 @Test fun closeAllIsImmediateAndRestorableFromLabelledHistory(){val c=start(3);val a=c.get();key(a,99);click(a,"Close all tabs");assertEquals(-1,active(a));assertTrue(views(a).filterIsInstance<TextView>().any{it.text=="0 tabs · choose a page to continue"});assertTrue(prefs.getString("sessions_v2","")!!.contains("[]"));click(a,"Recently closed tabs");click(a,"Site 2");assertEquals(0,active(a));assertEquals("https://fixture2.test/",views(a).filterIsInstance<WebView>().single().url);c.pause().stop().destroy()}
 @Test @Config(qualifiers="w412dp-h800dp-port-mdpi") fun portraitOverviewKeepsBothLabelledActionsVisible(){val c=start(3);val a=c.get();key(a,99);for(s in listOf("Recently closed tabs","Close all tabs")){val v=desc(a,s);val r=Rect();assertTrue(v.getGlobalVisibleRect(r));assertEquals(v.width,r.width());assertEquals(v.height,r.height());assertTrue(all(v).filterIsInstance<TextView>().any{it.text==s})};c.pause().stop().destroy()}
 @Test fun stripPlansCoverNarrowAndWideWindowsWithoutLosingActiveTab(){for(width in listOf(160,240,412,800))for(count in listOf(1,4,12,40,200)){val p=TabStripPlan.create(width,count,count-1);assertTrue(p.indices.contains(count-1));assertEquals(count,p.indices.size+p.hidden);val used=p.indices.size*(p.width+4)+(if(p.showNewTabButton)46 else 0)+if(p.hidden>0)44 else 0;assertTrue("$width/$count uses $used",used<=width||count==1)}}
}
