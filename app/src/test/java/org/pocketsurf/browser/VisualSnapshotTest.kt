package org.pocketsurf.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.*
import android.widget.TextView
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Actual native Android view rendering. This is not a Chromium/emulator screenshot. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisualSnapshotTest {
 @org.junit.Before fun skipFirstRunForBrowserFixture(){org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().putBoolean("setup_complete",true).commit()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k))}
 private fun click(a:BrowserActivity,s:String){if(s in listOf("Settings","History")){if(all(a.window.decorView).any{it.contentDescription=="Browser menu popup"})a.dispatchKeyEvent(KeyEvent(0,97));all(a.window.decorView).first{it.contentDescription==s}.performClick();return};var v:View=all(a.window.decorView).filterIsInstance<TextView>().first{it.text.toString()==s};while(!v.hasOnClickListeners())v=v.parent as View;v.performClick()}
 private fun shot(a:BrowserActivity,name:String,w:Int=960,h:Int=540){TestFrameClock.advance(java.time.Duration.ofMillis(360));val view=a.window.decorView;view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);val bitmap=Bitmap.createBitmap(w,h+18,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap));val canvas=Canvas(bitmap);val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.BLACK};canvas.drawRect(0f,h.toFloat(),w.toFloat(),(h+18).toFloat(),paint);paint.color=android.graphics.Color.WHITE;paint.textSize=10f;canvas.drawText("Robolectric native UI fixture - no Chromium page rendering",8f,(h+12).toFloat(),paint);val dir=File(requireNotNull(System.getProperty("pocketsurf.snapshots")));dir.mkdirs();File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}}
 @Test fun nativeScreens(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();val a=c.get();shot(a,"01-home");key(a,4);shot(a,"02-menu");click(a,"Settings");shot(a,"03-settings");click(a,"Bookmarks bar");shot(a,"18-bookmarks-bar-settings");key(a,97);click(a,"About PocketSurf");shot(a,"20-about");key(a,97);click(a,"Appearance");shot(a,"12-appearance");key(a,97);click(a,"Controller");shot(a,"04-controller");key(a,97);key(a,97)
  val d=BrowserData(a.getSharedPreferences("browser",Context.MODE_PRIVATE));d.record("https://developer.android.com","Android Developers");d.record("https://www.wikipedia.org","Wikipedia",time=System.currentTimeMillis()-86_400_000)
  key(a,4);click(a,"History");shot(a,"05-history");key(a,97);key(a,4);click(a,"Downloads");shot(a,"06-downloads");key(a,97);key(a,99);key(a,100);key(a,99);shot(a,"07-tabs");c.pause().stop().destroy()
 }
 @Test fun pagePopupFixtures(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://fixture.test"))).setup().visible();val a=c.get();shot(a,"13-page-fixture");key(a,4);shot(a,"14-menu-over-page-fixture");key(a,97);a.dispatchKeyEvent(KeyEvent(0,96));Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(600));a.dispatchKeyEvent(KeyEvent(1,96));val web=all(a.window.decorView).filterIsInstance<android.webkit.WebView>().single();Shadows.shadowOf(web).lastEvaluatedJavascriptCallback.onReceiveValue(org.json.JSONObject.quote("""{"link":"https://destination.test","title":"Fixture link","rect":{"left":40,"top":40,"width":150,"height":32},"viewportWidth":960}"""));shot(a,"15-context-over-page-fixture");c.pause().stop().destroy()
 }
 @Test fun downloadProgressAndPlaystationLabels(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();val a=c.get()
  val downloads=SurfDownloads(a);val item=downloads.start("https://example.com/handheld-guide.pdf","test",mime="application/pdf")
  val manager=a.getSystemService(android.app.DownloadManager::class.java);val request=org.robolectric.shadow.api.Shadow.extract<org.robolectric.shadows.ShadowDownloadManager.ShadowRequest>(org.robolectric.Shadows.shadowOf(manager).getRequest(item.id));request.status=android.app.DownloadManager.STATUS_RUNNING;request.totalSize=2097152;request.bytesSoFar=1048576
  key(a,4);click(a,"Downloads");shot(a,"08-download-progress");key(a,97);click(a,"Settings");click(a,"Controller");click(a,"SNES labels");shot(a,"09-playstation-labels");c.pause().stop().destroy()
 }
 @Test @Config(sdk=[33],qualifiers="w412dp-h800dp-port-mdpi") fun portraitTouchLayout(){val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();shot(c.get(),"10-portrait",412,800);c.pause().stop().destroy()}
 @Test fun lightTheme(){RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().putString("theme","Light").apply();val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();shot(c.get(),"11-light");c.pause().stop().destroy()}

}
