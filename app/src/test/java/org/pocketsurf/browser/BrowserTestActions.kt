package org.pocketsurf.browser

import android.view.View
import android.view.ViewGroup
import java.time.Duration

/** Existing feature fixtures enter through the visible actions; SurfBarRegressionTest tests physical keys. */
internal object BrowserTestActions {
 fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
 fun field(a:BrowserActivity,n:String)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 fun settle(a:BrowserActivity){repeat(2){a.window.decorView.apply{val d=a.resources.displayMetrics.density;val c=a.resources.configuration;measure(View.MeasureSpec.makeMeasureSpec((c.screenWidthDp*d).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((c.screenHeightDp*d).toInt(),View.MeasureSpec.EXACTLY));layout(0,0,measuredWidth,measuredHeight)};TestFrameClock.advance(Duration.ofMillis(32))}}
 private fun click(a:BrowserActivity,label:String){views(a.window.decorView).first{it.isShown&&it.contentDescription==label&&it.hasOnClickListeners()}.performClick();settle(a)}
 /** Navigate the actual category UI when an older feature fixture requests a moved setting. */
 fun settingsClick(a:BrowserActivity,label:String):Boolean{
  val aliases=mapOf("Appearance" to "Theme and accent","Reading and accessibility" to "Website text size","Privacy and browsing data" to "Browsing data")
  val category=when(label){"Appearance","Theme and accent","Reading and accessibility","Website text size","Touch comfort","Touch comfort: on","Touch comfort: off"->"Appearance & accessibility";"Search engine","Start page","Bookmarks bar","Default browser"->"Browsing";"Controller","Mouse"->"Controls";"Adblock","Cookie consent","Privacy and browsing data","Browsing data"->"Privacy & protection";"Backup and restore","Export backup","Import backup"->"Backup & restore";"Help and controls","About PocketSurf"->"About & help";else->null}
  val target=aliases[label]?:if(label.startsWith("Touch comfort:"))"Touch comfort"else label
  fun matches(v:View,name:String)=v.isShown&&v.contentDescription==name&&v.hasOnClickListeners()
  if(views(a.window.decorView).any{matches(it,label)})return false
  val state=field(a,"screen")?.toString()
  if(state=="controller"&&Control.entries.any{it.title==label}){val directional=Control.entries.first{it.title==label} in listOf(Control.UP,Control.DOWN,Control.LEFT,Control.RIGHT);click(a,if(directional)"Directional buttons"else"Action buttons");click(a,label);return true}
  if(category==null)return false
  if(state !in listOf("settings","settings_category","appearance","reading_settings","about"))return false
  if(state !in listOf("settings","settings_category")){views(a.window.decorView).first{matches(it,"Back")}.performClick();settle(a)}
  if(field(a,"screen")=="settings_category"&&views(a.window.decorView).none{matches(it,target)}){views(a.window.decorView).first{matches(it,"Back")}.performClick();settle(a)}
  if(field(a,"screen")=="settings")click(a,category)
  if(label=="Backup and restore")return true
  if(views(a.window.decorView).none{matches(it,target)})return false
  click(a,target);return true
 }
 fun openTabs(a:BrowserActivity)=click(a,"Open tabs")
 fun newTab(a:BrowserActivity)=click(a,"New tab")
 fun focusAddress(a:BrowserActivity){BrowserActivity::class.java.getDeclaredMethod("focusChrome").apply{isAccessible=true}.invoke(a);settle(a)}
}
