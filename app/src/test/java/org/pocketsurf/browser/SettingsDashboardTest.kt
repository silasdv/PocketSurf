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
 private val app get()=RuntimeEnvironment.getApplication()
 @Before fun reset(){app.getSharedPreferences("browser",0).edit().clear().putBoolean(SetupActivity.COMPLETE,true).commit()}
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
 @Test fun railNavigationChangesCategoryImmediatelyAndBackReturnsToRailThenPage(){val c=start();val a=c.get();desc(a,"Category: mouse").requestFocus();settle(a);assertEquals("mouse",dashboard(a).category);key(a,22);assertEquals("Movement speed",(field(a,"focused") as View).contentDescription);key(a,22);assertEquals(630,app.getSharedPreferences("browser",0).getInt("mouse_speed",0));key(a,97);assertEquals("Category: mouse",(field(a,"focused") as View).contentDescription);key(a,97);assertEquals("page",field(a,"screen"));c.pause().stop().destroy()}
 @Test fun appearanceAndPointerChoicesSaveInlineAndKeepHeading(){val c=start();val a=c.get();dashboard(a).select("appearance",true);desc(a,"Theme: OLED").performClick();settle(a);assertEquals("settings",field(a,"screen"));assertEquals("appearance",dashboard(a).category);assertEquals("OLED",app.getSharedPreferences("browser",0).getString("theme",null));desc(a,"Accent: Red").performClick();settle(a);assertEquals("Red",app.getSharedPreferences("browser",0).getString("accent",null));dashboard(a).select("mouse",true);desc(a,"Pointer style: Pixel").performClick();settle(a);assertEquals("pixel",app.getSharedPreferences("browser",0).getString("pointer_style",null));assertFits(a);c.pause().stop().destroy()}
 @Test fun mappingRecordingAndCollisionStayInSameSettingsSurface(){val c=start();val a=c.get();dashboard(a).select("controller",true);desc(a,Control.ZOOM.title).performClick();settle(a);assertNotNull(desc(a,"Choose physical button"));desc(a,"Record a button").performClick();key(a,188);assertEquals(188,ControllerBindings(app.getSharedPreferences("browser",0)).key(Control.ZOOM));assertEquals("settings",field(a,"screen"));desc(a,Control.ZOOM.title).performClick();desc(a,"Record a button").performClick();key(a,96);assertNotNull(desc(a,"Cancel setting action"));assertFits(a);desc(a,"Cancel setting action").performClick();assertEquals(188,ControllerBindings(app.getSharedPreferences("browser",0)).key(Control.ZOOM));c.pause().stop().destroy()}
 @Test fun privacyConfirmationIsInlineAndCancelDoesNotClearHistory(){val prefs=app.getSharedPreferences("browser",0);BrowserData(prefs).record("https://saved.test/","Saved");val c=start();val a=c.get();dashboard(a).select("privacy",true);desc(a,"Clear history").performClick();settle(a);assertFits(a);assertEquals(1,BrowserData(prefs).entries("history").size);desc(a,"Cancel setting action").performClick();assertEquals(1,BrowserData(prefs).entries("history").size);desc(a,"Clear history").performClick();desc(a,"Confirm setting action").performClick();assertTrue(BrowserData(prefs).entries("history").isEmpty());c.pause().stop().destroy()}
 @Test fun simultaneousCustomSearchAndHomeEditorsFitAndUnfinishedHomeIsNotSaved(){val c=start();val a=c.get();val p=app.getSharedPreferences("browser",0);call(a,"settingsAction","focus","Search engine");repeat(3){desc(a,"Search engine previous").performClick()};repeat(3){desc(a,"Search engine next").performClick()};desc(a,"Search engine previous").performClick();settle(a);assertNotNull(desc(a,"Engine name"));desc(a,"Start page previous").performClick();settle(a);assertNotNull(desc(a,"Start page address"));assertEquals("pocket",StartPageStore(p).homeMode);assertFits(a);shot(a,"custom-editors");c.pause().stop().destroy()}
 @Test fun mouseTestUsesL3AndNoPageNavigationHintsLeakIntoSettings(){val c=start();val a=c.get();dashboard(a).select("mouse",true);key(a,106);assertTrue(dashboard(a).mouseTesting());assertTrue(all(a).filterIsInstance<TextView>().any{it.text=="Stop test"});key(a,106);assertFalse(dashboard(a).mouseTesting());dashboard(a).select("privacy",true);val legend=field(a,"legend") as ViewGroup;assertFalse(BrowserTestActions.views(legend).filterIsInstance<TextView>().any{it.text in listOf("Surf Bar","Close tab","Scroll","Zoom")});c.pause().stop().destroy()}
 @Test @Config(qualifiers="w960dp-h540dp-land-mdpi") fun wideLayoutAndLightThemeKeepConsistentSurfaces(){app.getSharedPreferences("browser",0).edit().putString("theme","Light").putString("accent","Red").commit();val c=start();val a=c.get();for(key in SettingsDashboard.categories.keys){dashboard(a).select(key,true);settle(a);assertFits(a);shot(a,"light-$key")};c.pause().stop().destroy()}
}
