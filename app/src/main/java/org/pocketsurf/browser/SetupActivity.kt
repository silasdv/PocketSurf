package org.pocketsurf.browser

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*

/** Two optional first-run choices. No Android role or permission prompt is part of setup. */
class SetupActivity:Activity() {
 private val prefs by lazy{getSharedPreferences("browser",MODE_PRIVATE)}
 private val store by lazy{StartPageStore(prefs)}
 private val icons by lazy{FaviconStore(this)}
 private val bindings by lazy{ControllerBindings(prefs)}
 private val input=ControllerInput()
 private val handler=Handler(Looper.getMainLooper())
 private var step=0
 private var engine=SearchEngines.default.id
 private var bookmarksBar=false
 private val selected=linkedSetOf<String>()
 private lateinit var root:LinearLayout
 private val controls=mutableListOf<View>()
 private var touchInput=false
 private var controllerFocus:View?=null
 private val theme get()=BrowserTheme.resolve(prefs.getString("theme","Dark")!!,resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK==android.content.res.Configuration.UI_MODE_NIGHT_YES,prefs.getString("accent",null))
 private val tick=object:Runnable{override fun run(){input.repeat(SystemClock.uptimeMillis(),::move);handler.postDelayed(this,32)}}
 private fun dp(n:Int)=(resources.displayMetrics.density*n).toInt()
 private fun label(value:String,size:Float=16f,color:Int=theme.text)=TextView(this).apply{text=value;textSize=size;setTextColor(color);includeFontPadding=false;setLineSpacing(dp(3).toFloat(),1f)}
 private fun shape(fill:Int,stroke:Int=theme.divider,width:Int=1)=GradientDrawable().apply{setColor(fill);cornerRadius=dp(12).toFloat();setStroke(dp(width),stroke)}
 private fun focusable(view:View){controls.add(view);view.isFocusable=true;view.isFocusableInTouchMode=!touchInput;view.setOnFocusChangeListener{v,focus->if(focus)controllerFocus=v;v.background=shape(if(v.isSelected)theme.selected else theme.surface,if(focus||v.isSelected)theme.accent else theme.divider,if(focus)3 else 1)}}
 private fun actionIcon(name:String)=SurfIcon(name,theme.text).apply{setBounds(0,0,dp(20),dp(20))}
 private fun button(title:String,icon:String,action:()->Unit)=Button(this).apply{
  text=title;textSize=15f;isAllCaps=false;setTextColor(theme.text);minimumHeight=dp(48);minHeight=dp(48);setPadding(dp(12),dp(6),dp(12),dp(6));background=shape(theme.surface);setCompoundDrawablesRelative(actionIcon(icon),null,null,null);compoundDrawablePadding=dp(8);focusable(this);setOnClickListener{action()}
 }
 private fun siteIcon(title:String,url:String)=SiteIconView.create(this,url,dp(24),Color.luminance(theme.background)<.5).apply{
  contentDescription="$title icon";importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
  val cached=icons.get(url);refresh(cached)
  if(cached==null)icons.fetch(url){bitmap->if(bitmap!=null&&isAttachedToWindow&&!this@SetupActivity.isFinishing&&!this@SetupActivity.isDestroyed)refresh(bitmap)}
 }
 private fun choice(title:String,url:String,description:String,checked:Boolean,radio:Boolean,action:()->Unit)=LinearLayout(this).apply{
  orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(6),dp(12),dp(6));contentDescription=description;isSelected=checked;isClickable=true;background=shape(if(checked)theme.selected else theme.surface,if(checked)theme.accent else theme.divider);focusable(this)
  addView(siteIcon(title,url),LinearLayout.LayoutParams(dp(24),dp(24)).apply{rightMargin=dp(10)})
  addView(label(title,15f),LinearLayout.LayoutParams(0,-2,1f));addView(label(if(checked)"✓"else if(radio)""else"□",18f,theme.accent).apply{gravity=Gravity.CENTER;importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO},LinearLayout.LayoutParams(dp(22),-2))
  accessibilityDelegate=object:View.AccessibilityDelegate(){override fun onInitializeAccessibilityNodeInfo(host:View,info:AccessibilityNodeInfo){super.onInitializeAccessibilityNodeInfo(host,info);info.className=if(radio)"android.widget.RadioButton"else"android.widget.CheckBox";info.isCheckable=true;info.isChecked=checked}}
  setOnClickListener{action()}
 }
 override fun onCreate(state:Bundle?){theme.applyPlatform(this);super.onCreate(state);step=(state?.getInt("step")?:0).coerceIn(0,1);engine=state?.getString("engine")?:SearchEngines.resolve(prefs).id;bookmarksBar=state?.getBoolean("favoritesBar")?:prefs.getBoolean(BOOKMARKS_BAR,false)
  selected.addAll(state?.getStringArrayList("selected")?:ArrayList(store.shortcuts().map{it.url}));render()
  if(Build.VERSION.SDK_INT>=33)onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT){backStep()}
 }
 override fun onResume(){super.onResume();input.reset();handler.post(tick)}
 override fun onPause(){handler.removeCallbacks(tick);input.reset();super.onPause()}
 override fun onDestroy(){icons.close();super.onDestroy()}
 override fun onSaveInstanceState(out:Bundle){super.onSaveInstanceState(out);out.putInt("step",step);out.putString("engine",engine);out.putBoolean("favoritesBar",bookmarksBar);out.putStringArrayList("selected",ArrayList(selected))}
 private fun render(focusDescription:String?=null){
  controls.clear()
  window.statusBarColor=theme.background;window.navigationBarColor=theme.background
  window.decorView.systemUiVisibility=if(Color.luminance(theme.background)>.5)View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
  root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(theme.background);setPadding(dp(24),dp(16),dp(24),dp(12))};setContentView(root)
  if(Build.VERSION.SDK_INT>=29){root.isForceDarkAllowed=false;window.decorView.isForceDarkAllowed=false}
  root.setOnApplyWindowInsetsListener{v,insets->
   val l:Int;val t:Int;val r:Int;val b:Int
   if(Build.VERSION.SDK_INT>=30){val bars=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout());l=bars.left;t=bars.top;r=bars.right;b=bars.bottom}else{l=insets.systemWindowInsetLeft;t=insets.systemWindowInsetTop;r=insets.systemWindowInsetRight;b=insets.systemWindowInsetBottom}
   v.setPadding(dp(24)+l,dp(16)+t,dp(24)+r,dp(12)+b);insets
  };root.requestApplyInsets()
  root.addView(ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=2;progress=step+1;minimumHeight=0;progressTintList=ColorStateList.valueOf(theme.accent);progressBackgroundTintList=ColorStateList.valueOf(theme.divider);contentDescription="Setup progress: step ${step+1} of 2"},LinearLayout.LayoutParams(-1,dp(4)).apply{bottomMargin=dp(10)})
  root.addView(label("PocketSurf · Setup ${step+1} of 2",13f,theme.secondary),LinearLayout.LayoutParams(-1,-2))
  root.addView(label(if(step==0)"Choose your search engine"else"Choose pinned bookmarks",26f).apply{typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)},LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8);bottomMargin=dp(12)})
  val scroll=ScrollView(this).apply{isFillViewport=true;clipToPadding=false};val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val effectiveWidth=resources.configuration.screenWidthDp/maxOf(1f,resources.configuration.fontScale);val cols=if(effectiveWidth>=700)4 else if(effectiveWidth>=350)2 else 1
  if(step==0){
   body.addView(label("Used for searches. You can change this later in Settings.",14f),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
   SearchEngines.entries.chunked(cols).forEach{group->val row=LinearLayout(this);group.forEach{entry->row.addView(choice(entry.name,entry.template.replace("{query}",""),"Search engine: ${entry.name}",entry.id==engine,true){engine=entry.id;render("Search engine: ${entry.name}")},LinearLayout.LayoutParams(0,dp((54*maxOf(1f,resources.configuration.fontScale)).toInt()),1f).apply{setMargins(dp(3),dp(3),dp(3),dp(3))})};body.addView(row,LinearLayout.LayoutParams(-1,-2))}
  }else{
   body.addView(label("Optional: choose up to six websites. Nothing is pinned automatically.",14f),LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
   StartPageStore.popular.chunked(cols).forEach{group->val row=LinearLayout(this);group.forEach{site->row.addView(choice(site.title,site.url,"Pin website: ${site.title}",site.url in selected,false){
     if(site.url in selected)selected.remove(site.url)else if(selected.size<StartPageStore.MAX_SHORTCUTS)selected.add(site.url)else{Toast.makeText(this,"Pinned bookmarks has six spaces. Deselect a website first.",Toast.LENGTH_SHORT).show();return@choice};render("Pin website: ${site.title}")
    },LinearLayout.LayoutParams(0,dp((54*maxOf(1f,resources.configuration.fontScale)).toInt()),1f).apply{setMargins(dp(3),dp(3),dp(3),dp(3))})};body.addView(row,LinearLayout.LayoutParams(-1,-2))}
   body.addView(CheckBox(this).apply{text="Show bookmarks bar while browsing";contentDescription=text;setTextColor(theme.text);textSize=15f;buttonTintList=ColorStateList.valueOf(theme.accent);setCompoundDrawablesRelative(SurfIcon("bookmark",theme.accent).apply{setBounds(0,0,dp(20),dp(20))},null,null,null);compoundDrawablePadding=dp(8);isChecked=bookmarksBar;setPadding(dp(12),dp(4),dp(12),dp(4));background=shape(theme.surface);focusable(this);setOnCheckedChangeListener{_,checked->bookmarksBar=checked}},LinearLayout.LayoutParams(-1,dp((50*maxOf(1f,resources.configuration.fontScale)).toInt())).apply{topMargin=dp(12)})
  }
  val footer=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(10),0,0)}
  footer.addView(button(if(step==0)"Skip setup"else"Back",if(step==0)"close"else"back",::backStep),LinearLayout.LayoutParams(0,dp((48*maxOf(1f,resources.configuration.fontScale)).toInt()),1f).apply{rightMargin=dp(12)})
  footer.addView(button(if(step==1)"Start browsing"else"Continue",if(step==1)"check"else"forward"){if(step==1)complete(true)else{step=1;render()}},LinearLayout.LayoutParams(0,dp((48*maxOf(1f,resources.configuration.fontScale)).toInt()),1f));root.addView(footer,LinearLayout.LayoutParams(-1,-2))
  root.addView(setupLegend(),LinearLayout.LayoutParams(-1,dp((34*maxOf(1f,resources.configuration.fontScale)).toInt())).apply{topMargin=dp(6)})
  fun find(v:View):View?{if(v.contentDescription?.toString()==focusDescription&&focusDescription!=null)return v;if(v is ViewGroup)for(i in 0 until v.childCount)find(v.getChildAt(i))?.let{return it};return null}
  val target=find(root)?:footer.getChildAt(1);controllerFocus=target;if(!touchInput){target.requestFocus();target.post{if(!touchInput&&target.isAttachedToWindow&&window.decorView.findFocus()==null)target.requestFocus()}}
 }
 private fun setupLegend():View=HorizontalScrollView(this).apply{
  isHorizontalScrollBarEnabled=false;isFillViewport=true;isFocusable=false;setPadding(0,0,0,0);setBackgroundColor(Color.BLACK);contentDescription="Setup controls: D-pad / left stick Move; ${bindings.label(Control.CONFIRM)} Select; ${bindings.label(Control.BACK)} ${if(step==0)"Skip setup"else"Back"}"
  val row=SpacedLegendLayout(this@SetupActivity);addView(row,FrameLayout.LayoutParams(-2,-1))
  fun hint(badge:String,action:String){val group=LinearLayout(this@SetupActivity).apply{gravity=Gravity.CENTER_VERTICAL;if(badge=="D-pad / LS")tag="legend-trailing"};val symbols=mapOf("cross" to "×","circle" to "○","square" to "□","triangle" to "△");group.addView(label(symbols[badge]?:badge,10f,Color.WHITE).apply{gravity=Gravity.CENTER;background=shape(Color.TRANSPARENT,Color.WHITE);contentDescription=badge},LinearLayout.LayoutParams(dp(((if(badge=="D-pad / LS")68 else if(badge.length>2&&badge !in symbols)42 else 22)*maxOf(1f,resources.configuration.fontScale)).toInt()),dp((18*maxOf(1f,resources.configuration.fontScale)).toInt())));group.addView(label(action,9f,Color.WHITE),LinearLayout.LayoutParams(-2,-2).apply{leftMargin=dp(6)});row.addView(group,LinearLayout.LayoutParams(-2,-1))}
  hint("D-pad / LS","Move");hint(bindings.label(Control.CONFIRM),"Select");hint(bindings.label(Control.BACK),if(step==0)"Skip setup"else"Back")
 }
 private fun complete(save:Boolean){
  if(save){SearchEngines.entries.firstOrNull{it.id==engine}?.let{SearchEngines.select(prefs,it)}
   val popular=StartPageStore.popular.map{it.url}.toSet();store.shortcuts().filter{it.url in popular&&it.url !in selected}.forEach{store.remove(it.id)}
   StartPageStore.popular.filter{it.url in selected&&store.shortcuts().none{old->old.url==it.url}}.forEach{site->if(store.shortcuts().size<StartPageStore.MAX_SHORTCUTS)store.put(site.title,site.url)}
   prefs.edit().putBoolean(BOOKMARKS_BAR,bookmarksBar).apply()
  }
  prefs.edit().putBoolean(COMPLETE,true).apply();setResult(RESULT_OK);finish()
 }
 private fun backStep(){if(isFinishing)return;if(step>0){step=0;render()}else complete(false)}
 @Deprecated("Legacy Android back") override fun onBackPressed(){backStep()}
 private fun move(direction:String){val dir=when(direction){"up"->View.FOCUS_UP;"down"->View.FOCUS_DOWN;"left"->View.FOCUS_LEFT;else->View.FOCUS_RIGHT};window.decorView.findFocus()?.focusSearch(dir)?.requestFocus()}
 private fun controllerMode(){if(touchInput){touchInput=false;controls.forEach{it.isFocusableInTouchMode=true};if(window.decorView.findFocus()==null)controllerFocus?.requestFocus()}}
 override fun dispatchTouchEvent(event:MotionEvent):Boolean{if(event.actionMasked==MotionEvent.ACTION_DOWN){touchInput=true;input.reset();controls.forEach{it.isFocusableInTouchMode=false};window.decorView.findFocus()?.clearFocus()};return super.dispatchTouchEvent(event)}
 override fun dispatchKeyEvent(event:KeyEvent):Boolean{
  val action=bindings.action(event.keyCode)
  if(action in listOf(Control.CONFIRM,Control.BACK,Control.UP,Control.DOWN,Control.LEFT,Control.RIGHT)){
   controllerMode()
   if(event.action==KeyEvent.ACTION_DOWN&&event.repeatCount==0)when(action){Control.CONFIRM->window.decorView.findFocus()?.performClick();Control.BACK->backStep();Control.UP->move("up");Control.DOWN->move("down");Control.LEFT->move("left");Control.RIGHT->move("right");else->Unit};return true
  }
  if(event.keyCode==KeyEvent.KEYCODE_BACK){if(event.action==KeyEvent.ACTION_UP&&!event.isCanceled)backStep();return true}
  return super.dispatchKeyEvent(event)
 }
 override fun dispatchGenericMotionEvent(event:MotionEvent):Boolean{if(ControllerInput.isController(event)){controllerMode();input.update(input.read(event),SystemClock.uptimeMillis(),false,::move,{});return true};return super.dispatchGenericMotionEvent(event)}
 companion object{const val COMPLETE="setup_complete";const val BOOKMARKS_BAR="favorites_bar"}
}
