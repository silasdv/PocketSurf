package org.pocketsurf.browser

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.webkit.WebView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(sdk=[33],shadows=[RecordingWebViewShadow::class])
class LinearUiScalingTest {
 @Before fun reset(){RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().clear().putBoolean("setup_complete",true).commit()}
 private fun field(a:BrowserActivity,n:String)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun geometry(view:View):String {
  val parts=mutableListOf<String>();var v:View?=view
  while(v!=null){parts.add("${v.javaClass.simpleName}:${v.contentDescription} top=${v.top} bottom=${v.bottom} height=${v.height} pad=${v.paddingTop}/${v.paddingBottom} scroll=${v.scrollY} translation=${v.translationY}");v=v.parent as? View}
  return parts.joinToString(" > ")
 }
 private fun settle(a:BrowserActivity){
  val v=a.window.decorView;val dm=a.resources.displayMetrics
  repeat(5){v.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dm.heightPixels,View.MeasureSpec.EXACTLY));v.layout(0,0,v.measuredWidth,v.measuredHeight);TestFrameClock.advance(Duration.ofMillis(80))}
 }
 private fun check(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/page"))).setup().visible()
  val a=c.get();settle(a);val ui=call(a,"getBrowserUi") as BrowserUiScale;val v=ui.viewport;val density=ui.resources.displayMetrics.density
  println("UI_MATRIX window=${v.width}x${v.height} baseDensity=${v.density} fontScale=${v.fontScale} factor=${v.factor} uiDensity=$density")
  assertEquals(BrowserUiScale.factor(v.width,v.height,a.resources.displayMetrics.density),v.factor,.0001f)
  assertEquals(v.uiDensity,density,.01f)
  val input=field(a,"addressField") as EditText
  assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,13f,ui.resources.displayMetrics),input.textSize,.2f)
  val originalSp=android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,13f,a.resources.displayMetrics)
  assertEquals("SP/font scale must be applied once",originalSp*v.factor,input.textSize,.2f)
  val toolbar=field(a,"toolbar") as View
  assertEquals((40f*maxOf(1f,v.fontScale)).roundToInt()*density,toolbar.height.toFloat(),2f)
  val back=field(a,"backButton") as ViewGroup;val icon=back.getChildAt(0)
  assertEquals((18*density).roundToInt(),icon.width);assertEquals(icon.width,icon.height)
  val web=call(a,"getWeb") as WebView
  assertEquals(a.resources.displayMetrics.density,web.resources.displayMetrics.density,0f)
  val webHost=web.parent as View;assertTrue(webHost.height>0);assertTrue(webHost.width>=a.window.decorView.width*.9f)
  // ShadowWebView deliberately has no Chromium measurement; verify the real host and fill contract.
  assertEquals(ViewGroup.LayoutParams.MATCH_PARENT,web.layoutParams.width);assertEquals(ViewGroup.LayoutParams.MATCH_PARENT,web.layoutParams.height)
  val tabs=field(a,"tabRow") as ViewGroup;val tabViews=(0 until tabs.childCount).map{tabs.getChildAt(it)}
  call(a,"showSearch",call(a,"getTab"),null,"");settle(a)
  val pages=(1..20).map{Visit("https://site$it.test/path?id=$it","Site $it",it.toLong())}
  BrowserActivity::class.java.getDeclaredField("searchPages").apply{isAccessible=true}.set(a,pages)
  BrowserActivity::class.java.getDeclaredField("searchCompletions").apply{isAccessible=true}.set(a,(1..30).map{"Suggestion $it"})
  call(a,"renderSearchResults");settle(a)
  val row=field(a,"websiteRows") as ViewGroup
  assertTrue(row.childCount>0)
  for(i in 0 until row.childCount){val bubble=row.getChildAt(i) as ViewGroup;val circle=bubble.getChildAt(0);assertEquals(circle.width,circle.height);assertTrue(bubble.right<=row.width)}
  val factor=ui.viewport.factor
  val root=field(a,"root") as View
  for(occlusion in listOf(.5f,.6f,.7f)){
   root.setPadding(0,0,0,(a.window.decorView.height*occlusion).roundToInt());settle(a);call(a,"renderSearchResults");settle(a)
   assertEquals(factor,ui.viewport.factor,0f)
   val suggestions=field(a,"googleRows") as ViewGroup
   for(i in 0 until suggestions.childCount)assertTrue(suggestions.getChildAt(i).bottom<=suggestions.height)
   val sites=field(a,"websiteRows") as ViewGroup
   for(i in 0 until sites.childCount)assertTrue(sites.getChildAt(i).bottom<=sites.height)
  }
  root.setPadding(0,0,0,0);call(a,"cancelAddress",false);settle(a)
  assertSame(web,call(a,"getWeb"));assertEquals(tabViews,(0 until tabs.childCount).map{tabs.getChildAt(it)})
  call(a,"showSettingsPanel","browsing");settle(a)
  val settings=field(a,"settingsDashboard") as SettingsDashboard
  for(category in SettingsDashboard.categories.keys){
   settings.select(category,true);settle(a)
   for(control in all(settings).filter{it.isShown&&it.isClickable&&it.contentDescription!=null}){
    val rect=Rect();assertTrue("$category: ${control.contentDescription} invisible",control.getGlobalVisibleRect(rect))
    assertEquals("$category: ${control.contentDescription} clipped vertically: ${geometry(control)}",control.height,rect.height())
   }
  }
  for(destination in listOf("bookmarks","history")){call(a,"showCollection",destination);settle(a);assertTrue((field(a,"content") as View).height>0)}
  call(a,"showDownloads",{});settle(a)
  call(a,"showTabs");settle(a)
  assertSame(web,(call(a,"getTab") as BrowserActivity.Tab).view)
  c.pause().stop().destroy()
 }
 @Test @Config(qualifiers="w1280dp-h720dp-land-mdpi") fun viewport1280x720()=check()
 @Test @Config(qualifiers="w1920dp-h1080dp-land-mdpi") fun viewport1920x1080()=check()
 @Test @Config(qualifiers="w2560dp-h1440dp-land-mdpi") fun viewport2560x1440()=check()
 @Test @Config(qualifiers="w3840dp-h2160dp-land-mdpi") fun viewport3840x2160()=check()
 @Test @Config(qualifiers="w1024dp-h768dp-land-mdpi") fun viewport1024x768()=check()
 @Test @Config(qualifiers="w1600dp-h1200dp-land-mdpi") fun viewport1600x1200()=check()
 @Test @Config(qualifiers="w2048dp-h1536dp-land-mdpi") fun viewport2048x1536()=check()
 @Test @Config(qualifiers="w640dp-h360dp-land-xhdpi") fun viewport1280x720AtDensityTwo()=check()
 @Test @Config(qualifiers="w960dp-h540dp-land-xhdpi") fun viewport1920x1080AtDensityTwo()=check()
 @Test @Config(qualifiers="w640dp-h360dp-land-xxhdpi") fun viewport1920x1080AtDensityThree()=check()
 @Test @Config(qualifiers="w960dp-h540dp-land-television-xhdpi") fun tvLogical960x540()=check()
 @Test @Config(qualifiers="w960dp-h540dp-land-xhdpi") fun fontScaleOnePointThree(){
  RuntimeEnvironment.setFontScale(1.3f);try{check()}finally{RuntimeEnvironment.setFontScale(1f)}
 }
 @Test @Config(sdk=[35],qualifiers="w1024dp-h768dp-land-mdpi") fun android15WithLargeFonts(){
  RuntimeEnvironment.setFontScale(1.5f);try{check()}finally{RuntimeEnvironment.setFontScale(1f)}
 }
 @Test @Config(qualifiers="w832dp-h468dp-land-mdpi") fun liveWindowResizeKeepsWebViewAndSearchDraft(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/page"))).setup().visible()
  val a=c.get();settle(a);val web=call(a,"getWeb") as WebView
  call(a,"showSearch",call(a,"getTab"),null,"draft retained");settle(a)
  val input=field(a,"addressField") as EditText;input.setSelection(2,6)
  BrowserActivity::class.java.getDeclaredField("addressKeyboardDismissed").apply{isAccessible=true}.set(a,true)
  val display=Shadows.shadowOf(a.windowManager.defaultDisplay);display.setWidth(1600);display.setHeight(1200)
  RuntimeEnvironment.setQualifiers("w1600dp-h1200dp-land-mdpi")
  a.onConfigurationChanged(a.resources.configuration);settle(a)
  val ui=call(a,"getBrowserUi") as BrowserUiScale
  assertTrue(ui.viewport.factor>1f);assertSame(web,call(a,"getWeb"))
  val after=field(a,"addressField") as EditText;assertEquals("draft retained",after.text.toString());assertEquals(2,after.selectionStart);assertEquals(6,after.selectionEnd)
  assertEquals(true,field(a,"addressKeyboardDismissed"))
  call(a,"cancelAddress",false);settle(a)
  assertTrue((field(a,"backButton") as View).width>28)
  c.pause().stop().destroy()
 }
 @Test @Config(qualifiers="w832dp-h468dp-land-mdpi") fun keyboardConfigurationDoesNotRebuildSearchInput(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible()
  val a=c.get();settle(a);call(a,"showSearch",call(a,"getTab"),null,"query retained");settle(a)
  val input=field(a,"addressField") as EditText
  val config=android.content.res.Configuration(a.resources.configuration).apply{keyboardHidden=android.content.res.Configuration.KEYBOARDHIDDEN_YES}
  a.onConfigurationChanged(config);settle(a)
  assertSame(input,field(a,"addressField"));assertEquals("query retained",input.text.toString());assertEquals(true,field(a,"addressEditing"))
  c.pause().stop().destroy()
 }
 @Test @Config(qualifiers="w960dp-h540dp-land-television-xhdpi") fun tvRemoteCenterActivatesNativeControls(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible()
  val a=c.get();settle(a);call(a,"showSettingsPanel","browsing");settle(a)
  val clear=all(a.window.decorView).first{it.isShown&&it.contentDescription=="Clear bookmarks"};clear.requestFocus()
  a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER));a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER));settle(a)
  val cancel=all(a.window.decorView).first{it.isShown&&it.contentDescription=="Cancel setting action"}
  cancel.performClick();settle(a)
  assertTrue(all(a.window.decorView).none{it.isShown&&it.contentDescription=="Confirm setting action"})
  c.pause().stop().destroy()
 }
 @Test fun proportionalMathHasNoUpperOrLowerClamp(){
  assertEquals(.5f,BrowserUiScale.factor(416,234,1f),0f)
  assertEquals(8f,BrowserUiScale.factor(6656,3744,1f),0f)
  assertEquals(BrowserUiScale.factor(1920,1080,1f)*2,BrowserUiScale.factor(3840,2160,1f),.0001f)
  val p=RuntimeEnvironment.getApplication().getSharedPreferences("mapping",0);p.edit().clear().commit()
  val bindings=ControllerBindings(p);assertEquals(Control.CONFIRM,bindings.action(KeyEvent.KEYCODE_DPAD_CENTER))
  bindings.assign(Control.MENU,KeyEvent.KEYCODE_DPAD_CENTER);assertEquals(Control.MENU,bindings.action(KeyEvent.KEYCODE_DPAD_CENTER))
 }
}
