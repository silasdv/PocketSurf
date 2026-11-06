package org.pocketsurf.browser

import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
class SearchRefinementTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0)
 @Before fun reset(){prefs.edit().clear().putBoolean("setup_complete",true).commit()}
 private fun field(a:BrowserActivity,n:String)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun set(a:BrowserActivity,n:String,v:Any?)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.set(a,v)
 private fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun layout(a:BrowserActivity){val v=a.window.decorView;val density=a.resources.displayMetrics.density;v.measure(View.MeasureSpec.makeMeasureSpec((a.resources.configuration.screenWidthDp*density).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((a.resources.configuration.screenHeightDp*density).toInt(),View.MeasureSpec.EXACTLY));v.layout(0,0,v.measuredWidth,v.measuredHeight)}
 private fun settle(a:BrowserActivity){repeat(4){layout(a);TestFrameClock.advance(Duration.ofMillis(100))}}
 private fun start()=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible().also{settle(it.get())}
 private fun open(a:BrowserActivity){call(a,"showSearch",call(a,"getTab"),null,"");settle(a)}
 private fun node(a:BrowserActivity,description:String)=all(a.window.decorView).first{it.isShown&&it.contentDescription==description}
 @Test fun typingSuggestionsAndImeChangesRetainBubbleObjectsAndOrder(){
  val c=start();val a=c.get();open(a)
  val history=(1..10).map{Visit("https://site$it.test/page?id=$it","Page title $it",it.toLong())}
  set(a,"searchPages",SearchDestinations.rank(history));call(a,"renderSearchResults");layout(a)
  val row=field(a,"websiteRows") as ViewGroup;val original=(0 until row.childCount).map{row.getChildAt(it)}
  for(query in listOf("git","cats","youtube","")){
   (field(a,"addressField") as EditText).setText(query);set(a,"searchCompletions",listOf("One","Two","Three"));call(a,"renderSearchResults");layout(a)
   assertEquals(original,(0 until row.childCount).map{row.getChildAt(it)})
  }
  val root=field(a,"root") as View;root.setPadding(0,0,0,270);layout(a);call(a,"renderSearchResults");layout(a)
  assertEquals(original,(0 until row.childCount).map{row.getChildAt(it)})
  val labels=all(row).filterIsInstance<TextView>().map{it.text.toString()};assertTrue(labels.all{!it.contains("://")&&!it.contains(".test")&&!it.contains("/page")})
  assertFalse(all(a.window.decorView).any{it.contentDescription=="Show or hide keyboard"})
  assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any{it.text=="Suggestions"})
  c.pause().stop().destroy()
 }
 @Test @Config(qualifiers="w1920dp-h720dp-land-mdpi") fun searchBubblesCanExceedEight(){
  val c=start();val a=c.get();open(a);set(a,"searchPages",SearchDestinations.rank((1..30).map{Visit("https://site$it.test/","Site $it",it.toLong())}));call(a,"renderSearchResults");layout(a)
  assertTrue((field(a,"websiteRows") as ViewGroup).childCount>8);c.pause().stop().destroy()
 }
 @Test fun recentSearchesSwitchImmediatelyWhenInputIsClearedAndSubmissionIsRecorded(){
  val data=BrowserData(prefs);data.recordSearch("Android WebView","https://www.google.com/search?q=Android+WebView",10);data.recordSearch("Retroid Pocket","https://www.google.com/search?q=Retroid+Pocket",20)
  val c=start();val a=c.get();open(a)
  set(a,"recentQueries",data.recentSearches().map{it.title});call(a,"showAddressSuggestions","")
  val row=field(a,"googleRows") as ViewGroup;assertEquals("Retroid Pocket",row.getChildAt(0).contentDescription)
  val input=field(a,"addressField") as EditText;input.setText("android");set(a,"searchCompletions",listOf("android webview","android games"));call(a,"renderSearchResults");assertEquals("android webview",row.getChildAt(0).contentDescription)
  input.setText("");assertEquals("Retroid Pocket",row.getChildAt(0).contentDescription)
  row.getChildAt(0).performClick();assertEquals("Retroid Pocket",data.recentSearches().first().title);assertEquals(false,field(a,"addressEditing"))
  call(a,"showSearch",call(a,"getTab"),null,"https://direct.test/path");call(a,"submitAddress");assertEquals(2,data.recentSearches().size)
  c.pause().stop().destroy()
 }
 @Test fun backAndForwardFadeThroughClosingAndKeepNavigationState(){
  val c=start();val a=c.get();val web=call(a,"getWeb");val tabs=field(a,"tabRow") as ViewGroup;val children=(0 until tabs.childCount).map{tabs.getChildAt(it)}
  val normalWidth=(field(a,"backButton") as View).width
  open(a);call(a,"cancelAddress",false);TestFrameClock.advance(Duration.ofMillis(90));layout(a)
  val back=field(a,"backButton") as View;val forward=field(a,"forwardButton") as View
  assertEquals(View.VISIBLE,back.visibility);assertTrue(back.alpha>0f&&back.alpha<1f);assertTrue(back.width in 1 until normalWidth);assertFalse(back.isClickable)
  val tab=call(a,"getTab") as BrowserActivity.Tab;tab.manualHistory=true;tab.trail=mutableListOf("https://previous.test/","https://fixture.test/","https://next.test/");tab.trailIndex=1
  call(a,"updateNavigationState");assertTrue(forward.isEnabled);assertTrue(forward.alpha<1f)
  settle(a);assertEquals(1f,back.alpha,.001f);assertEquals(1f,forward.alpha,.001f);assertTrue(back.isClickable);assertSame(web,call(a,"getWeb"));assertEquals(children,(0 until tabs.childCount).map{tabs.getChildAt(it)})
  c.pause().stop().destroy()
 }
 @Test fun bubbleInteriorIsTransparentAndFocusDoesNotResizeIt(){
  val c=start();val a=c.get();open(a);set(a,"searchPages",listOf(Visit("https://github.com/user/repository","Repository - GitHub",1)));call(a,"renderSearchResults");layout(a)
  val row=field(a,"websiteRows") as ViewGroup;val bubble=row.getChildAt(0) as HistoryBubbleView;val circle=bubble.getChildAt(0);val width=bubble.width
  assertEquals(Color.TRANSPARENT,(circle.background as GradientDrawable).color!!.defaultColor);assertNull(bubble.background);assertFalse(bubble.defaultFocusHighlightEnabled);assertEquals("GitHub",bubble.contentDescription)
  bubble.requestFocus();layout(a);assertEquals(width,bubble.width);assertNull(bubble.background);assertEquals(Color.TRANSPARENT,(circle.background as GradientDrawable).color!!.defaultColor)
  c.pause().stop().destroy()
 }
 @Test fun unchangedFocusPaintKeepsResultDrawablesAndLayoutStable(){
  val c=start();val a=c.get();open(a);set(a,"searchPages",listOf(Visit("https://github.com/u/repo","Repository",1)));set(a,"searchCompletions",listOf("android","android webview"));call(a,"renderSearchResults");settle(a)
  val bubble=(field(a,"websiteRows") as ViewGroup).getChildAt(0) as ViewGroup
  val suggestion=(field(a,"googleRows") as ViewGroup).getChildAt(0) as ViewGroup;val decoration=suggestion.getChildAt(0).background
  repeat(3){call(a,"syncFocusMode");call(a,"renderSearchResults")}
  assertSame(decoration,suggestion.getChildAt(0).background);assertFalse(bubble.isLayoutRequested);assertFalse(suggestion.isLayoutRequested)
  c.pause().stop().destroy()
 }
 @Test fun suggestionViewsOnlyFadeWhenInsertedAndStaleQueriesClearImmediately(){
  val c=start();val a=c.get();open(a);val input=field(a,"addressField") as EditText
  input.setText("android");set(a,"searchCompletions",listOf("android games","android webview"));call(a,"renderSearchResults");settle(a)
  val row=field(a,"googleRows") as ViewGroup;val first=row.getChildAt(0);val second=row.getChildAt(1)
  first.requestFocus();set(a,"focused",first)
  set(a,"searchCompletions",listOf("android games","android webview","android studio"));call(a,"renderSearchResults")
  assertSame(first,row.getChildAt(0));assertSame(second,row.getChildAt(1));assertEquals(1f,first.alpha,0f);assertSame(first,a.window.decorView.findFocus())
  val added=row.getChildAt(2);assertTrue(added.alpha<1f);settle(a);assertEquals(1f,added.alpha,.01f)
  call(a,"renderSearchResults");assertSame(added,row.getChildAt(2));assertEquals(1f,added.alpha,0f)
  input.setText("entirely different");assertEquals(0,row.childCount)
  input.setText("");set(a,"recentQueries",listOf("Recent actual query"));call(a,"showAddressSuggestions","");assertEquals("Recent actual query",row.getChildAt(0).contentDescription)
  c.pause().stop().destroy()
 }
 @Test fun unavailableNavigationHintsRemainVisibleAndNeverChangeGeometry(){
  val c=start();val a=c.get();val t=call(a,"getTab") as BrowserActivity.Tab
  t.manualHistory=true;t.trail=mutableListOf("https://fixture.test/","https://next.test/");t.trailIndex=0
  call(a,"updateNavigationState");settle(a)
  val back=field(a,"backButton") as ViewGroup;val forward=field(a,"forwardButton") as ViewGroup
  val l=all(back).filterIsInstance<TextView>().first{it.text=="L"};val r=all(forward).filterIsInstance<TextView>().first{it.text=="R"}
  val bounds=listOf(l.left,l.top,l.width,l.height,r.left,r.top,r.width,r.height)
  assertEquals(View.VISIBLE,l.visibility);assertEquals(View.VISIBLE,r.visibility);assertFalse(back.isEnabled);assertTrue(forward.isEnabled);assertNotEquals(l.currentTextColor,r.currentTextColor)
  t.trailIndex=1;call(a,"updateNavigationState");settle(a);assertTrue(back.isEnabled);assertFalse(forward.isEnabled)
  assertEquals(bounds,listOf(l.left,l.top,l.width,l.height,r.left,r.top,r.width,r.height))
  open(a);call(a,"cancelAddress",false);settle(a);assertEquals(View.VISIBLE,l.visibility);assertEquals(View.VISIBLE,r.visibility)
  call(a,"newTab");settle(a);assertFalse(back.isEnabled);assertFalse(forward.isEnabled);assertEquals(View.VISIBLE,l.visibility);assertEquals(View.VISIBLE,r.visibility)
  call(a,"switchTab",0);settle(a);assertTrue(back.isEnabled);assertFalse(forward.isEnabled)
  c.pause().stop().destroy()
 }
 @Test fun browsingSettingsUseCompactFullWidthRowsAndSeparateBookmarks(){
  val c=start();val a=c.get();call(a,"showSettingsPanel","browsing");settle(a)
  val start=node(a,"Start page");val default=node(a,"Default browser");val zoom=node(a,"Page zoom");val clear=node(a,"Clear bookmarks");val toggle=node(a,"Show bookmarks bar while browsing")
  assertEquals(start.width,default.width);assertTrue(default.height<=48*a.resources.displayMetrics.density)
  assertTrue(zoom.top>=0);assertSame(clear.parent,toggle.parent)
  assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any{it.tag=="settings-heading"&&it.text=="Bookmarks"})
  c.pause().stop().destroy()
 }
 @Test fun clearBookmarksIsBesideTheToggleAndRequiresConfirmation(){
  val data=BrowserData(prefs);repeat(12){data.record("https://bookmark$it.test/","Bookmark $it","bookmarks")};data.record("https://history.test/","History")
  val c=start();val a=c.get();call(a,"showSettingsPanel","browsing");settle(a)
  val clear=node(a,"Clear bookmarks");val toggle=node(a,"Show bookmarks bar while browsing");assertSame(clear.parent,toggle.parent)
  clear.performClick();settle(a);assertEquals(12,data.entries("bookmarks").size)
  node(a,"Cancel setting action").performClick();assertEquals(12,data.entries("bookmarks").size)
  clear.performClick();node(a,"Confirm setting action").performClick();assertTrue(data.entries("bookmarks").isEmpty());assertTrue(data.entries("history").any{it.url=="https://history.test/"})
  assertFalse(all(a.window.decorView).any{it.contentDescription=="Search engine"});c.pause().stop().destroy()
 }
}
