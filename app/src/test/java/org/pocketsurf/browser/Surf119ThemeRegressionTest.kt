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
class Surf119ThemeRegressionTest {
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
 private fun shot(a:BrowserActivity,name:String){TestFrameClock.advance(Duration.ofMillis(360));settle(a);val v=a.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));File("build/reports/surf119/$name.png").apply{parentFile.mkdirs();outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}};b.recycle()}

 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun desc(a:BrowserActivity,label:String)=views(a).first{it.contentDescription==label&&it.isShown}
 private fun count(a:BrowserActivity)=(field(a,"tabs") as List<*>).size
 private fun texts(v:View)=all(v).filterIsInstance<TextView>().map{it.text.toString()}

 @Test fun lightModeUsesLightPlatformWidgetsAndReadableGenericControllerBadges(){prefs.edit().putString("theme","Light").commit();val c=start();val a=c.get();val value=android.util.TypedValue();a.theme.resolveAttribute(android.R.attr.colorForeground,value,true);assertTrue("Platform foreground must be dark in Light mode",android.graphics.Color.luminance(value.data)<.3);val badge=call(a,"badge",Control.HISTORY_BACK,false,android.graphics.Color.WHITE) as TextView;assertEquals(BrowserTheme.resolve("Light",false).text,badge.currentTextColor);c.pause().stop().destroy()}
 @Test fun switchingThemesRecolorsPersistentChromeAndBody(){val c=start();val a=c.get();for(mode in listOf("Light","OLED","Dark","Light")){prefs.edit().putString("theme",mode).commit();call(a,"applyTheme");settle(a);val palette=BrowserTheme.resolve(mode,false);for(name in listOf("root","tabNavigation","content")){val v=field(a,name) as View;assertEquals("$mode: $name",palette.background,(v.background as android.graphics.drawable.ColorDrawable).color)}};c.pause().stop().destroy()}

 @Test fun allNeutralBasesAndAccentsHaveReadableTextAndActualIconPixels(){
  fun luminance(c:Int)=android.graphics.Color.luminance(c)
  fun contrast(a:Int,b:Int)=(maxOf(luminance(a),luminance(b))+.05)/(minOf(luminance(a),luminance(b))+.05)
  for(mode in BrowserTheme.names)for(night in listOf(false,true))for(accent in BrowserTheme.accentNames){val t=BrowserTheme.resolve(mode,night,accent);for(bg in listOf(t.background,t.surface)){assertTrue("$mode/$accent text",contrast(t.text,bg)>=7);assertTrue("$mode/$accent secondary",contrast(t.secondary,bg)>=4.5);assertTrue("$mode/$accent accent",contrast(t.accent,bg)>=4.5)};val b=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);b.eraseColor(t.surface);SurfIcon("settings",t.text).apply{setBounds(0,0,48,48);draw(Canvas(b))};assertTrue((0 until 48).any{x->(0 until 48).any{y->b.getPixel(x,y)==t.text}});b.recycle()}
 }
 @Test fun nativeScreensRenderWithCurrentThemeRatherThanInheritedWhiteIcons(){val c=start(4);val a=c.get();for(mode in BrowserTheme.names){prefs.edit().putString("theme",mode).putString("accent","Red").commit();call(a,"applyTheme");val palette=BrowserTheme.resolve(mode,false,"Red");val accentValue=android.util.TypedValue();a.theme.resolveAttribute(android.R.attr.colorAccent,accentValue,true);assertEquals(palette.accent,accentValue.data);for(fn in listOf("showPage","showSettings","showController","showMouseSettings","showAppearance","showTabs","showEngine","showHomeSettings","showBookmarksBarSettings","showReadingSettings","showAdblock","showCookieSettings","showPrivacy","showBackup","showAbout","showHelp","showDownloads","showClosed","showWebsiteInfo","showMenu")){if(fn=="showMenu")call(a,"showPage");if(fn=="showDownloads"){val back:()->Unit={call(a,"showPage")};call(a,fn,back)}else call(a,fn);settle(a);if(fn=="showAbout")assertTrue(views(a).filterIsInstance<TextView>().any{it.text.toString().startsWith("PocketSurf "+BuildConfig.VERSION_NAME+" ·")});val images=views(a).filterIsInstance<ImageView>().filter{it.isShown&&it.drawable is SurfIcon};assertTrue("$mode/$fn icons",images.isNotEmpty());for(image in images){val paint=field(image.drawable,"p") as android.graphics.Paint;assertTrue("$mode/$fn foreground",paint.color in listOf(palette.text,palette.secondary,palette.accent)||(paint.color in listOf(android.graphics.Color.WHITE,0xffc3c9d1.toInt())&&generateSequence(image as View?){it.parent as? View}.any{it.contentDescription?.toString() in listOf("Previous tab shortcut","Next tab shortcut")}))};shot(a,"${mode.lowercase()}-${fn.removePrefix("show").lowercase()}")}};c.pause().stop().destroy()}
 @Test fun autoThemeFollowsNightChangesAndExplicitLightIgnoresSystemDark(){val c=start();val a=c.get();try{for(night in listOf(true,false)){RuntimeEnvironment.setQualifiers(if(night)"+night"else"+notnight");prefs.edit().putString("theme","Auto").commit();a.onConfigurationChanged(a.resources.configuration);settle(a);assertEquals(BrowserTheme.resolve("Auto",night).background,((field(a,"content") as View).background as android.graphics.drawable.ColorDrawable).color);val value=android.util.TypedValue();a.theme.resolveAttribute(android.R.attr.colorForeground,value,true);assertEquals(night,android.graphics.Color.luminance(value.data)>.5)};RuntimeEnvironment.setQualifiers("+night");prefs.edit().putString("theme","Light").commit();call(a,"applyTheme");val value=android.util.TypedValue();a.theme.resolveAttribute(android.R.attr.colorForeground,value,true);assertTrue(android.graphics.Color.luminance(value.data)<.3)}finally{c.pause().stop().destroy();RuntimeEnvironment.setQualifiers("+notnight")}}
 @Test fun unfinishedFormsKeepTextAndCursorWhenThemeRebuildsTheirViews(){val c=start();val a=c.get();for(fn in listOf("shortcutForm","showCustomEngine","showHomeUrl")){if(fn=="shortcutForm")call(a,fn,null)else call(a,fn);settle(a);val fields=views(a).filterIsInstance<EditText>().filter{it.isShown};fields.forEachIndexed{i,v->v.setText("draft-$i");v.setSelection(2)};prefs.edit().putString("theme","Light").commit();call(a,"applyTheme");settle(a);val restored=views(a).filterIsInstance<EditText>().filter{it.isShown};assertEquals(fields.size,restored.size);restored.forEachIndexed{i,v->assertEquals("draft-$i",v.text.toString());assertEquals(2,v.selectionStart);assertEquals(BrowserTheme.resolve("Light",false).text,v.currentTextColor)}};c.pause().stop().destroy()}
 @Test fun dismissingKeyboardInNativeHistorySearchRestoresControllerNavigation(){val c=start();val a=c.get();call(a,"showCollection","history");settle(a);val field=views(a).filterIsInstance<EditText>().single{it.isShown};field.requestFocus();BrowserActivity::class.java.getDeclaredField("focused").apply{isAccessible=true}.set(a,field);call(a,"activateNative");assertEquals(true,field(a,"nativeTyping"));call(a,"onImeVisibilityChanged",true);call(a,"onImeVisibilityChanged",false);assertEquals(false,field(a,"nativeTyping"));assertEquals(false,field(a,"imeExpected"));c.pause().stop().destroy()}
 @Test fun autoThemePreviewMatchesCurrentDaylightAppearance(){val c=start();val a=c.get();val u=DashboardChecks;u.open(a,"appearance");u.click(a,"Theme: Auto");assertEquals("Auto",a.getSharedPreferences("browser",0).getString("theme",null));val expected=BrowserTheme.resolve("Auto",false);assertEquals(expected.background,((u.field(a,"root") as View).background as android.graphics.drawable.ColorDrawable).color);assertTrue(u.node(a,"Theme: Auto").isSelected);c.pause().stop().destroy()}
}
