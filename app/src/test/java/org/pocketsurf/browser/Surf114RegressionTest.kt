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
class Surf114RegressionTest {
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

 @Test fun mouseSpeedSliderStartsSlowerAndControllerChangesPersist(){val c=start();val a=c.get();assertEquals(600,call(a,"mouseSpeed"));call(a,"showMouseSettings");settle(a);val slider=views(a).filterIsInstance<SeekBar>().single{it.contentDescription=="Mouse movement speed"};assertEquals(50,slider.progress);slider.requestFocus();key(a,22);assertEquals(660,prefs.getInt("mouse_speed",0));key(a,21);assertEquals(600,prefs.getInt("mouse_speed",0));slider.progress=150;assertEquals(1200,prefs.getInt("mouse_speed",0));assertEquals(1200,BackupCodec.validate(BackupCodec.export(prefs)).getInt("mouse_speed"));assertFalse(texts(a.window.decorView).any{it.endsWith("dp/s")});shot(a,"mouse-slider");c.pause().stop().destroy()}
 @Test fun mousePreviewCanBeToggledByL3AndTouchWithoutLosingSliderNavigation(){val c=start();val a=c.get();call(a,"showMouseSettings");settle(a);val slider=views(a).filterIsInstance<SeekBar>().single{it.contentDescription=="Mouse movement speed"};slider.requestFocus();key(a,106);assertEquals(true,field(a,"mouse"));val cursor=field(a,"mousePreviewCursor") as MousePointer;assertEquals(View.VISIBLE,cursor.visibility);val legend=field(a,"legendHost") as View;assertTrue(legend.contentDescription.toString().contains("L3 Stop test"));val before=cursor.translationX;val props=MotionEvent.PointerProperties().apply{id=0};val axes=MotionEvent.PointerCoords().apply{setAxisValue(MotionEvent.AXIS_X,1f)};val event=MotionEvent.obtain(0,0,MotionEvent.ACTION_MOVE,1,arrayOf(props),arrayOf(axes),0,0,1f,1f,0,0,InputDevice.SOURCE_JOYSTICK,0);a.dispatchGenericMotionEvent(event);event.recycle();TestFrameClock.advance(Duration.ofMillis(80));assertTrue(cursor.translationX>before);assertSame(slider,field(a,"focused"));key(a,22);assertEquals(660,prefs.getInt("mouse_speed",0));views(a).filterIsInstance<Switch>().single{it.contentDescription=="Test mouse navigation"}.performClick();assertEquals(false,field(a,"mouse"));assertEquals(View.VISIBLE,cursor.visibility);shot(a,"mouse-test");c.pause().stop().destroy()}
 @Test fun surfBarIconsRemainCenteredWithoutInlineLabels(){val c=start();val a=c.get();for(name in listOf("Settings","Menu","Open tabs","New tab")){val button=desc(a,name) as ViewGroup;val icon=button.getChildAt(0);assertEquals(button.width/2f,icon.left+icon.width/2f,1f);assertEquals(button.height/2f,icon.top+icon.height/2f,1f);assertTrue(all(button).none{it.tag==0x7f0f0108})};shot(a,"surf-bar-centered-icons");c.pause().stop().destroy()}
}
