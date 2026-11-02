package org.pocketsurf.browser
import android.content.Intent
import android.net.Uri
import android.graphics.*
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
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-hdpi",shadows=[RecordingWebViewShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Surf128RegressionTest {
 private val app get()=RuntimeEnvironment.getApplication()
 @Before fun reset(){app.getSharedPreferences("browser",0).edit().clear().putBoolean(SetupActivity.COMPLETE,true).commit()}
 private fun field(a:BrowserActivity,n:String)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun settle(a:BrowserActivity){repeat(4){val d=a.resources.displayMetrics;a.window.decorView.apply{measure(View.MeasureSpec.makeMeasureSpec(d.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(d.heightPixels,View.MeasureSpec.EXACTLY));layout(0,0,measuredWidth,measuredHeight)};TestFrameClock.advance(Duration.ofMillis(120))}}
 private fun start(page:Boolean=true)=Robolectric.buildActivity(BrowserActivity::class.java,if(page)Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))else Intent(Intent.ACTION_MAIN)).setup().visible().also{settle(it.get())}
 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun desc(a:BrowserActivity,n:String)=all(a.window.decorView).first{it.contentDescription==n&&it.isShown}
 private fun shot(a:BrowserActivity,n:String){val v=a.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));java.io.File("build/reports/surf128/$n.png").apply{parentFile.mkdirs();outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}};b.recycle()}
 @Test fun hdpiActionsMatchAddressHeightAndActivateOnFirstPress(){for((label,destination) in listOf("History" to "history","Downloads" to "downloads","Open tabs" to "tabs","Settings" to "settings")){val c=start();val a=c.get();val address=field(a,"addressHost") as View;val bar=field(a,"surfBar") as View;assertEquals(address.height,bar.height);key(a,100);val v=desc(a,label);v.requestFocus();settle(a);assertNull(v.foreground);val image=(v as ViewGroup).getChildAt(0) as ImageView;assertNotNull(image.colorFilter);assertEquals(1.1f,image.scaleX,.01f);val bounds=Rect();assertTrue(v.getGlobalVisibleRect(bounds));assertEquals(v.height,bounds.height());key(a,96);assertEquals(destination,field(a,"screen"));c.pause().stop().destroy()}}
 @Test fun zoomSitsBeforeNewTabAndSupportsControllerAdjustment(){val c=start();val a=c.get();val actions=field(a,"surfActions") as ViewGroup;assertTrue(actions.getChildAt(0).contentDescription in listOf("Reload page","Stop loading"));assertEquals(listOf("History","Downloads","Page zoom","New tab","Open tabs","Share page","Menu","Settings"),(1 until actions.childCount).map{actions.getChildAt(it).contentDescription.toString()});key(a,100);assertEquals("New tab",(field(a,"focused") as View).contentDescription);key(a,21);key(a,96);assertEquals("zoom",field(a,"popupKind"));key(a,22);val tab=call(a,"getTab") as BrowserActivity.Tab;assertEquals(105,tab.zoomPercent);key(a,21);assertEquals(100,tab.zoomPercent);c.pause().stop().destroy()}
 @Test fun bookmarkTouchBackLeavesEvenWhenSearchStillHasFocus(){val c=start(false);val a=c.get();all(a.window.decorView).first{it.contentDescription?.startsWith("All bookmarks (")==true}.performClick();settle(a);val query=all(a.window.decorView).filterIsInstance<EditText>().first{it.hint=="Search bookmarks"};query.requestFocus();BrowserActivity::class.java.getDeclaredField("nativeTyping").apply{isAccessible=true}.set(a,true);desc(a,"Back").performClick();settle(a);assertEquals("page",field(a,"screen"));c.pause().stop().destroy()}
 @Test fun pointerSizeAndStylesPersistAndAreIncludedInBackups(){val c=start();val a=c.get();call(a,"showMouseSettings");settle(a);val slider=desc(a,"Pointer size") as SeekBar;slider.progress=170;settle(a);val prefs=a.getSharedPreferences("browser",0);assertEquals(220,prefs.getInt("pointer_size",0));call(a,"showPointerSettings");settle(a);desc(a,"Pointer style: Pixel").performClick();settle(a);desc(a,"Fill: Black").performClick();desc(a,"Outline: None").performClick();assertEquals("pixel",prefs.getString("pointer_style",null));val clean=BackupCodec.validate(BackupCodec.export(prefs));assertEquals(220,clean.getInt("pointer_size"));assertEquals("pixel",clean.getString("pointer_style"));assertEquals("none",clean.getString("pointer_outline"));val pointer=field(a,"pointer") as MousePointer;assertEquals(33,pointer.layoutParams.width);shot(a,"pointer-settings");c.pause().stop().destroy()}
 @Test fun permanentDesktopPolicyAppliesBeforeNavigation(){val c=start();val a=c.get();val web=call(a,"getWeb") as WebView;assertTrue(web.settings.userAgentString.contains("X11; Linux x86_64"));assertFalse(web.settings.userAgentString.contains("Mobile"));assertTrue(web.settings.useWideViewPort);assertTrue(web.settings.loadWithOverviewMode);assertTrue(DesktopBrowsing.userAgent("Mozilla Android Chrome/145.0.7 Mobile").contains("Chrome/145.0.7"));c.pause().stop().destroy()}
 @Test @Config(qualifiers="w640dp-h360dp-land-mdpi") fun settingsPagesNeverRequireVerticalScrolling(){val c=start();val a=c.get();for(fn in listOf("showSettings","showController","showMouseSettings","showAppearance","showAbout","showEngine","showReadingSettings","showPrivacy","showBackup")){call(a,fn);settle(a);val scroll=field(a,"nativeScroll") as ScrollView;assertFalse("$fn overflow ${scroll.getChildAt(0).height}/${scroll.height}",scroll.canScrollVertically(1));shot(a,fn)};c.pause().stop().destroy()}
 @Test @Config(qualifiers="w412dp-h800dp-port-mdpi") fun pointerChoicesAndSizeFitPortrait(){val c=start();val a=c.get();call(a,"showPointerSettings");settle(a);assertEquals("pointer",field(a,"popupKind"));for(e in MousePointer.styles.values){val v=desc(a,"Pointer style: $e");val r=Rect();assertTrue(v.getGlobalVisibleRect(r));assertEquals(v.height,r.height())};shot(a,"pointer-portrait");c.pause().stop().destroy()}
}
