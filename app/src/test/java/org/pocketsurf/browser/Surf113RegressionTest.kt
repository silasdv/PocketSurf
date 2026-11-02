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
class Surf113RegressionTest {
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
 @Test fun defaultButtonsAndLegacyBackupsMigrateWithoutLosingCustomBindings(){
  val legacy=JSONObject();Control.entries.forEach{legacy.put("key_${it.name}",if(it==Control.SEARCH)108 else if(it==Control.MENU)109 else it.defaultKey)};legacy.put("key_CONFIRM",100).put("key_CONTEXT",96)
  val raw=JSONObject().put("format","PocketSurf backup").put("version",1).put("values",legacy).toString();BackupCodec.apply(prefs,BackupCodec.validate(raw));val bindings=ControllerBindings(prefs)
  assertEquals(108,bindings.key(Control.MENU));assertEquals(109,bindings.key(Control.SEARCH));assertEquals(100,bindings.key(Control.CONFIRM));assertEquals(96,bindings.key(Control.CONTEXT));assertEquals(Control.entries.size,Control.entries.map{bindings.key(it)}.distinct().size)
  val current=BackupCodec.validate(BackupCodec.export(prefs));assertEquals(2,current.getInt("controls_layout_version"));val destination=RuntimeEnvironment.getApplication().getSharedPreferences("restored",0);destination.edit().clear().commit();BackupCodec.apply(destination,current);val restored=ControllerBindings(destination);Control.entries.forEach{assertEquals(bindings.key(it),restored.key(it))}
 }
 @Test fun menuAndAddressBindingsStayCustomWhenUpdating(){prefs.edit().putInt("key_MENU",188).putInt("key_SEARCH",189).commit();val b=ControllerBindings(prefs);assertEquals(188,b.key(Control.MENU));assertEquals(189,b.key(Control.SEARCH));assertEquals(Control.entries.size,Control.entries.map{b.key(it)}.distinct().size)}
 @Test fun onlyHistoryKeepsInlineHintsAndSurfBarIsInLegend(){val c=start(3);val a=c.get();for(name in listOf("Settings","Menu","Open tabs"))assertTrue(all(desc(a,name)).none{it.tag==0x7f0f0108});assertTrue(texts(desc(a,"Back")).contains("L"));assertFalse(texts(field(a,"addressHost") as View).contains("Select"));val b=ControllerBindings(prefs);b.assign(Control.CONTEXT,100);call(a,"updateChrome");assertTrue((field(a,"legendHost") as View).contentDescription.toString().contains("Y Surf Bar"));shot(a,"surf-bar-shortcuts");c.pause().stop().destroy()}
 @Test fun selectOpensMenuBBackAndXClosesTab(){val c=start();val a=c.get();key(a,109);assertNotNull(desc(a,"Menu popup"));key(a,97);key(a,97);assertEquals(1,count(a));assertTrue((call(a,"getTab") as BrowserActivity.Tab).home);key(a,99);assertEquals(0,count(a));key(a,97);assertTrue(a.isFinishing);c.pause().stop().destroy()}
 @Test fun selectFocusesAddressAndADecidesWhenToOpenKeyboard(){val c=start();val a=c.get();BrowserTestActions.focusAddress(a);val address=field(a,"addressField") as EditText;assertSame(address,a.window.decorView.findFocus());assertEquals(false,field(a,"addressEditing"));assertFalse(address.showSoftInputOnFocus);key(a,96);assertEquals(true,field(a,"addressEditing"));assertTrue(desc(a,"Go").isShown);c.pause().stop().destroy()}
 @Test fun startOpensSettingsAndSettingsHasNoSubtitle(){val c=start();val a=c.get();key(a,108);assertEquals("settings",field(a,"screen"));assertFalse(texts(a.window.decorView).contains("Choose a category"));val before=a.window.decorView.findFocus();key(a,108);assertSame(before,a.window.decorView.findFocus());key(a,100);assertEquals(1,count(a));assertEquals("settings",field(a,"screen"));c.pause().stop().destroy()}
 @Test fun yFocusesSurfBarAndXClosesWithoutOpeningOverview(){val c=start();val a=c.get();key(a,100);assertEquals(1,count(a));assertEquals("New tab",a.window.decorView.findFocus()?.contentDescription);key(a,96);assertEquals(2,count(a));BrowserTestActions.openTabs(a);assertEquals("tabs",field(a,"screen"));key(a,97);assertEquals("page",field(a,"screen"));assertEquals(2,count(a));key(a,99);assertEquals(1,count(a));c.pause().stop().destroy()}
 @Test fun yIsInactiveInMenusAndOverview(){val c=start(3);val a=c.get();key(a,109);key(a,100);assertEquals(3,count(a));assertNotNull(desc(a,"Menu popup"));key(a,97);BrowserTestActions.openTabs(a);desc(a,"Tab: Site 1").requestFocus();key(a,99);assertEquals(2,count(a));assertEquals("Tab: Site 2",a.window.decorView.findFocus()?.contentDescription);key(a,100);assertEquals(2,count(a));c.pause().stop().destroy()}
 @Test fun tabTriggersAreAtStripEdgesAndBoundariesAreNoOps(){val c=start(3);val a=c.get();assertTrue((field(a,"jumpHints") as List<*>).isEmpty());assertNotNull(desc(a,"Previous tab shortcut"));assertNotNull(desc(a,"Next tab shortcut"));key(a,105);key(a,105);RecordingWebViewShadow.clearEvidence();val before=web(a);key(a,105);assertSame(before,web(a));assertTrue(RecordingWebViewShadow.scripts.isEmpty());shot(a,"strip-edge-shortcuts");c.pause().stop().destroy()}
 @Test fun heldAOpensContextInTheSameBrowserMenuWithoutClickingLink(){val c=start();val a=c.get();val v=web(a);RecordingWebViewShadow.clearEvidence();a.dispatchKeyEvent(KeyEvent(0,96));assertFalse(RecordingWebViewShadow.scripts.any{it.contains(".activationPoint(")});Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600));val shadow=Shadows.shadowOf(v);assertTrue("Scripts: ${RecordingWebViewShadow.scripts}; focus=${field(a,"focused")}; held=${field(a,"heldConfirmKey")}; handled=${field(a,"heldConfirmHandled")}",shadow.lastEvaluatedJavascript.contains(".target()"));shadow.lastEvaluatedJavascriptCallback.onReceiveValue(JSONObject.quote("""{"link":"https://destination.test/","title":"Chosen link","viewportWidth":960}"""));a.dispatchKeyEvent(KeyEvent(1,96));settle(a);assertNotNull(desc(a,"Menu popup"));assertTrue(texts(a.window.decorView).contains("Open in new tab"));assertTrue(texts(a.window.decorView).contains("Bookmarks"));assertEquals(1,count(a));assertSame(v,web(a));shot(a,"context-in-browser-menu");key(a,97);key(a,109);assertFalse(texts(a.window.decorView).contains("Open in new tab"));assertNotNull(desc(a,"Menu popup"));c.pause().stop().destroy()}
 @Test fun shortAActivatesOnceOnReleaseAndCancelledPressDoesNothing(){val c=start();val a=c.get();RecordingWebViewShadow.clearEvidence();a.dispatchKeyEvent(KeyEvent(0,96));assertFalse(RecordingWebViewShadow.scripts.any{it.contains(".activationPoint(")});a.dispatchKeyEvent(KeyEvent(1,96));assertEquals(1,RecordingWebViewShadow.scripts.count{it.contains(".activationPoint(")});RecordingWebViewShadow.clearEvidence();a.dispatchKeyEvent(KeyEvent(0,96));a.dispatchKeyEvent(KeyEvent(0L,0L,1,96,0,0,0,0,KeyEvent.FLAG_CANCELED));assertFalse(RecordingWebViewShadow.scripts.any{it.contains(".activationPoint(")});c.pause().stop().destroy()}
 @Test fun longPressCannotOpenOptionsForADifferentTab(){val c=start(2);val a=c.get();a.dispatchKeyEvent(KeyEvent(0,96));key(a,105);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600));a.dispatchKeyEvent(KeyEvent(1,96));assertFalse(views(a).any{it.contentDescription=="Menu popup"});assertEquals(2,count(a));c.pause().stop().destroy()}
 @Test fun aboutDefaultBrowserOpensAndroidDefaultAppSelection(){val c=start();val a=c.get();val u=DashboardChecks;u.open(a,"browsing");u.click(a,"Default browser");val intent=Shadows.shadowOf(a).nextStartedActivity;assertEquals(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,intent.action);c.pause().stop().destroy()}
}
