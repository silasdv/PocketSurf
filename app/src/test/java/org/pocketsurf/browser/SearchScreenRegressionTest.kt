package org.pocketsurf.browser

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
class SearchScreenRegressionTest {
 @Before fun reset(){RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().clear().putBoolean("setup_complete",true).commit()}
 private fun field(a:BrowserActivity,n:String)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 private fun settle(a:BrowserActivity){repeat(4){val v=a.window.decorView;v.measure(View.MeasureSpec.makeMeasureSpec((a.resources.configuration.screenWidthDp*a.resources.displayMetrics.density).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((a.resources.configuration.screenHeightDp*a.resources.displayMetrics.density).toInt(),View.MeasureSpec.EXACTLY));v.layout(0,0,v.measuredWidth,v.measuredHeight);TestFrameClock.advance(Duration.ofMillis(100))}}
 @Test fun interruptedTransitionsRetainInputWebViewAndTabChildren(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible()
  val a=c.get();settle(a);val web=call(a,"getWeb");val row=field(a,"tabRow") as ViewGroup;val children=(0 until row.childCount).map{row.getChildAt(it)}
  call(a,"showSearch",call(a,"getTab"),null,"git");settle(a)
  val input=field(a,"addressField") as EditText;assertEquals("git",input.text.toString());assertTrue((field(a,"goButton") as View).isEnabled)
  (call(a,"getTab") as BrowserActivity.Tab).title="Finished while searching";call(a,"updateChrome")
  call(a,"cancelAddress",false);call(a,"showSearch",call(a,"getTab"),null,"repo");call(a,"cancelAddress",false);settle(a)
  assertSame(web,call(a,"getWeb"));assertNull(field(a,"suggestions"));assertEquals(children,(0 until row.childCount).map{row.getChildAt(it)});assertEquals(1f,(field(a,"tabNavigation") as View).alpha,.001f)
  c.pause().stop().destroy()
 }
 private fun set(a:BrowserActivity,n:String,value:Any?)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.set(a,value)
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 @Test fun measuredSpaceLimitsCompleteRowsAndSingleShortcutRow(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible();val a=c.get();settle(a)
  call(a,"showSearch",call(a,"getTab"),null,"");settle(a)
  var previous=Int.MAX_VALUE
  for(fraction in listOf(0f,.5f,.6f,.7f)){
   val root=field(a,"root") as View;root.setPadding(0,0,0,(540*fraction).toInt());settle(a)
   set(a,"searchCompletions",(1..20).map{"Suggestion $it"});set(a,"searchPages",SearchDestinations.rank((1..20).map{Visit("https://site$it.test/path","Site $it",it.toLong())}));set(a,"searchRenderSignature","");call(a,"renderSearchResults")
   settle(a);val rows=field(a,"googleRows") as ViewGroup;val sites=field(a,"websiteRows") as ViewGroup
   assertTrue(rows.childCount<=previous);previous=rows.childCount
   for(i in 0 until rows.childCount)assertTrue(rows.getChildAt(i).bottom<=rows.height)
   for(i in 0 until sites.childCount){assertEquals(0,sites.getChildAt(i).top);assertTrue(sites.getChildAt(i).right<=sites.width)}
  }
  c.pause().stop().destroy()
 }
 @Test @Config(qualifiers="w833dp-h468dp-land-369dpi") fun compactShortcutsRemainVisibleWithSeventyPercentOcclusion(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible();val a=c.get();settle(a);call(a,"showSearch",call(a,"getTab"),null,"");settle(a)
  val root=field(a,"root") as View;root.setPadding(0,0,0,(root.height*.7f).toInt());settle(a)
  set(a,"searchPages",SearchDestinations.rank(listOf(Visit("https://site.test/path","Site",1))));set(a,"searchRenderSignature","");call(a,"renderSearchResults");settle(a)
  val rows=field(a,"websiteRows") as ViewGroup;assertEquals(1,rows.childCount);val item=rows.getChildAt(0);assertTrue(item.height<88*a.resources.displayMetrics.density);assertTrue(rows.top+item.height<=(rows.parent as View).height)
  c.pause().stop().destroy()
 }
 @Test fun savedAndSuggestedBookmarksCoexistAndRemovalRestoresSuggestion(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible();val a=c.get();settle(a);call(a,"showCollection","bookmarks");settle(a)
  all(a.window.decorView).first{it.contentDescription=="Google"&&it.isShown}.performClick();settle(a)
  assertTrue(all(a.window.decorView).any{it.contentDescription=="YouTube"&&it.isShown})
  assertTrue(all(a.window.decorView).any{it.contentDescription=="Remove Google"&&it.isShown})
  all(a.window.decorView).first{it.contentDescription=="Remove Google"&&it.isShown}.performClick();settle(a)
  assertTrue(all(a.window.decorView).any{it.contentDescription=="Google"&&it.isShown})
  c.pause().stop().destroy()
 }
 @Test fun appearanceRefreshPreservesQueryCursorAndKeyboardPreference(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("https://fixture.test/"))).setup().visible();val a=c.get();settle(a);call(a,"showSearch",call(a,"getTab"),null,"repository");settle(a)
  for(hidden in listOf(false,true)){
   set(a,"addressKeyboardDismissed",hidden);(field(a,"addressField") as EditText).setSelection(3);call(a,"applyTheme");settle(a)
   val input=field(a,"addressField") as EditText;assertEquals("repository",input.text.toString());assertEquals(3,input.selectionStart);assertEquals(hidden,field(a,"addressKeyboardDismissed"))
  }
  c.pause().stop().destroy()
 }
 @Test fun startupSubmitIsClickableAndScreenExists(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java,Intent(Intent.ACTION_MAIN)).setup().visible();val a=c.get();settle(a)
  assertEquals(true,field(a,"addressEditing"));assertNotNull(field(a,"suggestions"));(field(a,"addressField") as EditText).setText("https://fixture.test/path")
  assertTrue((field(a,"goButton") as View).isEnabled);assertTrue((field(a,"goButton") as View).performClick());settle(a);assertEquals(false,field(a,"addressEditing"));assertNull(field(a,"suggestions"));assertEquals("https://fixture.test/path",(call(a,"getTab") as BrowserActivity.Tab).url)
  c.pause().stop().destroy()
 }
}
