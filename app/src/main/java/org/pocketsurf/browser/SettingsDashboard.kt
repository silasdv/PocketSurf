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
 private val body=LinearLayout(context)
 private val footer=LinearLayout(context)
 private val nodes=mutableListOf<Node>()
 private val remembered=mutableMapOf<String,String>()
 private var current:Node?=null
 private var building=false
 private var pending:(()->Unit)?=null
 private var recording:Control?=null
 private var listening=false
 private var candidate=96
 private var customEngine=SearchEngines.resolve(prefs).id=="custom"
 private var searchName=if(customEngine)SearchEngines.resolve(prefs).name else ""
 private var searchUrl=if(customEngine)SearchEngines.resolve(prefs).template else ""
 private var homeUrl=StartPageStore(prefs).homeUrl
 private var homeChoice=StartPageStore(prefs).homeMode
 private var test=false
 private var preview:FrameLayout?=null
 private var cursor:MousePointer?=null
 private var cursorX=0f;private var cursorY=0f
 private var help=false
 private fun d(n:Int)=(n*resources.displayMetrics.density).roundToInt()
 private val controlHeight=if(resources.configuration.screenHeightDp>=480)44 else 32
 private fun lp(w:Int=-1,h:Int=32)=LayoutParams(if(w<0)w else d(w),if(h<0)h else d(if(h==32)controlHeight else h))
 private fun label(s:String,size:Float=12f,color:Int=colors.text)=TextView(context).apply{text=s;textSize=if(size==12f&&controlHeight==44)14f else size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL;includeFontPadding=false;maxLines=1;ellipsize=TextUtils.TruncateAt.END}
 private fun row()=LinearLayout(context).apply{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;isBaselineAligned=false}
 private fun surface()=GradientDrawable().apply{setColor(this@SettingsDashboard.colors.surface);cornerRadius=d(10).toFloat()}
 private fun box()=row().apply{background=surface();setPadding(d(10),0,d(10),0)}
 private fun line(v:View,height:Int=32){body.addView(v,lp(-1,height).apply{bottomMargin=d(4)})}
 private fun focus(id:String){if(!isAttachedToWindow)return;nodes.firstOrNull{it.id==id&&it.view.isShown&&it.view.isEnabled}?.view?.requestFocus()}
 private fun node(v:View,id:String,r:Int,c:Int=1,adjust:((Int)->Unit)?=null,selected:Boolean=false,click:()->Unit={}){
  val n=Node(v,id,r,c,adjust);nodes.add(n);v.contentDescription=id;bind(v,r,c,click,selected)
  val paint=v.onFocusChangeListener
  v.onFocusChangeListener=OnFocusChangeListener{w,has->paint?.onFocusChange(w,has);if(has){current=n;action("focus",id);if(c>0)remembered[category]=id;else if(!building&&category!=id.removePrefix("Category: "))select(id.removePrefix("Category: "),false)}}
 }
 init{
  orientation=HORIZONTAL;contentDescription="Settings dashboard";clipChildren=false
  rail.orientation=VERTICAL;rail.contentDescription="Settings categories";rail.setPadding(0,0,d(12),0)
  addView(rail,lp(if(resources.configuration.screenWidthDp<700)142 else 180,-1))
  pane.orientation=VERTICAL;pane.contentDescription="Settings detail pane";pane.setPadding(d(12),0,0,0);addView(pane,LayoutParams(0,-1,1f))
  body.orientation=VERTICAL;footer.orientation=HORIZONTAL;footer.gravity=Gravity.CENTER_VERTICAL
  building=true
  categories.entries.forEachIndexed{i,(key,title)->
   val v=box();v.addView(ImageView(context).apply{setImageDrawable(SurfIcon(listOf("globe","theme","controller","mouse","shield","info")[i],colors.text));importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO},lp(18,18).apply{rightMargin=d(10)})
   v.addView(label(title,13f),LayoutParams(0,-1,1f));node(v,"Category: $key",i,0,click={select(key,true)})
   rail.addView(v,lp(-1,32).apply{if(i<5)bottomMargin=d(4)})
  };building=false;render();post{focus(initialFocus?:"Category: $category")}
 }
 fun select(key:String,enter:Boolean=false){if(key !in categories)return;recording=null;pending=null;test=false;category=key;action("category",key);render();if(enter)enterPane()}
 private fun enterPane(){focus(remembered[category]?:nodes.firstOrNull{it.col>0&&it.row>=0}?.id?:return)}
 private fun render(restore:Boolean=false){
  val previous=current?.id;building=true;forget(pane);nodes.removeAll{it.col>0};pane.removeAllViews();body.removeAllViews();footer.removeAllViews();preview=null;cursor=null
  categories.keys.forEachIndexed{i,key->val v=rail.getChildAt(i) as ViewGroup;(v.getChildAt(1) as TextView).setTextColor(if(key==category)colors.accent else colors.text);v.contentDescription="Category: $key"}
  pane.addView(label(categories.getValue(category),19f).apply{typeface=android.graphics.Typeface.DEFAULT_BOLD;contentDescription="Settings section heading"},lp(-1,24).apply{bottomMargin=d(4)})
  pane.addView(body,LayoutParams(-1,0,1f));pane.addView(footer,lp(-1,32).apply{topMargin=d(4)})
  when(category){"browsing"->browsing();"appearance"->appearance();"controller"->controller();"mouse"->mouse();"privacy"->privacy();else->about()}
  message(if(category=="about")"PocketSurf ${BuildConfig.VERSION_NAME} · SilasDV · MIT"else"Changes are saved automatically")
  building=false;if(restore&&previous!=null)focus(previous)
 }
 private fun button(title:String,id:String,r:Int,c:Int=1,selected:Boolean=false,click:()->Unit):View=box().apply{gravity=Gravity.CENTER;addView(label(title).apply{gravity=Gravity.CENTER},lp(-1,-1));node(this,id,r,c,selected=selected,click=click)}
 private fun buttons(items:List<Pair<String,()->Unit>>,r:Int){val v=row();v.gravity=Gravity.END;items.forEachIndexed{i,(title,run)->v.addView(button(title,title,r,i+1,click=run),LayoutParams(if(items.size==1)d(132)else 0,-1,if(items.size==1)0f else 1f).apply{if(i>0)leftMargin=d(6)})};line(v)}
 private fun choice(title:String,values:List<Pair<String,String>>,value:String,r:Int,change:(String)->Unit){
  val v=box();v.addView(label(title),LayoutParams(0,-1,1f));val result=label(values.firstOrNull{it.first==value}?.second?:value,12f,colors.accent).apply{gravity=Gravity.CENTER}
  fun step(delta:Int){val index=values.indexOfFirst{it.first==value}.coerceAtLeast(0);change(values[(index+delta+values.size)%values.size].first)}
  fun arrow(s:String,delta:Int)=label(s,18f).apply{gravity=Gravity.CENTER;isFocusable=false;contentDescription="$title ${if(delta<0)"previous"else"next"}";setOnClickListener{step(delta)}}
  v.addView(arrow("‹",-1),lp(28,-1));v.addView(result,LayoutParams(0,-1,1.2f));v.addView(arrow("›",1),lp(28,-1));node(v,title,r,adjust={step(it)},click={step(1)});line(v)
 }
 private fun toggle(title:String,value:Boolean,r:Int,change:(Boolean)->Unit){
  val v=box();v.addView(label(title),LayoutParams(0,-1,1f));val toggle=Switch(context).apply{isChecked=value;thumbTintList=ColorStateList.valueOf(if(value)colors.accent else colors.secondary);buttonTintList=ColorStateList.valueOf(colors.accent);isFocusable=false;contentDescription="$title switch";setOnCheckedChangeListener{_,on->change(on)}};v.addView(toggle,lp(-2,-1));node(v,title,r,click={toggle.isChecked=!toggle.isChecked});line(v)
 }
 private fun slider(title:String,value:Int,min:Int,max:Int,step:Int,r:Int,change:(Int)->Unit){
  val v=box();v.addView(label(title),LayoutParams(0,-1,1f));val number=label("$value%",11f,colors.secondary).apply{gravity=Gravity.END or Gravity.CENTER_VERTICAL};val slider=SeekBar(context).apply{contentDescription="$title slider";this.max=max-min;progress=value-min;keyProgressIncrement=step;isFocusable=false;progressTintList=ColorStateList.valueOf(colors.accent);thumbTintList=ColorStateList.valueOf(colors.accent)}
  slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(b:SeekBar,n:Int,user:Boolean){number.text="${n+min}%";change(n+min)};override fun onStartTrackingTouch(b:SeekBar){};override fun onStopTrackingTouch(b:SeekBar){}})
  v.addView(slider,LayoutParams(0,-1,1.4f));v.addView(number,lp(42,-1));node(v,title,r,adjust={slider.progress=(slider.progress+it*step).coerceIn(0,slider.max)},click={});line(v)
 }
 private fun options(title:String,values:List<Pair<String,String>>,value:String,r:Int,change:(String)->Unit){val v=row();v.addView(label(title,12f,colors.secondary),lp(65,-1));values.forEachIndexed{i,(key,name)->v.addView(button(name,"$title: $name",r,i+1,key==value){change(key)},LayoutParams(0,-1,1f).apply{if(i>0)leftMargin=d(5)})};line(v,32)}
 private fun edit(hint:String,value:String,r:Int,col:Int=1,save:(String)->Unit):EditText=EditText(context).apply{setText(value);setSingleLine(true);textSize=12f;setTextColor(colors.text);setHintTextColor(colors.secondary);this.hint=hint;setPadding(d(10),0,d(10),0);background=surface();imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_DONE or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN;node(this,hint,r,col,click={action("edit",hint)});addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){save(s.toString())};override fun afterTextChanged(s:android.text.Editable?){}})}
 private fun browsing(){
  val engines=SearchEngines.entries.map{it.id to it.name}+listOf("custom" to "Custom")
  choice("Search engine",engines,if(customEngine)"custom"else SearchEngines.resolve(prefs).id,0){id->customEngine=id=="custom";if(!customEngine)SearchEngines.select(prefs,SearchEngines.entries.first{it.id==id});render(true)}
  if(customEngine){val v=row();v.addView(edit("Engine name",searchName,1){searchName=it},LayoutParams(0,-1,.8f));v.addView(edit("URL with {query}",searchUrl,1,2){searchUrl=it},LayoutParams(0,-1,1.4f).apply{leftMargin=d(6)});v.addView(button("Save","Save search engine",1,3){try{SearchEngines.select(prefs,SearchEngines.custom(searchName,searchUrl));action("hide_keyboard","");message("Search engine saved")}catch(e:Exception){message(e.message?:"Check the search template")}},lp(54,-1).apply{leftMargin=d(6)});line(v,32)}
  val store=StartPageStore(prefs);choice("Start page",listOf("pocket" to "PocketSurf","blank" to "Blank","custom" to "Website"),homeChoice,2){homeChoice=it;if(it!="custom")store.homeMode=it;render(true)}
  if(homeChoice=="custom"){val v=row();v.addView(edit("Start page address",homeUrl,3){homeUrl=it},LayoutParams(0,-1,1f));v.addView(button("Save","Save start page",3,2){try{store.homeUrl=if(homeUrl.contains("://"))homeUrl else "https://$homeUrl";store.homeMode="custom";action("hide_keyboard","");message("Start page saved")}catch(e:Exception){message("Enter an HTTP or HTTPS address")}},lp(54,-1).apply{leftMargin=d(6)});line(v,32)}
  val bookmarks=row();val bookmarkToggle=box();bookmarkToggle.addView(label("Bookmarks bar"),LayoutParams(0,-1,1f));val enabled=Switch(context).apply{isChecked=prefs.getBoolean("favorites_bar",false);isFocusable=false;thumbTintList=ColorStateList.valueOf(colors.accent);contentDescription="Bookmarks bar switch";setOnCheckedChangeListener{_,on->prefs.edit().putBoolean("favorites_bar",on).apply()}};bookmarkToggle.addView(enabled,lp(-2,-1));node(bookmarkToggle,"Show bookmarks bar while browsing",4,1,click={enabled.isChecked=!enabled.isChecked});bookmarks.addView(bookmarkToggle,LayoutParams(0,-1,1f));bookmarks.addView(button("Default browser","Default browser",4,2){action("default","")},LayoutParams(0,-1,1f).apply{leftMargin=d(6)});line(bookmarks)
  slider("Page zoom",prefs.getInt("page_zoom",100),50,250,5,5){action("zoom",it.toString())}

 }
 private fun appearance(){
  options("Theme",BrowserTheme.names.map{it to it},BrowserTheme.baseName(prefs.getString("theme","Dark")!!),0){prefs.edit().putString("theme",it).apply();action("theme","")}
  val v=row();v.addView(label("Accent colors",10f,colors.secondary),lp(65,-1));BrowserTheme.accentNames.forEachIndexed{i,name->val b=FrameLayout(context);b.addView(View(context).apply{background=GradientDrawable().apply{setColor(BrowserTheme.accentColor(name,Color.luminance(this@SettingsDashboard.colors.background)<.5));shape=GradientDrawable.OVAL}},FrameLayout.LayoutParams(d(18),d(18),Gravity.CENTER));node(b,"Accent: $name",1,i+1,selected=(prefs.getString("accent","Blue")==name),click={prefs.edit().putString("accent",name).apply();action("theme","")});v.addView(b,LayoutParams(0,-1,1f).apply{if(i>0)leftMargin=d(5)})};line(v)
  slider("Website text size",prefs.getInt("default_text_zoom",100),75,200,5,2){prefs.edit().putInt("default_text_zoom",it).apply();action("text","")}
  toggle("Touch comfort",prefs.getBoolean("comfort_touch",false),3){prefs.edit().putBoolean("comfort_touch",it).apply();action("comfort","")}
 }
 private fun controller(){
  options("Labels",listOf("snes" to "SNES","ps" to "PlayStation"),if(bindings.playStation)"ps"else"snes",0){bindings.playStation=it=="ps";render(true);action("labels","")}
  Control.entries.chunked(4).forEachIndexed{r,group->val v=row();group.forEachIndexed{c,a->val title=when(a){Control.CONFIRM->"Open";Control.BACK->"Back";Control.TABS->"Close tab";Control.CONTEXT->"Surf Bar";Control.POINTER->"Mouse";Control.ZOOM->"Zoom";else->a.title.removePrefix("Move ")};val b=box();b.addView(label(title,11f).apply{maxLines=2},LayoutParams(0,-1,1f));b.addView(makeBadge?.invoke(a)?:label(when(bindings.label(a)){"cross"->"×";"circle"->"○";"square"->"□";"triangle"->"△";else->bindings.label(a)},10f,colors.accent).apply{gravity=Gravity.CENTER},lp(if(bindings.label(a) in listOf("Start","Select"))34 else 25,18));node(b,a.title,r+1,c+1,click={recording=a;listening=false;candidate=bindings.key(a);renderAssignment();focus("Choose physical button")});v.addView(b,LayoutParams(0,-1,1f).apply{if(c>0)leftMargin=d(6)})};line(v)}
  buttons(listOf("Restore defaults" to {confirmInline("Restore controller defaults?",{bindings.reset();bindings.playStation=false;render();action("labels","")})}),5)
 }
 private fun mouse(){
  slider("Movement speed",(prefs.getInt("mouse_speed",600)/6f).roundToInt(),50,300,5,0){prefs.edit().putInt("mouse_speed",it*6).apply()}
  slider("Pointer size",prefs.getInt("pointer_size",100),50,220,5,1){prefs.edit().putInt("pointer_size",it).apply();refreshCursor();action("pointer","")}
  toggle("Mouse acceleration",prefs.getBoolean("mouse_acceleration",false),2){prefs.edit().putBoolean("mouse_acceleration",it).apply()}
  val shapes=row();shapes.addView(label("Style",12f,colors.secondary),lp(65,-1));MousePointer.styles.entries.forEachIndexed{i,e->val b=box().apply{gravity=Gravity.CENTER;addView(MousePointer(context,e.key),lp(18,23));tooltipText=e.value};node(b,"Pointer style: ${e.value}",3,i+1,selected=MousePointer.form(prefs)==e.key,click={val fill=MousePointer.fill(prefs);val outline=MousePointer.outline(prefs);prefs.edit().putString("pointer_style",e.key).putString("pointer_fill",fill).putString("pointer_outline",outline).apply();render(true);action("pointer","")});shapes.addView(b,LayoutParams(0,-1,1f).apply{if(i>0)leftMargin=d(5)})};line(shapes,32)
  val colorRow=row();fun colorChoice(title:String,key:String,values:List<Pair<String,String>>,value:String,col:Int){val b=box();b.addView(label(title,11f),LayoutParams(0,-1,1f));val t=label(values.first{it.first==value}.second,11f,colors.accent);b.addView(t,lp(-2,-1));fun step(delta:Int){val at=values.indexOfFirst{it.first==value};val outline=MousePointer.outline(prefs);prefs.edit().putString(key,values[(at+delta+values.size)%values.size].first).apply();if(key=="pointer_fill")prefs.edit().putString("pointer_outline",outline).apply();render(true);action("pointer","")};b.addView(label(" ›",16f),lp(16,-1));node(b,title,4,col,adjust={step(it)},click={step(1)});colorRow.addView(b,LayoutParams(0,-1,1f).apply{if(col>1)leftMargin=d(6)})};colorChoice("Fill","pointer_fill",listOf("white" to "White","black" to "Black","accent" to "Accent"),MousePointer.fill(prefs),1);colorChoice("Outline","pointer_outline",listOf("none" to "None","white" to "White","black" to "Black"),MousePointer.outline(prefs),2);line(colorRow,32)
  val area=FrameLayout(context).apply{contentDescription="Mouse test area";background=surface();clipChildren=true};preview=area;cursor=MousePointer(context).also{area.addView(it,FrameLayout.LayoutParams(d(18),d(23),Gravity.CENTER))};val testButton=button(if(test)"Stop test"else"Test mouse","Test mouse navigation",5){toggleMouseTest()};area.addView(testButton,FrameLayout.LayoutParams(d(104),d(30),Gravity.END or Gravity.CENTER_VERTICAL).apply{rightMargin=d(6)});line(area,32);refreshCursor()
 }
 private fun refreshCursor(){cursor?.apply{val size=prefs.getInt("pointer_size",100);layoutParams=FrameLayout.LayoutParams(d((10*size/100f).roundToInt()),d((14*size/100f).roundToInt()),if(test)Gravity.TOP or Gravity.START else Gravity.CENTER_VERTICAL).apply{leftMargin=if(test)0 else d(14)};invalidate()};preview?.let{v->fun walk(x:View){if(x is MousePointer)x.invalidate();if(x is ViewGroup)for(i in 0 until x.childCount)walk(x.getChildAt(i))};walk(v)}}
 fun toggleMouseTest(){if(category!="mouse")return;test=!test;action("mouse_test",test.toString());render(true)}
 fun mouseTesting()=category=="mouse"&&test
 fun movePointer(dx:Float,dy:Float){val p=preview?:return;val v=cursor?:return;if(!test)return;cursorX=(cursorX+dx).coerceIn(0f,max(0f,p.width-v.width.toFloat()));cursorY=(cursorY+dy).coerceIn(0f,max(0f,p.height-v.height.toFloat()));v.translationX=cursorX;v.translationY=cursorY}
 private fun privacy(){
  toggle("Adblock",status("adblock")=="true",0){action("adblock",it.toString())}
  toggle("Automatic cookie rejection",status("cookies")=="true",1){action("cookies",it.toString())}
  if(status("site").isNotBlank())toggle("Cookie rejection on this site",status("site_cookie")=="true",2){action("site_cookie",it.toString())}
  val filter=box();filter.addView(label(status("filters"),11f,colors.secondary),LayoutParams(0,-1,1f));filter.addView(button("Update filters","Update filters",3){action("filters","")},lp(100,-1));line(filter)
  buttons(listOf("Clear history" to {confirmInline("Remove saved browsing history?",{action("clear_history","");message("History cleared")})},"Clear site data" to {confirmInline("Sign out and clear website data?",{action("clear_data","");message("Website data cleared")})}),4)
  body.addView(label("Cookie rejection supports recognized banners; some need manual choices.",10f,colors.secondary).apply{maxLines=2},lp(-1,28))
 }
 private fun about(){
  listOf(listOf("GitHub","Ko-fi","Run setup again"),listOf("Export backup","Import backup","Copy diagnostics"),listOf("Help and controls","Privacy notice","Release notes")).forEachIndexed{r,items->buttons(items.map{title->title to {when(title){"Help and controls"->{help=!help;render(true)};"Import backup"->action("import","");else->action(title,"")}}},r)}
  body.addView(label(if(help)"D-pad / left stick: select. A: activate. B: return to categories, then browser. L3: test mouse in Mouse settings. Backup excludes tabs, cookies and browsing history."else"PocketSurf uses Android System WebView. No analytics. Backups contain bookmarks and preferences; browser data stays on this device.",12f,colors.secondary).apply{maxLines=4;gravity=Gravity.TOP;ellipsize=TextUtils.TruncateAt.END},LayoutParams(-1,0,1f))
 }
 private fun message(s:String){pending=null;recording=null;forget(footer);nodes.removeAll{it.row>=90};footer.removeAllViews();footer.addView(label(s,10f,colors.secondary).apply{maxLines=2},LayoutParams(0,-1,1f))}
 fun notify(s:String){if(pending==null&&recording==null)message(s)}
 fun confirmInline(s:String,yes:()->Unit){message(s);pending=yes;footer.addView(button("Confirm","Confirm setting action",90,1){pending=null;yes()},lp(74,-1).apply{leftMargin=d(6)});footer.addView(button("Cancel","Cancel setting action",90,2){message("No changes made");enterPane()},lp(64,-1).apply{leftMargin=d(6)});focus("Cancel setting action")}
 private fun renderAssignment(){val a=recording?:return;forget(footer);nodes.removeAll{it.row>=90};footer.removeAllViews();footer.addView(label(if(listening)"Press a button…"else a.title,10f,colors.secondary).apply{maxLines=2},LayoutParams(0,-1,1f));val keys=(Control.entries.map{bindings.key(it)}+Control.entries.map{it.defaultKey}+(96..110)+(188..203)+(19..23)).distinct();fun step(delta:Int){candidate=keys[(keys.indexOf(candidate).coerceAtLeast(0)+delta+keys.size)%keys.size];renderAssignment();focus("Choose physical button")};val value=button("‹ ${bindings.labelForKey(candidate)} ›","Choose physical button",90,1){step(1)};nodeAdjust(value){step(it)};footer.addView(value,lp(82,-1));footer.addView(button("Record","Record a button",90,2){listening=true;renderAssignment()},lp(54,-1).apply{leftMargin=d(6)});footer.addView(button("Apply","Apply button mapping",90,3){commitAssignment(a,candidate)},lp(54,-1).apply{leftMargin=d(6)});footer.addView(button("Cancel","Cancel button mapping",90,4){recording=null;message("Mapping unchanged");focus(a.title)},lp(58,-1).apply{leftMargin=d(6)})}
 private fun nodeAdjust(view:View,adjust:(Int)->Unit){val at=nodes.indexOfFirst{it.view===view};if(at>=0)nodes[at]=nodes[at].copy(adjust=adjust)}
 private fun commitAssignment(a:Control,key:Int){val occupied=bindings.action(key)?.takeIf{it!=a};val run={bindings.assign(a,key);recording=null;render();focus(a.title);action("labels","")};if(occupied==null)run()else confirmInline("Swap with ${occupied.title}?",run)}
 fun recordKey(e:KeyEvent):Boolean{val a=recording?:return false;if(!listening)return false;if(e.action==KeyEvent.ACTION_DOWN&&e.repeatCount==0){if(e.keyCode==KeyEvent.KEYCODE_BACK||e.keyCode==bindings.key(Control.BACK)){recording=null;message("Mapping unchanged");focus(a.title)}else if(e.keyCode in ((96..110)+(188..203)+(19..23)))commitAssignment(a,e.keyCode)};return true}
 fun back():Boolean{if(recording!=null||pending!=null){recording=null;message("No changes made");enterPane();return true};if(current?.col!=0){focus("Category: $category");return true};return false}
 fun move(direction:String){
  val n=nodes.firstOrNull{it.view.hasFocus()}?:current?.let{old->nodes.firstOrNull{it.view===old.view}}?:nodes.firstOrNull{it.id=="Category: $category"}?:return
  current=n
  if(n.col==0){when(direction){"up","down"->{val delta=if(direction=="up")-1 else 1;focus("Category: ${categories.keys.elementAt((n.row+delta).coerceIn(0,5))}")};"right"->enterPane()};return}
  if(direction in listOf("left","right")&&n.adjust!=null){n.adjust.invoke(if(direction=="left")-1 else 1);return}
  val candidates=nodes.filter{it.col>0&&it.view.isShown&&it.view.isEnabled&&it!==n}
  val next=when(direction){"left"->candidates.filter{it.row==n.row&&it.col<n.col}.maxByOrNull{it.col};"right"->candidates.filter{it.row==n.row&&it.col>n.col}.minByOrNull{it.col};"up"->candidates.filter{it.row<n.row}.minByOrNull{(n.row-it.row)*10+abs(it.col-n.col)};else->candidates.filter{it.row>n.row}.minByOrNull{(it.row-n.row)*10+abs(it.col-n.col)}}
  if(next!=null)next.view.requestFocus()else if(direction=="left")focus("Category: $category")
 }
}
