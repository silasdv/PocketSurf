package org.pocketsurf.browser
import android.content.Intent
import android.os.Looper
import android.view.View
import android.view.ViewGroup
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
class CompactSettingsRegressionTest {
 private val app get()=RuntimeEnvironment.getApplication()
 @Before fun reset(){app.getSharedPreferences("browser",0).edit().clear().putBoolean(SetupActivity.COMPLETE,true).commit()}
 private fun field(a:BrowserActivity,n:String)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
 private fun start()=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_MAIN)).setup().visible()
 private fun settle(a:BrowserActivity){val config=a.resources.configuration;val density=a.resources.displayMetrics.density;repeat(3){views(a.window.decorView).forEach{it.forceLayout()};a.window.decorView.apply{val w=(config.screenWidthDp*density).toInt();val h=(config.screenHeightDp*density).toInt();measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));layout(0,0,w,h)};TestFrameClock.advance(Duration.ofMillis(150))}}
 private fun shot(a:BrowserActivity,name:String){val v=a.window.decorView;val b=android.graphics.Bitmap.createBitmap(v.width,v.height,android.graphics.Bitmap.Config.ARGB_8888);v.draw(android.graphics.Canvas(b));java.io.File("build/reports/compact-settings/$name-${v.width}.png").apply{parentFile.mkdirs();outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}};b.recycle()}
 @Test fun addressUsesPaperPlaneWithoutSelectHint(){val c=start();val a=c.get();call(a,"focusChrome");settle(a);val host=field(a,"addressHost") as View;assertTrue(views(host).none{it.contentDescription=="Shortcut: Select"});call(a,"showSearch",null,null,"example.com");settle(a);val send=field(a,"goButton") as View;assertTrue(send.isEnabled);send.performClick();settle(a);assertEquals(false,field(a,"addressEditing"));assertEquals("https://example.com",(call(a,"getTab") as BrowserActivity.Tab).url);assertFalse(send.isEnabled);shot(a,"address");c.pause().stop().destroy()}
 @Test fun mouseSwitchesHaveBalancedInsets(){val c=start();val a=c.get();call(a,"showMouseSettings");settle(a);val switches=views(a.window.decorView).filterIsInstance<Switch>();assertEquals(2,switches.size);switches.forEach{assertEquals(16,it.paddingLeft);assertEquals(16,it.paddingRight)};shot(a,"mouse");c.pause().stop().destroy()}
 private fun aboutFits(){val c=start();val a=c.get();call(a,"showAbout");settle(a);val scroll=field(a,"nativeScroll") as ScrollView;val body=field(a,"nativeBody") as View;assertTrue(scroll.height>150);assertTrue("About body=${body.height}, viewport=${scroll.height}",body.height<=scroll.height);val actions=listOf("Default browser","Run setup again","Copy diagnostics").map{label->views(a.window.decorView).first{it.contentDescription==label}};assertEquals(actions[0].top,actions[1].top);assertEquals(actions[1].top,actions[2].top);assertSame(actions[0].parent,actions[2].parent);shot(a,"about");c.pause().stop().destroy()}
 @Test fun aboutFitsHandheldLandscape(){aboutFits()}
 @Test @Config(qualifiers="w640dp-h360dp-land-mdpi") fun aboutFitsCompactLandscape(){aboutFits()}
}
