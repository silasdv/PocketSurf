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
class Surf121RegressionTest {
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

 private fun launchIntent()=android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
 @Test fun launcherCreatesOneReadyNewTabOnFirstStart(){val c=Robolectric.buildActivity(BrowserActivity::class.java,launchIntent()).setup().visible();val a=c.get();settle(a);assertEquals(1,count(a));assertEquals(true,field(call(a,"getTab")!!,"home"));assertFalse(field(a,"addressEditing") as Boolean);assertEquals(field(a,"addressField"),field(a,"focused"));c.pause().stop().destroy()}
 @Test fun launcherPreservesExistingSessionAndSelectsFreshTab(){val seed=start(2);seed.pause().stop().destroy();val c=Robolectric.buildActivity(BrowserActivity::class.java,launchIntent()).setup().visible();val a=c.get();settle(a);assertEquals(3,count(a));val tabs=field(a,"tabs") as List<*>;assertEquals(2,tabs.count{field(it!!,"home")==false});assertEquals(true,field(call(a,"getTab")!!,"home"));val state=android.os.Bundle();c.saveInstanceState(state).pause().stop().destroy();val restored=Robolectric.buildActivity(BrowserActivity::class.java,launchIntent()).create(state).start().resume().visible();settle(restored.get());assertEquals(3,count(restored.get()));restored.pause().stop().destroy()}
 @Test fun returningThroughLauncherCreatesNewTabButExternalLinkDoesNotAddExtraBlank(){val c=start(1);val a=c.get();c.newIntent(launchIntent());settle(a);assertEquals(2,count(a));assertEquals(true,field(call(a,"getTab")!!,"home"));val link=android.content.Intent(android.content.Intent.ACTION_VIEW,Uri.parse("https://target.test/"));c.newIntent(link);settle(a);assertEquals(3,count(a));assertEquals("https://target.test/",field(call(a,"getTab")!!,"url"));c.pause().stop().destroy()}
 @Test fun coldExternalLinkCreatesOnlyItsWebsiteTab(){val i=android.content.Intent(android.content.Intent.ACTION_VIEW,Uri.parse("https://target.test/"));val c=Robolectric.buildActivity(BrowserActivity::class.java,i).setup().visible();val a=c.get();settle(a);assertEquals(1,count(a));assertEquals(false,field(call(a,"getTab")!!,"home"));c.pause().stop().destroy()}
 @Test fun newTabKeepsDimShareAndReloadInPlaceAndWebsiteReenablesThem(){val c=start(1);val a=c.get();call(a,"newTab");settle(a);for(label in listOf("Share page","Reload page")){val v=desc(a,label);assertTrue(v.isShown);assertFalse(v.isEnabled);assertEquals(.32f,v.alpha,.001f)};call(a,"switchTab",0);settle(a);for(v in listOf(desc(a,"Share page"),field(a,"reloadButton") as View)){assertTrue(v.isShown);assertTrue(v.isEnabled);assertEquals(1f,v.alpha,.001f)};c.pause().stop().destroy()}
}
