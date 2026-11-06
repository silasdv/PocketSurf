package org.pocketsurf.browser

import android.content.Intent
import android.net.Uri
import android.widget.SeekBar
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],shadows=[RecordingWebViewShadow::class])
class ZoomStickRegressionTest {
 @Before fun reset(){RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().clear().putBoolean("setup_complete",true).commit()}
 private fun call(a:BrowserActivity,name:String,vararg args:Any)=BrowserActivity::class.java.declaredMethods.first{it.name==name&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun field(a:BrowserActivity,name:String)=BrowserActivity::class.java.getDeclaredField(name).apply{isAccessible=true}.get(a)
 @Test fun rightStickChangesVisibleSliderWithDeadzoneRepeatAndImmediateReversal(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible();val a=c.get()
  call(a,"showPageZoom");val slider=field(a,"zoomSlider") as SeekBar;val input=field(a,"controller") as ControllerInput
  fun stick(x:Float,now:Long){input.update(ControllerInput.Axes(rightX=x),now,false,{},{ });call(a,"adjustZoomWithRightStick",now)}
  stick(.2f,1000);assertEquals(50,slider.progress)
  stick(1f,1010);assertEquals(55,slider.progress)
  stick(1f,1100);assertEquals(55,slider.progress)
  stick(1f,1160);assertEquals(60,slider.progress)
  stick(-1f,1170);assertEquals(55,slider.progress)
  stick(0f,1180);stick(1f,1190);assertEquals(60,slider.progress)
  assertEquals(110,a.getSharedPreferences("browser",0).getInt("page_zoom",100))
  a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_RIGHT));assertEquals(65,slider.progress)
  call(a,"navigate","left","axis");assertEquals(60,slider.progress)
  slider.progress=200;stick(1f,1400);assertEquals(200,slider.progress)
  call(a,"closePopup",false);stick(-1f,1600);assertEquals(250,a.getSharedPreferences("browser",0).getInt("page_zoom",100));assertNull(field(a,"zoomSlider"))
  c.pause().stop().destroy()
 }
 @Test fun rightStickCannotChangeZoomInOtherPopups(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible();val a=c.get()
  call(a,"showMenu");val input=field(a,"controller") as ControllerInput
  input.update(ControllerInput.Axes(rightX=1f),1000,false,{},{ });call(a,"adjustZoomWithRightStick",1000L)
  assertEquals(100,a.getSharedPreferences("browser",0).getInt("page_zoom",100));c.pause().stop().destroy()
 }
 @Test fun bookmarksHaveEqualChromeGapsAndNoClippedTouchTargets(){
  for(comfort in listOf(false,true)){
   RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().putBoolean("favorites_bar",true).putBoolean("comfort_touch",comfort).commit()
   val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible();val a=c.get()
   val root=a.window.decorView;root.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));root.layout(0,0,960,540)
   val toolbar=field(a,"toolbar") as View;val bar=field(a,"favoritesBar") as View;val row=field(a,"favoritesRow") as ViewGroup;val stage=field(a,"stage") as View
   assertEquals(bar.top-toolbar.bottom,stage.top-bar.bottom)
   assertTrue(row.childCount>0);assertTrue(row.getChildAt(0).height<=bar.height)
   c.pause().stop().destroy()
  }
 }
}
