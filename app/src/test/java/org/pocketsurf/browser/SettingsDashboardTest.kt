package org.pocketsurf.browser

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.view.*
import android.widget.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w640dp-h360dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsDashboardTest {
 @Test fun newTabNavigationMovesFromAddressThroughBookmarksToPinsAndBack(){val c=start();val a=c.get();val prefs=app.getSharedPreferences("browser",0);StartPageStore(prefs).put("Google","https://www.google.com/");call(a,"newTab");settle(a);call(a,"focusChrome");key(a,KeyEvent.KEYCODE_DPAD_DOWN);assertEquals("Bookmark: Google",a.window.decorView.findFocus()?.contentDescription);key(a,KeyEvent.KEYCODE_DPAD_DOWN);assertEquals("Pinned bookmark: Google",a.window.decorView.findFocus()?.contentDescription);key(a,KeyEvent.KEYCODE_DPAD_UP);assertEquals("Bookmark: Google",a.window.decorView.findFocus()?.contentDescription);c.pause().stop().destroy()}
 @Test fun controllerGlyphsFollowThemeAndDpadNeverUsesAccent(){val c=start();val a=c.get();val prefs=app.getSharedPreferences("browser",0);for(mode in listOf("Light","OLED")){prefs.edit().putString("theme",mode).commit();call(a,"applyTheme");settle(a);dashboard(a).select("controller",true);settle(a);val expected=if(mode=="Light")Color.BLACK else Color.WHITE;val wrappers=all(a).filterIsInstance<BadgePreview>();assertEquals(16,wrappers.size);for(wrapper in wrappers){val glyph=wrapper.getChildAt(0);if(glyph is TextView)assertEquals(expected,glyph.currentTextColor);if(glyph is StickBadge){val bitmap=Bitmap.createBitmap(88,72,Bitmap.Config.ARGB_8888);glyph.layout(0,0,88,72);glyph.draw(Canvas(bitmap));val pixels=IntArray(88*72);bitmap.getPixels(pixels,0,88,0,0,88,72);assertTrue(pixels.contains(expected))};if(glyph is ImageView){(wrapper.parent as View).requestFocus();assertNull(glyph.colorFilter);assertNull(glyph.imageTintList)}};shot(a,"controller-neutral-$mode")};c.pause().stop().destroy()}
 @Test fun choiceControlsRequireActivationAndReleaseOnBack(){val c=start();val a=c.get();dashboard(a).select("browsing",true);settle(a);val prefs=app.getSharedPreferences("browser",0);val before=StartPageStore(prefs).homeMode;key(a,KeyEvent.KEYCODE_DPAD_RIGHT);assertEquals(before,StartPageStore(prefs).homeMode);desc(a,"Start page").requestFocus();key(a,KeyEvent.KEYCODE_BUTTON_A);assertEquals(before,StartPageStore(prefs).homeMode);key(a,KeyEvent.KEYCODE_DPAD_RIGHT);assertNotEquals(before,StartPageStore(prefs).homeMode);key(a,KeyEvent.KEYCODE_BUTTON_B);assertFalse(dashboard(a).sliderEditing());c.pause().stop().destroy()}
 @Test fun rockHandRespectsFillAndOutline(){val prefs=app.getSharedPreferences("browser",0);prefs.edit().putString("pointer_fill","accent").putString("accent","Mint").putString("pointer_outline","white").commit();val v=MousePointer(app,"rock");v.layout(0,0,96,96);val b=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));val pixels=IntArray(96*96);b.getPixels(pixels,0,96,0,0,96,96);assertTrue(pixels.contains(BrowserTheme.accentColor("Mint",true)));assertTrue(pixels.contains(Color.WHITE));prefs.edit().putString("pointer_fill","black").putString("pointer_outline","none").commit();b.eraseColor(Color.TRANSPARENT);v.draw(Canvas(b));b.getPixels(pixels,0,96,0,0,96,96);assertTrue(pixels.contains(Color.BLACK));assertFalse(pixels.contains(Color.WHITE))}
 @Test fun bookmarkBarRemainsVisibleOnPocketBlankAndWebsiteTabs(){val c=start();val a=c.get();val prefs=app.getSharedPreferences("browser",0);prefs.edit().putBoolean("favorites_bar",true).commit();for(mode in listOf("pocket","blank")){StartPageStore(prefs).homeMode=mode;call(a,"newTab");settle(a);assertEquals(View.VISIBLE,(field(a,"favoritesBar") as View).visibility);assertTrue(desc(a,"All bookmarks").isShown)};call(a,"switchTab",0);settle(a);assertEquals(View.VISIBLE,(field(a,"favoritesBar") as View).visibility);prefs.edit().putBoolean("favorites_bar",false).commit();call(a,"updateChrome");assertEquals(View.GONE,(field(a,"favoritesBar") as View).visibility);c.pause().stop().destroy()}
 @Test fun switchingTabsKeepsAddressSurfaceAttachedAndOpaque(){val c=start();val a=c.get();call(a,"showPage");settle(a);val host=field(a,"addressHost") as View;val background=host.background;call(a,"newTab");call(a,"switchTab",0);assertSame(background,host.background);assertEquals(1f,host.alpha,0f);assertNull((field(a,"addressField") as View).background);c.pause().stop().destroy()}
 @Test fun settingsHeadingsShareStyleAndAlwaysBelongToAGroup(){val c=start();val a=c.get();for(category in SettingsDashboard.categories.keys){dashboard(a).select(category,true);settle(a);val headings=all(a).filterIsInstance<TextView>().filter{it.tag=="settings-heading"};for(h in headings){var parent=h.parent as? View;while(parent!=null&&parent.tag!="settings-group")parent=parent.parent as? View;assertNotNull("${h.text} has no group",parent);assertEquals(12*h.resources.displayMetrics.scaledDensity,h.textSize,.1f);assertEquals(Math.round(18*h.resources.displayMetrics.density),h.height)}};c.pause().stop().destroy()}
 @Test @Config(qualifiers="w800dp-h450dp-land-384dpi")
 fun bookmarksActionsShareRowAndSuggestionsFit(){val c=start();val a=c.get();call(a,"showCollection","bookmarks");settle(a);val enter=desc(a,"Enter a website");val bar=desc(a,"Show bookmarks bar");assertSame(enter.parent,bar.parent);assertEquals(enter.height,bar.height);assertTrue(bar.left>=enter.right);val scroll=all(a).filterIsInstance<ScrollView>().first{it.isShown};assertFalse(scroll.canScrollVertically(1));assertFalse(scroll.canScrollVertically(-1));bar.performClick();assertFalse(app.getSharedPreferences("browser",0).getBoolean("favorites_bar",true));shot(a,"full-hd-bookmarks-actions");c.pause().stop().destroy()}
 @Test fun pointerArtworkKeepsItsAspectInTallAndWidePreviewCells(){
 fun bounds(style:String,w:Int,h:Int):Rect{val v=MousePointer(app,style);v.layout(0,0,w,h);val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));val r=Rect(w,h,0,0);for(y in 0 until h)for(x in 0 until w)if(Color.alpha(b.getPixel(x,y))>0){r.left=minOf(r.left,x);r.top=minOf(r.top,y);r.right=maxOf(r.right,x+1);r.bottom=maxOf(r.bottom,y+1)};return r}
 for(style in listOf("windows","pixel","diamond")){val normal=bounds(style,72,92);for(size in listOf(72 to 200,200 to 92)){val stretched=bounds(style,size.first,size.second);assertEquals(style,normal.width(),stretched.width());assertEquals(style,normal.height(),stretched.height())}}
 }
 @Test fun menuEdgeUsesTheSelectedAccent(){val c=start();val a=c.get();val prefs=app.getSharedPreferences("browser",0);prefs.edit().putString("accent","Mint").commit();call(a,"showPage");call(a,"showMenu");settle(a);val panel=desc(a,"Menu panel");val b=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);panel.background.apply{setBounds(0,0,100,100);draw(Canvas(b))};assertEquals(BrowserTheme.accentColor("Mint",true),b.getPixel(99,50));c.pause().stop().destroy()}
 @Test fun appearanceChangesNeverFadeTheSettingsSurface(){val c=start();val a=c.get();dashboard(a).select("appearance",true);settle(a)
 for(id in listOf("Accent: Blue","Theme: Light","Theme: OLED")){
 val target=desc(a,id)
 target.performClick()
 var v:View?=dashboard(a)
 while(v!=null){assertEquals("Appearance must never fade through black",1f,v.alpha,0f);v=v.parent as? View}
 settle(a)
 if(id=="Theme: Light")shot(a,"appearance-light-checkmark")
 }
 c.pause().stop().destroy()
 }
 @Test fun suppliedCursorUsesTransparencyMask(){val image=CursorAsset.load(app);assertEquals(0,Color.alpha(image.getPixel(0,0)));assertTrue((0 until image.height).any{y->(0 until image.width).any{x->Color.alpha(image.getPixel(x,y))==255}})}
 @Test @Config(qualifiers="w800dp-h450dp-land-384dpi")
 fun fullHdIntermediateDisplayScaling(){checkFullHdSettings("full-hd-intermediate")}
 @Test fun slidersRequireActivationAndAllowCrossColumnNavigation(){val c=start();val a=c.get();dashboard(a).select("mouse",true);settle(a);val prefs=app.getSharedPreferences("browser",0);val before=prefs.getInt("mouse_speed",600);key(a,22);assertEquals(before,prefs.getInt("mouse_speed",600));assertTrue(a.window.decorView.findFocus()!!.contentDescription.toString().startsWith("Pointer style:"));key(a,21);assertEquals("Movement speed",a.window.decorView.findFocus()!!.contentDescription);key(a,96);key(a,22);assertEquals(before+30,prefs.getInt("mouse_speed",600));key(a,97);assertFalse(dashboard(a).sliderEditing());assertEquals("Movement speed",a.window.decorView.findFocus()!!.contentDescription);c.pause().stop().destroy()}
 @Test fun categoryNavigationReachesHeaderAndSettingsReopenOnBrowsing() {
  val c=start();val a=c.get()
  desc(a,"Category: browsing").requestFocus()
  key(a,KeyEvent.KEYCODE_DPAD_UP)
  assertEquals("Back",a.window.decorView.findFocus()?.contentDescription)
  key(a,KeyEvent.KEYCODE_DPAD_DOWN)
  assertEquals("Category: browsing",a.window.decorView.findFocus()?.contentDescription)
  dashboard(a).select("privacy",false)
  desc(a,"Category: privacy").requestFocus()
  key(a,KeyEvent.KEYCODE_BUTTON_B)
  call(a,"showSettingsRoot");settle(a)
  assertEquals("Category: browsing",a.window.decorView.findFocus()?.contentDescription)
  c.pause().stop().destroy()
 }
 @Test fun expandedMouseTestClosesWithBothBackInputsAndRestoresItsButton() {
  val c=start();val a=c.get();dashboard(a).select("mouse",true);settle(a)
  for(back in listOf(KeyEvent.KEYCODE_BUTTON_B,KeyEvent.KEYCODE_BACK)) {
   desc(a,"Test mouse navigation").performClick();settle(a)
   val area=desc(a,"Mouse test area")
   assertTrue(area.height>a.resources.displayMetrics.density*100)
   assertTrue(area.width>a.resources.displayMetrics.density*250)
   assertTrue(dashboard(a).mouseTesting())
   key(a,back)
   assertFalse(dashboard(a).mouseTesting())
   assertEquals("Test mouse navigation",a.window.decorView.findFocus()?.contentDescription)
  }
  c.pause().stop().destroy()
 }
 @Test @Config(qualifiers="w960dp-h540dp-land-xhdpi")
 fun fullHdSettingsAndPointerInput() {
  checkFullHdSettings("full-hd")
 }

 @Test @Config(qualifiers="w640dp-h360dp-land-xxhdpi")
 fun fullHdSettingsWithLargerDisplayScaling() {
  checkFullHdSettings("full-hd-large")
 }

 private fun checkFullHdSettings(snapshot: String) {
  val controller = start()
  val activity = controller.get()
  val decor = activity.window.decorView
  assertEquals(1920, decor.width)
  assertEquals(1080, decor.height)
  for (category in SettingsDashboard.categories.keys) {
   dashboard(activity).select(category, true)
   settle(activity)
   shot(activity, "$snapshot-$category")
   assertFits(activity)
   for (control in all(activity).filter { it.isShown && it.isClickable && it.contentDescription != null }) {
    val visible = Rect()
    assertTrue("${control.contentDescription} is outside its container", control.getGlobalVisibleRect(visible))
    assertEquals("${control.contentDescription} clipped vertically", control.height, visible.height())
    assertEquals("${control.contentDescription} clipped horizontally", control.width, visible.width())
   }
   shot(activity, "$snapshot-$category")
  }
  dashboard(activity).select("mouse", true)
  settle(activity)
  val prefs = activity.getSharedPreferences("browser", 0)
  val fill = desc(activity, "Fill")
  val outline = desc(activity, "Outline")
  val fillPosition = IntArray(2).also(fill::getLocationOnScreen)
  val outlinePosition = IntArray(2).also(outline::getLocationOnScreen)
  assertTrue(outlinePosition[1] > fillPosition[1] + fill.height)
  fill.requestFocus()
  val oldOutline = MousePointer.outline(prefs)
  key(activity, KeyEvent.KEYCODE_BUTTON_A)
  key(activity, KeyEvent.KEYCODE_DPAD_RIGHT)
  val changedFill = MousePointer.fill(prefs)
  assertEquals("black", changedFill)
  assertEquals(oldOutline, MousePointer.outline(prefs))
  key(activity, KeyEvent.KEYCODE_BUTTON_A)
  key(activity, KeyEvent.KEYCODE_DPAD_DOWN)
  assertEquals("Outline", decor.findFocus()?.contentDescription)
  key(activity, KeyEvent.KEYCODE_BUTTON_A)
  key(activity, KeyEvent.KEYCODE_DPAD_RIGHT)
  assertNotEquals(oldOutline, MousePointer.outline(prefs))
  assertEquals(changedFill, MousePointer.fill(prefs))
  desc(activity, "Fill previous").performClick()
  settle(activity)
  assertEquals("white", MousePointer.fill(prefs))
  dashboard(activity).select("appearance", true)
  controller.pause().stop().destroy()
 }
 private val app get()=RuntimeEnvironment.getApplication()
 @Before fun reset(){app.getSharedPreferences("browser",0).edit().clear().putBoolean("setup_complete",true).commit()}
 private fun field(a:BrowserActivity,n:String)=BrowserTestActions.field(a,n)
 private fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserTestActions.call(a,n,*args)
 private fun settle(a:BrowserActivity){repeat(4){BrowserTestActions.settle(a);TestFrameClock.advance(java.time.Duration.ofMillis(150))}}
 private fun all(a:BrowserActivity)=BrowserTestActions.views(a.window.decorView)
 private fun desc(a:BrowserActivity,n:String)=all(a).first{it.isShown&&it.contentDescription==n}
 private fun key(a:BrowserActivity,k:Int){a.dispatchKeyEvent(KeyEvent(0,k));a.dispatchKeyEvent(KeyEvent(1,k));settle(a)}
 private fun start()=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible().also{call(it.get(),"showSettingsRoot");settle(it.get())}
 private fun dashboard(a:BrowserActivity)=field(a,"settingsDashboard") as SettingsDashboard
 private fun assertFits(a:BrowserActivity){val panel=desc(a,"Settings detail pane");val p=IntArray(2);panel.getLocationOnScreen(p);val bottom=p[1]+panel.height;assertTrue(all(a).none{it is ScrollView});assertNull(field(a,"popupLayer"));for(v in all(a).filter{it.isShown&&it.isClickable&&it.contentDescription!=null&&it.contentDescription!="Back"}){val at=IntArray(2);v.getLocationOnScreen(at);assertTrue("${v.contentDescription} exceeds pane bottom: ${at[1]+v.height} > $bottom",at[1]+v.height<=bottom+1);assertTrue("${v.contentDescription} is clipped",v.width>0&&v.height>0)}}
 private fun shot(a:BrowserActivity,n:String){settle(a);val v=a.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));java.io.File("build/reports/settings-dashboard/$n.png").apply{parentFile.mkdirs();outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}};b.recycle()}
 @Test fun allCategoriesHaveFixedHeadingAndFitWithoutScrollOrPopup(){val c=start();val a=c.get();val headingPositions=mutableSetOf<Pair<Int,Int>>();for(category in SettingsDashboard.categories.keys){dashboard(a).select(category,true);settle(a);assertEquals("settings",field(a,"screen"));val heading=desc(a,"Settings section heading");val at=IntArray(2);heading.getLocationOnScreen(at);headingPositions.add(at[0] to at[1]);assertFits(a);shot(a,category)};assertEquals(1,headingPositions.size);c.pause().stop().destroy()}
 @Test fun railNavigationChangesCategoryImmediatelyAndBackReturnsToRailThenPage(){val c=start();val a=c.get();desc(a,"Category: mouse").requestFocus();settle(a);assertEquals("mouse",dashboard(a).category);key(a,22);assertEquals("Movement speed",(field(a,"focused") as View).contentDescription);key(a,96);key(a,22);assertEquals(630,app.getSharedPreferences("browser",0).getInt("mouse_speed",0));key(a,97);assertEquals("Movement speed",(field(a,"focused") as View).contentDescription);key(a,97);assertEquals("Category: mouse",(field(a,"focused") as View).contentDescription);key(a,97);assertEquals("page",field(a,"screen"));c.pause().stop().destroy()}
 @Test fun appearanceAndPointerChoicesSaveInlineAndKeepHeading(){val c=start();val a=c.get();dashboard(a).select("appearance",true);desc(a,"Theme: OLED").performClick();settle(a);assertEquals("settings",field(a,"screen"));assertEquals("appearance",dashboard(a).category);assertEquals("OLED",app.getSharedPreferences("browser",0).getString("theme",null));desc(a,"Accent: Red").performClick();settle(a);assertEquals("Red",app.getSharedPreferences("browser",0).getString("accent",null));dashboard(a).select("mouse",true);desc(a,"Pointer style: Pixel").performClick();settle(a);assertEquals("pixel",app.getSharedPreferences("browser",0).getString("pointer_style",null));assertFits(a);c.pause().stop().destroy()}
 @Test fun mappingRecordingAndCollisionStayInSameSettingsSurface(){val c=start();val a=c.get();dashboard(a).select("controller",true);desc(a,Control.ZOOM.title).performClick();settle(a);assertNotNull(desc(a,"Choose physical button"));desc(a,"Record a button").performClick();key(a,188);assertEquals(188,ControllerBindings(app.getSharedPreferences("browser",0)).key(Control.ZOOM));assertEquals("settings",field(a,"screen"));desc(a,Control.ZOOM.title).performClick();desc(a,"Record a button").performClick();key(a,96);assertNotNull(desc(a,"Cancel setting action"));assertFits(a);desc(a,"Cancel setting action").performClick();assertEquals(188,ControllerBindings(app.getSharedPreferences("browser",0)).key(Control.ZOOM));c.pause().stop().destroy()}
 @Test fun privacyConfirmationIsInlineAndCancelDoesNotClearHistory(){val prefs=app.getSharedPreferences("browser",0);BrowserData(prefs).record("https://saved.test/","Saved");val c=start();val a=c.get();dashboard(a).select("privacy",true);desc(a,"Clear history").performClick();settle(a);assertFits(a);assertEquals(1,BrowserData(prefs).entries("history").size);desc(a,"Cancel setting action").performClick();assertEquals(1,BrowserData(prefs).entries("history").size);desc(a,"Clear history").performClick();desc(a,"Confirm setting action").performClick();assertTrue(BrowserData(prefs).entries("history").isEmpty());c.pause().stop().destroy()}
 @Test fun customHomeEditorFitsWithoutRetiredEngineControls(){val c=start();val a=c.get();val p=app.getSharedPreferences("browser",0);assertFalse(all(a).any{it.contentDescription=="Search engine"||it.contentDescription=="Engine name"});desc(a,"Start page").performClick();desc(a,"Start page previous").performClick();settle(a);assertNotNull(desc(a,"Start page address"));assertEquals("pocket",StartPageStore(p).homeMode);assertFits(a);shot(a,"custom-editors");c.pause().stop().destroy()}
 @Test fun mouseTestUsesL3AndNoPageNavigationHintsLeakIntoSettings(){val c=start();val a=c.get();dashboard(a).select("mouse",true);key(a,106);assertTrue(dashboard(a).mouseTesting());assertTrue(all(a).filterIsInstance<TextView>().any{it.text=="Stop test"});key(a,106);assertFalse(dashboard(a).mouseTesting());dashboard(a).select("privacy",true);val legend=field(a,"legend") as ViewGroup;assertFalse(BrowserTestActions.views(legend).filterIsInstance<TextView>().any{it.text in listOf("Surf Bar","Close tab","Scroll","Zoom")});c.pause().stop().destroy()}
 @Test @Config(qualifiers="w960dp-h540dp-land-mdpi") fun wideLayoutAndLightThemeKeepConsistentSurfaces(){app.getSharedPreferences("browser",0).edit().putString("theme","Light").putString("accent","Red").commit();val c=start();val a=c.get();for(key in SettingsDashboard.categories.keys){dashboard(a).select(key,true);settle(a);assertFits(a);shot(a,"light-$key")};c.pause().stop().destroy()}
}

