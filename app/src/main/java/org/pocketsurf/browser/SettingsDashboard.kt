package org.pocketsurf.browser

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.*
import android.widget.*
import kotlin.math.*

/** One non-scrolling settings surface. Categories, editors and confirmations stay in place. */
class SettingsDashboard(
 context:Context,private val prefs:SharedPreferences,private val bindings:ControllerBindings,
 private val colors:BrowserTheme,
 private val bind:(View,Int,Int,()->Unit,Boolean)->Unit,
 private val forget:(View)->Unit,private val action:(String,String)->Unit,
 private val status:(String)->String,
 initialCategory:String="browsing",private val initialFocus:String?=null,private val makeBadge:((Control)->View)?=null
):LinearLayout(context){
 data class Node(val view:View,val id:String,val row:Int,val col:Int,val adjust:((Int)->Unit)?=null)
 companion object {val categories=linkedMapOf("browsing" to "Browsing","appearance" to "Appearance","controller" to "Controller","mouse" to "Mouse","privacy" to "Privacy","about" to "About")}
 var category=initialCategory.takeIf{it in categories}?:"browsing";private set
 private val rail=LinearLayout(context)
 private val pane=LinearLayout(context)
 private val preferredRowHeights = mutableMapOf<View, Int>()
 private val body = SettingsContent(context)
 private var group: LinearLayout? = null
 private val footer=LinearLayout(context)
 private val nodes=mutableListOf<Node>()
 private val remembered=mutableMapOf<String,String>()
 private var current:Node?=null
 private var activeSeekBar:SeekBar?=null
 private var editingSlider:String?=null
 private val sliderIds=mutableSetOf<String>()
 private val choiceIds=mutableSetOf<String>()
 private var leaveChoice:(()->Unit)?=null
 fun sliderEditing()=editingSlider!=null
 private fun endSlider(){leaveChoice?.invoke();leaveChoice=null;activeSeekBar?.progressTintList=ColorStateList.valueOf(colors.secondary);activeSeekBar?.thumbTintList=ColorStateList.valueOf(colors.secondary);activeSeekBar=null;editingSlider=null;action("slider_edit","")}
 private var building=false
 private var pending:(()->Unit)?=null
 private var recording:Control?=null
 private var listening=false
 private var candidate=96
 private var homeUrl=StartPageStore(prefs).homeUrl
 private var homeChoice=StartPageStore(prefs).homeMode
 private var test=false
 private var preview:FrameLayout?=null
 private var cursor:MousePointer?=null
 private var cursorX=0f;private var cursorY=0f
 private var help=false
 private fun d(n:Int)=(n*resources.displayMetrics.density).roundToInt()
 private val controlHeight=28
 private fun lp(w:Int=-1,h:Int=32)=LayoutParams(if(w<0)w else d(w),if(h<0)h else d(if(h==32)controlHeight else h))
 private fun label(s:String,size:Float=12f,color:Int=colors.text)=TextView(context).apply{text=s;textSize=if(size==12f&&controlHeight>=44)14f else size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL;includeFontPadding=false;maxLines=1;ellipsize=TextUtils.TruncateAt.END}
 private fun row()=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;isBaselineAligned=false}
 private fun surface()=GradientDrawable().apply{setColor(this@SettingsDashboard.colors.surface);cornerRadius=d(10).toFloat()}
 private val settingIcons = mapOf(
  "Search engine" to "search", "Start page" to "plus", "Bookmarks bar" to "bookmark",
  "Adblock" to "shield", "Automatic cookie rejection" to "cookie", "Cookie rejection on this site" to "cookie",
  "Clear history" to "history", "Clear site data" to "trash",
  "Help and controls" to "help", "Privacy notice" to "privacy", "Export backup" to "export",
  "Import backup" to "download", "Copy diagnostics" to "copy", "Restore defaults" to "reload",
  "Labels: Nintendo layout" to "snes-pad", "Labels: PlayStation" to "playstation"
 )
 private fun LinearLayout.addIcon(name: String) {
  addView(ImageView(context).apply {
   setImageDrawable(SurfIcon(name, colors.text))
   importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }, lp(16, 16).apply { rightMargin = d(6) })
 }
 private fun box()=row().apply{background=surface();setPadding(d(10),0,d(10),0)}
 private val rowGap = 4
 private fun panel(color: Int) = GradientDrawable().apply {
  setColor(color)
  cornerRadius = d(12).toFloat()
 }
 private fun settingHeading(title:String)=label(title,12f,colors.secondary).apply {
  tag="settings-heading";setPadding(d(3),d(1),d(3),d(1))
 }
 private fun groupPanel(title: String, description: String = "") = LinearLayout(context).apply {
  orientation=VERTICAL;background=panel(androidx.core.graphics.ColorUtils.blendARGB(colors.background,colors.surface,.55f))
  setPadding(d(8),d(8),d(8),d(8));tag="settings-group"
  if(title.isNotEmpty())addView(settingHeading(title),lp(-1,18))
 }
 private fun line(v: View, height: Int = 32) {
  val target = group ?: body
  val rowHeight = if (height == 32 && rowGap > 4 && category in listOf("mouse", "privacy")) 40 else height
  target.addView(v, lp(-1, rowHeight).apply {
   topMargin = d(if(target.childCount==0)0 else if(target.getChildAt(target.childCount-1).tag=="settings-heading")4 else if(category=="appearance")3 else 8)
  })
  if (height == 32) preferredRowHeights[v] = v.layoutParams.height
 }
 private fun sectionHeading(title: String, description: String) {
  group = groupPanel(title, description).also {
   body.addView(it, lp(-1, -2).apply { bottomMargin = d(if (rowGap > 4) 18 else if(category=="appearance")3 else 7) })
  }
 }
 private fun focus(id:String){if(!isAttachedToWindow)return;nodes.firstOrNull{it.id==id&&it.view.isShown&&it.view.isEnabled}?.view?.requestFocus()}
 private fun node(v:View,id:String,r:Int,c:Int=1,adjust:((Int)->Unit)?=null,selected:Boolean=false,click:()->Unit={}){
  val n=Node(v,id,r,c,adjust);nodes.add(n);v.contentDescription=id;bind(v,r,c,click,selected)
  val paint=v.onFocusChangeListener
  v.onFocusChangeListener=OnFocusChangeListener{w,has->paint?.onFocusChange(w,has);if(has){if(editingSlider!=null&&editingSlider!=id)endSlider();current=n;action("focus",id);if(c>0)remembered[category]=id;else if(!building&&category!=id.removePrefix("Category: "))select(id.removePrefix("Category: "),false)}}
 }
 init{
  orientation=HORIZONTAL;contentDescription="Settings dashboard";clipChildren=false
  rail.orientation=VERTICAL;rail.contentDescription="Settings categories";rail.setPadding(d(8),d(8),d(8),d(8));rail.background=panel(colors.surface)
  addView(rail,lp(if(resources.configuration.screenWidthDp<700)142 else 180,-1))
  pane.orientation=VERTICAL;pane.contentDescription="Settings detail pane";pane.setPadding(d(10),d(10),d(10),d(10));pane.background=panel(colors.surface);addView(pane,LayoutParams(0,-1,1f).apply{leftMargin=d(14)})
  body.orientation=VERTICAL;footer.orientation=HORIZONTAL;footer.gravity=Gravity.CENTER_VERTICAL
  building=true
  categories.entries.forEachIndexed{i,(key,title)->
   val v=box();v.addView(ImageView(context).apply{setImageDrawable(SurfIcon(listOf("globe","theme","controller","mouse","shield","info")[i],colors.text));importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO},lp(18,18).apply{rightMargin=d(10)})
   v.addView(label(title,13f),LayoutParams(0,-1,1f));node(v,"Category: $key",i,0,click={select(key,true)})
   rail.addView(v,LayoutParams(-1,0,1f).apply{if(i<5)bottomMargin=d(10)})
  };building=false;render();post{focus(initialFocus?:"Category: $category")}
 }
 fun select(key:String,enter:Boolean=false){if(key !in categories)return;endSlider();recording=null;pending=null;test=false;category=key;action("category",key);render();if(enter)enterPane()}
 private fun enterPane(){focus(remembered[category]?:nodes.firstOrNull{it.col>0&&it.row>=0}?.id?:return)}
 private fun render(restore:Boolean=false){
  val previous=current?.id;building=true;forget(pane);nodes.removeAll{it.col>0};pane.removeAllViews();body.removeAllViews();preferredRowHeights.clear();sliderIds.clear();choiceIds.clear();group=null;footer.removeAllViews();preview=null;cursor=null
  categories.keys.forEachIndexed{i,key->val v=rail.getChildAt(i) as ViewGroup;(v.getChildAt(1) as TextView).setTextColor(if(key==category)colors.accent else colors.text);v.contentDescription="Category: $key"}
  pane.addView(label(categories.getValue(category),19f).apply{typeface=android.graphics.Typeface.DEFAULT_BOLD;contentDescription="Settings section heading"},lp(-1,24).apply{bottomMargin=d(4)})
  pane.addView(body,LayoutParams(-1,0,1f));pane.addView(footer,lp(-1,if(rowGap>4)32 else 24).apply{topMargin=d(4)});footer.visibility=GONE
  if (category in listOf("mouse", "appearance") || (category == "privacy" && rowGap == 4)) sectionHeading("", "")
  when(category){"browsing"->browsing();"appearance"->appearance();"controller"->controller();"mouse"->mouse();"privacy"->privacy();else->about()}
  group?.let { (it.layoutParams as LayoutParams).bottomMargin = 0 }
  building=false;if(restore&&previous!=null)focus(previous)
 }
 private fun button(title:String,id:String,r:Int,c:Int=1,selected:Boolean=false,click:()->Unit):View=box().apply{gravity=Gravity.CENTER;if(title=="ⓘ")setPadding(0,0,0,0);settingIcons[id]?.let{addIcon(it)};addView(label(if(title=="Help and controls")"Help & controls"else title).apply{gravity=Gravity.CENTER},LayoutParams(-2,-1));node(this,id,r,c,selected=selected,click=click)}
 private fun buttons(items:List<Pair<String,()->Unit>>,r:Int){val v=row();v.gravity=Gravity.END;items.forEachIndexed{i,(title,run)->v.addView(button(title,title,r,i+1,click=run),LayoutParams(if(items.size==1)d(132)else 0,-1,if(items.size==1)0f else 1f).apply{if(i>0)leftMargin=d(6)})};line(v)}
 private fun choice(title:String,values:List<Pair<String,String>>,value:String,r:Int,change:(String)->Unit){
  val v=box();settingIcons[title]?.let{v.addIcon(it)};v.addView(label(title),LayoutParams(0,-1,1f));val result=label(values.firstOrNull{it.first==value}?.second?:value,12f,colors.accent).apply{gravity=Gravity.CENTER}
  fun step(delta:Int){val index=values.indexOfFirst{it.first==value}.coerceAtLeast(0);change(values[(index+delta+values.size)%values.size].first)}
  val arrows=mutableListOf<ImageView>()
  fun paintEditing(enabled:Boolean){result.setTextColor(if(enabled)colors.accent else colors.text);arrows.forEach{it.imageTintList=ColorStateList.valueOf(if(enabled)colors.accent else colors.secondary)}}
  fun beginEditing(){endSlider();editingSlider=title;current=nodes.firstOrNull{it.id==title};leaveChoice={paintEditing(false)};paintEditing(true);action("slider_edit",title)}
  fun arrow(s:String,delta:Int)=ImageView(context).apply{setImageDrawable(SurfIcon(if(delta<0)"back"else"forward",colors.text));scaleType=ImageView.ScaleType.CENTER_INSIDE;setPadding(d(6),d(6),d(6),d(6));isFocusable=false;contentDescription="$title ${if(delta<0)"previous"else"next"}";setOnClickListener{if(editingSlider!=title)beginEditing();step(delta)};arrows.add(this)}
  val selector=row();selector.addView(arrow("‹",-1),lp(28,-1));selector.addView(result,LayoutParams(0,-1,1f));selector.addView(arrow("›",1),lp(28,-1));v.addView(selector,lp(if(title in listOf("Search engine","Start page"))180 else 116,-1));choiceIds.add(title);node(v,title,r,adjust={step(it)},click={if(editingSlider==title)endSlider()else beginEditing()});paintEditing(editingSlider==title);if(editingSlider==title)leaveChoice={paintEditing(false)};line(v)
 }
 private fun toggle(title:String,value:Boolean,r:Int,change:(Boolean)->Unit){
  val v=box();settingIcons[title]?.let{v.addIcon(it)};v.addView(label(title),LayoutParams(0,-1,1f));val toggle=Switch(context).apply{isChecked=value;thumbTintList=ColorStateList.valueOf(if(value)colors.accent else colors.secondary);buttonTintList=ColorStateList.valueOf(colors.accent);isFocusable=false;contentDescription="$title switch";setOnCheckedChangeListener{_,on->change(on)}};v.addView(toggle,lp(-2,-1));node(v,title,r,click={toggle.isChecked=!toggle.isChecked});line(v)
 }
 private fun slider(title:String,value:Int,min:Int,max:Int,step:Int,r:Int,change:(Int)->Unit){
  val v=box();settingIcons[title]?.let{v.addIcon(it)};v.addView(label(title),LayoutParams(0,-1,1f));if(title=="Page zoom")v.addIcon("zoom-out");val number=label("$value%",11f,colors.secondary).apply{gravity=Gravity.END or Gravity.CENTER_VERTICAL};val slider=SeekBar(context).apply{contentDescription="$title slider";this.max=max-min;progress=value-min;keyProgressIncrement=step;isFocusable=false;progressTintList=ColorStateList.valueOf(colors.secondary);thumbTintList=ColorStateList.valueOf(colors.secondary)}
  slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(b:SeekBar,n:Int,user:Boolean){number.text="${n+min}%";change(n+min)};override fun onStartTrackingTouch(b:SeekBar){};override fun onStopTrackingTouch(b:SeekBar){}})
  v.addView(slider,LayoutParams(0,-1,1.4f));v.addView(number,lp(42,-1));if(title=="Page zoom"){v.addView(Space(context),lp(8,1));v.addIcon("zoom-in")};sliderIds.add(title);node(v,title,r,adjust={slider.progress=(slider.progress+it*step).coerceIn(0,slider.max)},click={if(editingSlider==title)endSlider()else{editingSlider=title;activeSeekBar=slider;slider.progressTintList=ColorStateList.valueOf(colors.accent);slider.thumbTintList=ColorStateList.valueOf(colors.accent);action("slider_edit",title)}})
  if(category in listOf("mouse","appearance")){val oldCaption=v.getChildAt(0);v.removeView(oldCaption);val caption=settingHeading(title);val stack=LinearLayout(context).apply{orientation=VERTICAL};stack.addView(caption,lp(-1,18));stack.addView(v,lp(-1,28).apply{topMargin=d(4)});line(stack,50);stack.layoutParams=stack.layoutParams.apply{height=d(18)+d(4)+d(28)}}else line(v)
 }
 private fun optionHeading(title:String){
  if(group==null)sectionHeading("", "")
  val target=group!!
  target.addView(settingHeading(title),lp(-1,18).apply{if(target.childCount>0)topMargin=d(4)})
 }
 private fun options(title:String,values:List<Pair<String,String>>,value:String,r:Int,change:(String)->Unit):LinearLayout {
  optionHeading(if(title=="Labels")"Controller labels" else title)
  val choices=row()
  values.forEachIndexed{i,(key,name)->choices.addView(button(name,"$title: $name",r,i+1,key==value){change(key)},LayoutParams(0,-1,1f).apply{if(i>0)leftMargin=d(5)})}
  line(choices,32)
  return choices
 }
 private fun edit(hint:String,value:String,r:Int,col:Int=1,save:(String)->Unit):EditText=EditText(context).apply{setText(value);setSingleLine(true);textSize=12f;setTextColor(colors.text);setHintTextColor(colors.secondary);this.hint=hint;setPadding(d(10),0,d(10),0);background=surface();imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_DONE or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN;node(this,hint,r,col,click={action("edit",hint)});addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){save(s.toString())};override fun afterTextChanged(s:android.text.Editable?){}})}
 private fun browsing(){
  sectionHeading("Browsing", "")
  val store=StartPageStore(prefs);choice("Start page",listOf("pocket" to "PocketSurf","blank" to "Blank","custom" to "Website"),homeChoice,2){homeChoice=it;if(it!="custom")store.homeMode=it;render(true)}
  if(homeChoice=="custom"){val v=row();v.addView(edit("Start page address",homeUrl,3){homeUrl=it},LayoutParams(0,-1,1f));v.addView(button("Save","Save start page",3,2){try{store.homeUrl=if(homeUrl.contains("://"))homeUrl else "https://$homeUrl";store.homeMode="custom";action("hide_keyboard","");message("Start page saved")}catch(e:Exception){message("Enter an HTTP or HTTPS address")}},lp(54,-1).apply{leftMargin=d(6)});line(v,32)}
  val defaults=row();defaults.addView(button("Default browser","Default browser",4){action("default","")},LayoutParams(-1,-1));line(defaults)
  slider("Page zoom",prefs.getInt("page_zoom",100),50,250,5,5){action("zoom",it.toString())}
  sectionHeading("Bookmarks", "")
  val bookmarks=row();val bookmarkToggle=box();bookmarkToggle.addIcon("bookmark");bookmarkToggle.addView(label("Bookmarks bar"),LayoutParams(0,-1,1f));val enabled=Switch(context).apply{isChecked=prefs.getBoolean("favorites_bar",true);isFocusable=false;thumbTintList=ColorStateList.valueOf(colors.accent);contentDescription="Bookmarks bar switch";setOnCheckedChangeListener{_,on->prefs.edit().putBoolean("favorites_bar",on).apply()}};bookmarkToggle.addView(enabled,lp(-2,-1));node(bookmarkToggle,"Show bookmarks bar while browsing",6,1,click={enabled.isChecked=!enabled.isChecked});bookmarks.addView(bookmarkToggle,LayoutParams(0,-1,1f));bookmarks.addView(button("Clear bookmarks","Clear bookmarks",6,2){confirmInline("Remove all saved bookmarks and pins? Browsing history stays.",{action("clear_bookmarks","");message("Bookmarks cleared")})},LayoutParams(0,-1,1f).apply{leftMargin=d(6)});line(bookmarks)


 }
 private fun appearance(){
  optionHeading("Theme")
  val themes = row()
  BrowserTheme.names.forEachIndexed { index, name ->
   val choice = LinearLayout(context).apply {
    orientation = VERTICAL
    gravity = Gravity.CENTER
    setPadding(d(6), d(4), d(6), d(4))
    addView(ThemePreview(context, name, prefs.getString("accent", "Blue") ?: "Blue"), LayoutParams(-1, 0, 1f))
    addView(label(name, 11f).apply { gravity = Gravity.CENTER }, lp(-1, 18))
   }
   node(choice, "Theme: $name", 0, index + 1,
    selected = BrowserTheme.baseName(prefs.getString("theme", "OLED")!!) == name,
    click = { prefs.edit().putString("theme", name).apply(); action("theme", "") })
   themes.addView(choice, LayoutParams(0, -1, 1f).apply { if (index > 0) leftMargin = d(6) })
  }
  line(themes, if (rowGap > 4) 88 else 40)
  optionHeading("Accent colors")
  BrowserTheme.accentNames.chunked(13).forEachIndexed { rowIndex, names ->
   val swatches = AccentRow(context)
   names.forEachIndexed { column, name ->
    val swatch = FrameLayout(context)
    swatch.addView(object:View(context){
     private val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=BrowserTheme.accentColor(name,Color.luminance(this@SettingsDashboard.colors.background)<.5)}
     override fun onDraw(canvas:android.graphics.Canvas){canvas.drawCircle(width/2f,height/2f,minOf(width,height)*.36f,paint)}
    },FrameLayout.LayoutParams(-1,-1))
    node(swatch, "Accent: $name", 1 + rowIndex, column + 1,
     selected = prefs.getString("accent", "Blue") == name,
     click = { prefs.edit().putString("accent", name).apply(); action("theme", "") })
    swatches.addView(swatch, LayoutParams(0, -1, 1f).apply { if (column > 0) leftMargin = d(5) })
   }
   line(swatches, if(rowGap>4)48 else 26)
  }
  sectionHeading("", "")
  slider("Website text size",prefs.getInt("default_text_zoom",100),75,200,5,3){prefs.edit().putInt("default_text_zoom",it).apply();action("text","")}
 }
 private fun columns(){
  if(body.childCount<2)return
  val children=(0 until body.childCount).map{body.getChildAt(it)}
  body.removeAllViews();val horizontal=row().apply{gravity=Gravity.TOP}
  children.forEachIndexed{i,v->horizontal.addView(v,LayoutParams(0,-2,1f).apply{if(i>0)leftMargin=d(12)})}
  body.addView(horizontal,lp(-1,-2))
 }
 private fun controller(){
  val labels = options("Labels",listOf("snes" to "Nintendo layout","ps" to "PlayStation"),if(bindings.playStation)"ps"else"snes",0){bindings.playStation=it=="ps";render(true);action("labels","")}
  labels.addView(button("Reset", "Restore defaults", 0, 3) {
   confirmInline("Restore controller defaults?", {
    bindings.reset()
    bindings.playStation = false
    render()
    action("labels", "")
   })
  }, lp(84, -1).apply { leftMargin = d(6) })
  group?.let{(it.layoutParams as LayoutParams).bottomMargin=0}
  val groups = listOf(
   "Face buttons" to listOf(Control.CONFIRM, Control.BACK, Control.TABS, Control.CONTEXT),
   "Navigation" to listOf(Control.HISTORY_BACK, Control.HISTORY_FORWARD, Control.PREVIOUS_TAB, Control.NEXT_TAB),
   "Sticks / Sys." to listOf(Control.POINTER, Control.ZOOM, Control.SEARCH, Control.MENU),
   "D-pad" to listOf(Control.UP, Control.DOWN, Control.LEFT, Control.RIGHT)
  )
  groups.chunked(4).forEachIndexed { band, pair ->
   val groupRow = row().apply { gravity = Gravity.TOP }
   pair.forEachIndexed { side, (title, controls) ->
    val card = groupPanel(title)
    controls.chunked(1).forEachIndexed { offset, actions ->
     val controlRow = row()
     actions.forEachIndexed { index, control ->
      val caption = when (control) {
       Control.CONFIRM -> "Open"
       Control.BACK -> "Back"
       Control.POINTER -> "Mouse"
       Control.ZOOM -> "Zoom"
       Control.HISTORY_BACK -> "Back"
       Control.HISTORY_FORWARD -> "Forward"
       Control.PREVIOUS_TAB -> "Prev tab"
       else -> control.title.removePrefix("Move ").replaceFirstChar { it.uppercase() }
      }
      val cell = box().apply { setPadding(d(8),0,d(8),0) }
      cell.addView(label(caption, if(rowGap>4)12f else 10f), LayoutParams(0, -1, 1f))
      val direction = when (bindings.label(control)) {
       "↑" -> "up"; "↓" -> "down"; "←" -> "left"; "→" -> "right"; else -> null
      }
      val badge = if (direction == null) ControllerBadge.create(context,bindings.label(control),if(Color.luminance(colors.background)>.5)Color.BLACK else Color.WHITE,if(Color.luminance(colors.background)>.5)Color.WHITE else Color.BLACK)
       else ImageView(context).apply { setImageDrawable(SurfIcon("dpad-$direction", colors.text)) }
      cell.addView(BadgePreview(context,badge), lp(if (bindings.label(control) in listOf("Start", "Select")) 34 else 24, 24).apply { leftMargin=d(6) })
      node(cell, control.title, offset + 1, side + 1, click = {
       recording = control
       listening = false
       candidate = bindings.key(control)
       renderAssignment()
       focus("Choose physical button")
      })
      controlRow.addView(cell, LayoutParams(0, -1, 1f).apply { if (index > 0) leftMargin = d(4) })
     }
     card.addView(controlRow, lp(-1, if (rowGap > 4) 44 else 24).apply { topMargin = d(4) })
    }
    groupRow.addView(card, LayoutParams(0, -2, 1f).apply { if (side > 0) leftMargin = d(8) })
   }
   body.addView(groupRow, lp(-1, -2).apply {
    topMargin = d(if (rowGap > 4) 16 else if (band == 0) 6 else 4)
   })
  }
 }
 private fun mouse(){
  if (test) {
   mouseTestSurface()
   return
  }
  slider("Movement speed",(prefs.getInt("mouse_speed",600)/6f).roundToInt(),50,300,5,0){prefs.edit().putInt("mouse_speed",it*6).apply()}
  slider("Pointer size",prefs.getInt("pointer_size",100),50,220,5,1){prefs.edit().putInt("pointer_size",it).apply();refreshCursor();action("pointer","")}
  val accelerationGroup = group ?: body
  toggle("Mouse acceleration",prefs.getBoolean("mouse_acceleration",false),2){prefs.edit().putBoolean("mouse_acceleration",it).apply()}
  val acceleration = accelerationGroup.getChildAt(accelerationGroup.childCount - 1)
  val accelerationParams = acceleration.layoutParams as LayoutParams
  accelerationGroup.removeView(acceleration)
  val pair = row()
  pair.orientation=VERTICAL
  pair.addView(acceleration, lp(-1,28))
  pair.addView(button("Test mouse", "Test mouse navigation", 3, 1) { toggleMouseTest() },
   lp(-1,28).apply { topMargin=d(8) })
  accelerationParams.height=d(64)
  accelerationGroup.addView(pair, accelerationParams)
  preferredRowHeights.remove(acceleration)
  preferredRowHeights[pair] = accelerationParams.height

  sectionHeading("", "")
  optionHeading("Mouse style")
  val shapes=row()
  MousePointer.styles.entries.forEachIndexed{i,e->
   val choice=LinearLayout(context).apply{orientation=VERTICAL;gravity=Gravity.CENTER;setPadding(d(4),d(6),d(4),d(6));addView(MousePointer(context,e.key),LayoutParams(d(28),0,1f).apply{gravity=Gravity.CENTER_HORIZONTAL});addView(label(e.value,10f).apply{gravity=Gravity.CENTER},lp(-1,18));tooltipText=e.value}
   node(choice,"Pointer style: ${e.value}",4,i+1,selected=MousePointer.form(prefs)==e.key,click={
    val fill=MousePointer.fill(prefs);val outline=MousePointer.outline(prefs)
    prefs.edit().putString("pointer_style",e.key).putString("pointer_fill",fill).putString("pointer_outline",outline).apply()
    render(true);action("pointer","")
   })
   shapes.addView(choice,LayoutParams(0,-1,1f).apply{if(i>0)leftMargin=d(5)})
  }
  line(shapes,62)

  val appearanceGroup = group ?: body
  val firstChoice = appearanceGroup.childCount
  choice("Fill", listOf("white" to "White", "black" to "Black", "accent" to "Accent"), MousePointer.fill(prefs), 5) {
   val outline = MousePointer.outline(prefs)
   prefs.edit().putString("pointer_fill", it).putString("pointer_outline", outline).apply()
   render(true);action("pointer", "")
  }
  choice("Outline", listOf("none" to "None", "white" to "White", "black" to "Black"), MousePointer.outline(prefs), 6) {
   prefs.edit().putString("pointer_outline", it).apply()
   render(true);action("pointer", "")
  }
  val choices = LinearLayout(context).apply { orientation = VERTICAL }
  repeat(2) {
   val choice = appearanceGroup.getChildAt(firstChoice)
   appearanceGroup.removeView(choice)
   choices.addView(choice)
  }
  val appearance = row()
  appearance.orientation=VERTICAL
  appearance.addView(choices, LayoutParams(-1,-2))
  val area = FrameLayout(context).apply {
   contentDescription = "Mouse test area"
   background = surface()
   clipChildren = true
  }
  preview = area
  cursor = MousePointer(context).also { area.addView(it, FrameLayout.LayoutParams(d(18), d(23), Gravity.CENTER)) }
  appearance.addView(area, LayoutParams(-1,d(36)).apply { topMargin=d(8) })
  appearanceGroup.addView(appearance, lp(-1,-2))
  refreshCursor()
  columns()
 }
 private fun mouseTestSurface() {
  group?.let { body.removeView(it) }
  group = null
  val area = FrameLayout(context).apply {
   contentDescription = "Mouse test area"
   background = surface()
   clipChildren = true
   addView(object : View(context) {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: android.graphics.Canvas) {
     paint.color = colors.divider
     paint.strokeWidth = d(1).toFloat()
     for (column in 1..5) {
      val x = width * column / 6f
      canvas.drawLine(x, 0f, x, height.toFloat(), paint)
     }
     for (row in 1..3) {
      val y = height * row / 4f
      canvas.drawLine(0f, y, width.toFloat(), y, paint)
     }
     paint.style = android.graphics.Paint.Style.STROKE
     paint.color = colors.accent
     for (column in 1..3) for (row in 1..2)
      canvas.drawCircle(width * column / 4f, height * row / 3f, d(8).toFloat(), paint)
    }
   }, FrameLayout.LayoutParams(-1,-1))
  }
  preview = area
  cursor = MousePointer(context).also { area.addView(it) }
  body.addView(area, LayoutParams(-1,0,1f))
  val controls = row()
  controls.addView(label("Move the left stick to test speed and precision.",11f,colors.secondary),LayoutParams(0,-1,1f))
  controls.addView(button("Stop test", "Test mouse navigation", 0, 1) { toggleMouseTest() },lp(104,-1))
  body.addView(controls,lp(-1,32).apply{topMargin=d(8)})
  refreshCursor()
  area.post {
   cursorX = (area.width - (cursor?.width ?: 0)) / 2f
   cursorY = (area.height - (cursor?.height ?: 0)) / 2f
   movePointer(0f,0f)
  }
 }
 private fun refreshCursor(){
  cursor?.apply{
   val size=prefs.getInt("pointer_size",100)
   layoutParams=FrameLayout.LayoutParams(d((10*size/100f).roundToInt()),d((14*size/100f).roundToInt()),if(test)Gravity.TOP or Gravity.START else Gravity.CENTER)
   invalidate()
  }
 }
 fun toggleMouseTest(){
  if(category!="mouse")return
  test=!test
  action("mouse_test",test.toString())
  render()
  focus("Test mouse navigation")
 }

 fun mouseTesting()=category=="mouse"&&test
 fun movePointer(dx:Float,dy:Float){val p=preview?:return;val v=cursor?:return;if(!test)return;cursorX=(cursorX+dx).coerceIn(0f,max(0f,p.width-v.width.toFloat()));cursorY=(cursorY+dy).coerceIn(0f,max(0f,p.height-v.height.toFloat()));v.translationX=cursorX;v.translationY=cursorY}
 private fun privacy(){
  if (rowGap > 4) sectionHeading("Website protection", "Manage ads and supported cookie banners")
  toggle("Adblock",status("adblock")=="true",0){action("adblock",it.toString())}
  toggle("Automatic cookie rejection",status("cookies")=="true",1){action("cookies",it.toString())}
  if(status("site").isNotBlank())toggle("Cookie rejection on this site",status("site_cookie")=="true",2){action("site_cookie",it.toString())}

  if (rowGap > 4) sectionHeading("Browsing data", "Remove data stored on this device")
  buttons(listOf("Clear history" to {confirmInline("Remove saved browsing history?",{action("clear_history","");message("History cleared")})},"Clear site data" to {confirmInline("Sign out and clear website data?",{action("clear_data","");message("Website data cleared")})}),4)
 }
 private fun about(){
  (pane.getChildAt(0) as TextView).apply {
   text="PocketSurf ${BuildConfig.VERSION_NAME} · About"
   setCompoundDrawablesRelative(SurfIcon("logo",colors.text).apply{setBounds(0,0,d(24),d(24))},null,null,null)
   compoundDrawablePadding=d(8)
  }
  sectionHeading("Project & support", "Source code, updates and support")
  val links = row()
  listOf(Triple("GitHub", "github", "Project source"), Triple("Ko-fi", "kofi", "Support PocketSurf"),
   Triple("Release notes", "info", "What’s new")).forEachIndexed { index, (title, glyph, detail) ->
   val link = box()
   link.addView(ImageView(context).apply { setImageDrawable(SurfIcon(glyph, colors.text)) },
    lp(18, 18).apply { rightMargin = d(8) })
   val copy = LinearLayout(context).apply {
    orientation = VERTICAL
    gravity = Gravity.CENTER_VERTICAL
    addView(label(title))
    addView(label(detail, 10f, colors.secondary))
   }
   link.addView(copy, LayoutParams(0, -1, 1f))
   node(link, title, 0, index + 1, click = { action(title, "") })
   links.addView(link, LayoutParams(0, -1, 1f).apply { if (index > 0) leftMargin = d(8) })
  }
  line(links)
  sectionHeading("Help & information", "Learn the controls and privacy options")
  buttons(listOf(
   "Help and controls" to { help = !help; render(true) },
   "Privacy notice" to { action("Privacy notice", "") }), 1)
  sectionHeading("Backup & diagnostics", "Save preferences or collect troubleshooting details")
  buttons(listOf("Export backup" to { action("Export backup", "") },
   "Import backup" to { action("import", "") },
   "Copy diagnostics" to { action("Copy diagnostics", "") }), 2)
  if (help) message("D-pad / left stick: move. A: select. B: back. L3: test mouse.")
 }
 private fun message(s:String){footer.visibility=VISIBLE;pending=null;recording=null;forget(footer);nodes.removeAll{it.row>=90};footer.removeAllViews();footer.addView(label(s,10f,colors.secondary).apply{maxLines=2},LayoutParams(0,-1,1f))}
 fun notify(s:String){if(pending==null&&recording==null)message(s)}
 fun confirmInline(s:String,yes:()->Unit){message(s);pending=yes;footer.addView(button("Confirm","Confirm setting action",90,1){pending=null;yes()},lp(74,-1).apply{leftMargin=d(6)});footer.addView(button("Cancel","Cancel setting action",90,2){message("No changes made");enterPane()},lp(64,-1).apply{leftMargin=d(6)});focus("Cancel setting action")}
 private fun renderAssignment(){footer.visibility=VISIBLE;val a=recording?:return;forget(footer);nodes.removeAll{it.row>=90};footer.removeAllViews();footer.addView(label(if(listening)"Press a button…"else a.title,10f,colors.secondary).apply{maxLines=2},LayoutParams(0,-1,1f));val keys=(Control.entries.map{bindings.key(it)}+Control.entries.map{it.defaultKey}+(96..110)+(188..203)+(19..23)).distinct();fun step(delta:Int){candidate=keys[(keys.indexOf(candidate).coerceAtLeast(0)+delta+keys.size)%keys.size];renderAssignment();focus("Choose physical button")};val value=button("‹ ${bindings.labelForKey(candidate)} ›","Choose physical button",90,1){step(1)};nodeAdjust(value){step(it)};footer.addView(value,lp(82,-1));footer.addView(button("Record","Record a button",90,2){listening=true;renderAssignment()},lp(54,-1).apply{leftMargin=d(6)});footer.addView(button("Apply","Apply button mapping",90,3){commitAssignment(a,candidate)},lp(54,-1).apply{leftMargin=d(6)});footer.addView(button("Cancel","Cancel button mapping",90,4){recording=null;message("Mapping unchanged");focus(a.title)},lp(58,-1).apply{leftMargin=d(6)})}
 private fun nodeAdjust(view:View,adjust:(Int)->Unit){val at=nodes.indexOfFirst{it.view===view};if(at>=0)nodes[at]=nodes[at].copy(adjust=adjust)}
 private fun commitAssignment(a:Control,key:Int){val occupied=bindings.action(key)?.takeIf{it!=a};val run={bindings.assign(a,key);recording=null;render();focus(a.title);action("labels","")};if(occupied==null)run()else confirmInline("Swap with ${occupied.title}?",run)}
 fun recordKey(e:KeyEvent):Boolean{val a=recording?:return false;if(!listening)return false;if(e.action==KeyEvent.ACTION_DOWN&&e.repeatCount==0){if(e.keyCode==KeyEvent.KEYCODE_BACK||e.keyCode==bindings.key(Control.BACK)){recording=null;message("Mapping unchanged");focus(a.title)}else if(e.keyCode in ((96..110)+(188..203)+(19..23)))commitAssignment(a,e.keyCode)};return true}
 fun back():Boolean{if(editingSlider!=null){endSlider();return true};if(test){toggleMouseTest();return true};if(recording!=null||pending!=null){recording=null;message("No changes made");enterPane();return true};if(current?.col!=0){focus("Category: $category");return true};return false}
 fun move(direction:String){
  if(test)return
  if(!hasFocus()) {
   if(direction=="down"||direction=="right")focus("Category: $category")
   return
  }
  val n=nodes.firstOrNull{it.view.hasFocus()}?:current?.let{old->nodes.firstOrNull{it.view===old.view}}?:nodes.firstOrNull{it.id=="Category: $category"}?:return
  current=n
  if(n.col==0){when(direction){"up","down"->{if(direction=="up"&&n.row==0){action("focus_header","");return};val delta=if(direction=="up")-1 else 1;focus("Category: ${categories.keys.elementAt((n.row+delta).coerceIn(0,5))}")};"right"->enterPane()};return}
  if(editingSlider!=null){if(n.id==editingSlider&&direction in listOf("left","right")){n.adjust?.invoke(if(direction=="left")-1 else 1);return}else endSlider()}
  if(direction in listOf("left","right")&&n.adjust!=null&&n.id !in sliderIds&&n.id !in choiceIds){n.adjust.invoke(if(direction=="left")-1 else 1);return}
  if(category=="mouse"&&direction=="right"&&n.id in listOf("Movement speed","Pointer size","Mouse acceleration","Test mouse navigation")){focus(when(n.id){"Pointer size"->"Fill";"Mouse acceleration"->"Outline";else->"Pointer style: ${MousePointer.styles[MousePointer.form(prefs)]}"});return}
  if(category=="mouse"&&direction=="left"&&n.id.startsWith("Pointer style:")&&n.col==1){focus("Movement speed");return}
  val candidates=nodes.filter{it.col>0&&it.view.isShown&&it.view.isEnabled&&it!==n}
  val next=when(direction){"left"->candidates.filter{it.row==n.row&&it.col<n.col}.maxByOrNull{it.col};"right"->candidates.filter{it.row==n.row&&it.col>n.col}.minByOrNull{it.col};"up"->candidates.filter{it.row<n.row}.minByOrNull{(n.row-it.row)*10+abs(it.col-n.col)};else->candidates.filter{it.row>n.row}.minByOrNull{(it.row-n.row)*10+abs(it.col-n.col)}}
  if(next!=null)next.view.requestFocus()else if(direction=="left")focus("Category: $category")
 }
}


