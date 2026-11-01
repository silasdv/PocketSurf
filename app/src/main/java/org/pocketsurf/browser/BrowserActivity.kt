package org.pocketsurf.browser

import android.app.Activity
import android.app.DownloadManager
import android.app.role.RoleManager
import android.content.*
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.*

/** Browser chrome, full-page native destinations, and the exclusive controller input boundary. */
open class BrowserActivity : Activity() {
 protected open val isPrivateSession=false
 override fun getSharedPreferences(name:String,mode:Int):android.content.SharedPreferences=if(isPrivateSession)applicationContext.getSharedPreferences(name,mode)else super.getSharedPreferences(name,mode)
 data class Tab(val id:String=UUID.randomUUID().toString(),var url:String="",var title:String="New tab",var home:Boolean=url.isBlank(),var progress:Int=100,var loadError:String?=null,var state:Bundle?=null,var view:WebView?=null,var preview:Bitmap?=null,var previewReady:Boolean=false,var lastUsed:Long=0,var scrollX:Int=0,var scrollY:Int=0,var restoreScroll:Boolean=false,var generation:Long=0,var formProtected:Boolean=false,var mediaProtected:Boolean=false,var trail:MutableList<String> = mutableListOf(),var trailIndex:Int=0,var manualHistory:Boolean=false,var historyReplay:Boolean=false,var transferActive:Boolean=false)
 private data class FocusNode(val view:View,val row:Int,val col:Int)
 private data class MenuItem(val icon:String,val title:String,val subtitle:String="",val siteUrl:String?=null,val action:()->Unit)
 private val prefs by lazy {getSharedPreferences("browser",MODE_PRIVATE)}
 private val homeStore by lazy {StartPageStore(prefs)}
 private val favicons by lazy {FaviconStore(this)}
 private val adblock by lazy {AdBlocker(this)}
 private val cookieConsent by lazy {CookieConsent(this)}
 private var setupPending=false
 private val data by lazy {BrowserData(prefs)}
 private val downloads by lazy {SurfDownloads(this)}
 private val bindings by lazy {ControllerBindings(prefs)}
 private val controller=ControllerInput()
 private val handler=Handler(Looper.getMainLooper())
 private val deviceListener=object:android.hardware.input.InputManager.InputDeviceListener {override fun onInputDeviceAdded(id:Int){stopInput()};override fun onInputDeviceChanged(id:Int){stopInput()};override fun onInputDeviceRemoved(id:Int){stopInput()}}
 private val tabs=mutableListOf<Tab>()
 private val closed=ArrayDeque<Tab>()
 private val hover=MouseHoverDispatcher()
 private var interactionEpoch=0L
 private var tabQuery=""
 private var lastClosedBatch=listOf<Tab>()
 private var lastClosedActive=""
 private var pendingBackup:JSONObject?=null
 private data class PendingGenerated(val lease:PageLease,val url:String,val name:String,val mime:String)
 private var pendingGenerated:PendingGenerated?=null
 private var generatedTransfer:GeneratedFileTransfer?=null
 private var pendingPermission:PermissionRequest?=null
 private var permissionEpoch=0L
 private var pendingImport:JSONObject?=null
 private var privateFinishing=false
 private var readerTitle="";private var readerText=""
 private var diagnosticError=""
 private var lastInputTime=0L
 private var active=-1
 private var selected=0
 private var screen="page"
 private var screenBack:(()->Unit)?=null
 private var returnFromTabs:Tab?=null
 private var focusNodes=mutableListOf<FocusNode>()
 private var focused:View?=null
 private var nativeScroll:ScrollView?=null
 private var nativeBody:LinearLayout?=null
 private var captureBinding:Control?=null
 private var remapTarget:Control?=null
 private val nativeFocus=mutableMapOf<String,Pair<Int,Int>>()
 private var searchTarget:Tab?=null
 private var searchReturn:(()->Unit)?=null
 private val collectionQueries=mutableMapOf<String,String>()
 private var collectionUndo:Pair<String,Visit>?=null
 private var collectionUndoPin:Shortcut?=null
 private var queryField:EditText?=null
 private var mouse=false
 private var pointerX=200f
 private var pointerY=140f
 private var frameScheduled=false
 private var resumed=false
 private var lastFrame=0L
 private var lastNav:String?=null
 private var lastNavTime=-1000L
 private var lastNavSource=""
 private val rawEvents=mutableMapOf<Int,Pair<String,Long>>()
 private var scrollRemainderX=0f
 private var scrollRemainderY=0f
 private var downloadPoll=0L
 private var downloadSignature=""
 private var downloadProblem:String?=null
 private var downloadRetry:(()->Unit)?=null
 private var confirmScreen:(()->Unit)?=null
 private var editingShortcut:Shortcut?=null
 private var fileCallback:ValueCallback<Array<Uri>>?=null
 private var fileLease:PageLease?=null
 private var permissionLease:PageLease?=null
 private var video:View?=null
 private var videoCallback:WebChromeClient.CustomViewCallback?=null
 private var prompt:android.app.AlertDialog?=null
 private lateinit var root:LinearLayout
 private lateinit var content:FrameLayout
 private lateinit var tabStrip:HorizontalScrollView
 private lateinit var tabNavigation:LinearLayout
 private lateinit var tabActions:LinearLayout
 private lateinit var stage:FrameLayout
 private lateinit var legendSurface:FrameLayout
 private lateinit var legendBackdrop:ImageView
 private var legendBitmap:Bitmap?=null
 private var lastTouchTime=0L
 private var fullscreenFlags=0
 private var fullscreenSystemUi=0
 private lateinit var tabRow:LinearLayout
 private lateinit var toolbar:LinearLayout
 private lateinit var favoritesBar:HorizontalScrollView
 private lateinit var favoritesRow:LinearLayout
 private lateinit var findBar:LinearLayout
 private lateinit var findField:EditText
 private lateinit var findCount:TextView
 private var findJob:Runnable?=null
 private var webHasSelection=false
 private var nativeRootScroll=true
 private var scrollRouteKnown=true
 private var scrollRoutePending=false
 private var scrollRouteRequestedAt=0L
 private var scrollRouteGeneration=0
 private lateinit var legend:LinearLayout
 private lateinit var addressText:TextView
 private lateinit var backButton:View
 private lateinit var forwardButton:View
 private lateinit var reloadIcon:ImageView
 private lateinit var reloadButton:View
 private lateinit var pointer:MousePointer
 private lateinit var addressHost:FrameLayout
 private lateinit var addressField:EditText
 private lateinit var goButton:View
 private lateinit var legendHost:HorizontalScrollView
 private var touchMode=false
 private var chromeFocus=false
 private var addressEditing=false
 private var addressKeyboardDismissed=false
 private var imeWasVisible=false
 private val addressTyping get()=addressEditing&&!addressKeyboardDismissed
 private var tabDock:LinearLayout?=null
 private var mouseHeldSeconds=0f
 private var webEditing=false
 private var imeExpected=false
 private var checkingEditing=false
 private var editingProbe=0
 private var nativeTyping=false
 private var chromeDirty=false
 private var nextEditCheck=0L
 private var lastScroll:Pair<Float,Float>?=null
 private var popupLayer:FrameLayout?=null
 private var popupKind=""
 private var dismissPopupGesture=false
 private var popupSavedNodes=listOf<FocusNode>()
 private var popupSavedFocus:View?=null
 private var popupTarget:View?=null
 private var suggestions:View?=null
 private var addressSavedNodes=listOf<FocusNode>()
 private var addressSavedFocus:View?=null
 private var shortcutUndo:Shortcut?=null
 private val tab get()=tabs.getOrNull(active)
 private val web get()=tab?.view
 private val theme get()=BrowserTheme.resolve(prefs.getString("theme","Dark")!!,resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_YES,prefs.getString("accent",null))
 private val layoutScale get()=max(1f,resources.configuration.fontScale)
 private val touchComfort get()=prefs.getBoolean("comfort_touch",false)
 private fun uiDp(value:Int)=dp((value*layoutScale).roundToInt())
 private fun dockHeight()=if(screen=="tabs"&&tabDock!=null)uiDp(if(resources.configuration.screenWidthDp<600)112 else 56)else 0
 private fun footerHeight()=if(video!=null)0 else uiDp(if(layoutScale>1.3f&&screen=="page"&&popupLayer==null&&!addressEditing)42 else 24)
 private fun dp(value:Int)=(value*resources.displayMetrics.density).roundToInt()
 private fun text(value:String,size:Float=15f,color:Int=theme.text,bold:Boolean=false)=TextView(this).apply {text=value;textSize=size;setTextColor(color);typeface=Typeface.create(if(bold)"sans-serif-medium"else"sans-serif",Typeface.NORMAL);includeFontPadding=false}
 private fun column()=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
 private fun row()=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
 private fun shape(color:Int,radius:Int=14,stroke:Int=Color.TRANSPARENT,width:Int=1)=GradientDrawable().apply {setColor(color);cornerRadius=dp(radius).toFloat();setStroke(dp(width),stroke)}
 private fun border()=if(Color.luminance(theme.background)>.5)0x1f000000 else 0x20ffffff
 private fun margin(w:Int=-1,h:Int=-2,left:Int=0,top:Int=0,right:Int=0,bottom:Int=0):LinearLayout.LayoutParams {val small=h in 12..54;val height=if(h<0)h else if(small)uiDp(if(touchComfort&&h in 28..40)if(w<0)52 else 48 else h)else dp(h);val width=if(w<0)w else if(small&&w in 12..48)uiDp(if(touchComfort&&w in 28..47)48 else w)else dp(w);return LinearLayout.LayoutParams(width,height).apply{setMargins(dp(left),dp(top),dp(right),dp(bottom))}}

 private fun icon(name:String,size:Int=22,color:Int=theme.text)=ImageView(this).apply {setImageDrawable(SurfIcon(name,color));imageTintList=android.content.res.ColorStateList.valueOf(color);layoutParams=LinearLayout.LayoutParams(dp(size),dp(size));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO}
 private fun focusOutline(view:View,row:Int,focusedNow:Boolean):android.graphics.drawable.Drawable?{if(row<=-2&&view.getTag(TAG_TAB)==null)return null;return shape(Color.TRANSPARENT,14,if(focusedNow)theme.focus else if(view.isSelected)theme.accent else border(),if(focusedNow)3 else 1)}
 private fun resetArrival(view:View){view.animate().cancel();view.alpha=1f;view.translationY=0f}
 private fun animateArrival(view:View){resetArrival(view);if(!resumed||!android.animation.ValueAnimator.areAnimatorsEnabled())return;view.alpha=0f;view.translationY=dp(4).toFloat();view.animate().alpha(1f).translationY(0f).setDuration(260).setInterpolator(android.view.animation.DecelerateInterpolator()).start()}
 private fun bindFocus(view:View,r:Int,c:Int,action:(()->Unit)?=null,chosen:Boolean=false){
  view.isFocusable=true;view.isFocusableInTouchMode=view is EditText||!touchMode;view.isSelected=chosen;if(view !is EditText)view.accessibilityDelegate=object:View.AccessibilityDelegate(){override fun onInitializeAccessibilityNodeInfo(host:View,info:android.view.accessibility.AccessibilityNodeInfo){super.onInitializeAccessibilityNodeInfo(host,info);info.className="android.widget.Button"}}
  fun paint(has:Boolean){val focus=has&&(r>-2||chromeFocus);view.background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf((theme.accent and 0x00ffffff) or 0x33000000),shape(if(focus&&!touchMode)theme.selected else theme.surface,14),shape(Color.WHITE,14));view.foreground=focusOutline(view,r,focus&&!touchMode)}
  if(r<=-2&&view !is EditText){view.background=android.graphics.drawable.StateListDrawable().apply{addState(intArrayOf(android.R.attr.state_pressed),shape(theme.surface,12));addState(intArrayOf(),shape(Color.TRANSPARENT,12))}}
  paint(false)
  if(r<=-2&&view !is EditText)view.background=shape(Color.TRANSPARENT,12)
  view.setOnFocusChangeListener {v,has->paint(has);if(r<=-2&&view !is EditText)view.background=shape(if(has&&!touchMode)theme.surface else Color.TRANSPARENT,12);if(has&&(r>-2||chromeFocus)){focused=v;if(screen=="tabs"){val i=(v.getTag(TAG_TAB)?:v.getTag(TAG_CLOSE)) as? Int;if(i!=null){selected=i;refreshJumpHints()}};updateLegend()}}
  if(action!=null)view.setOnClickListener {if(!touchMode)view.requestFocus();action()}
  focusNodes.add(FocusNode(view,r,c))
 }
 private val jumpHints=mutableListOf<Pair<Int,FrameLayout>>()
 private fun inlineHint(action:Control):View=badge(action,true,theme.text).apply{tag=TAG_HINT;isClickable=false;isFocusable=false;importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO;alpha=.72f;background=null;if(this is TextView){gravity=Gravity.END or Gravity.CENTER_VERTICAL;text=when(bindings.label(action)){"L1"->"L";"R1"->"R";else->bindings.label(action)};setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,if(bindings.label(action) in listOf("Start","Select"))5.5f else 6.5f);includeFontPadding=false};setTag(TAG_HINT_ACTION,action);contentDescription="Shortcut: ${bindings.label(action)}"}
 private fun shortcutButtonWidth(action:Control)=max(36,8+hintWidth(action))
 private fun hintWidth(action:Control)=if(bindings.label(action) in listOf("Start","Select"))22 else if(bindings.label(action) in listOf("cross","circle","square","triangle"))14 else if(bindings.label(action).length>2)22 else 14
 private fun cornerParams(width:Int)=FrameLayout.LayoutParams(dp(width),dp(9),Gravity.END or Gravity.BOTTOM).apply{rightMargin=dp(2);bottomMargin=dp(2)}
 private fun addAndroidBackHint(button:FrameLayout){for(i in button.childCount-1 downTo 1)if(button.getChildAt(i).tag==TAG_HINT)button.removeViewAt(i);(button.getChildAt(0) as? ImageView)?.layoutParams=FrameLayout.LayoutParams(dp(18),dp(18),Gravity.CENTER);button.addView(text("Back",5.5f,theme.text,true).apply{tag=TAG_HINT;gravity=Gravity.END or Gravity.CENTER_VERTICAL;setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,5.5f);alpha=.72f;isFocusable=false;importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO;contentDescription="Android Back"},cornerParams(22));button.tooltipText="Browser menu · Android Back"}
 private fun addInlineShortcut(button:FrameLayout,action:Control){for(i in button.childCount-1 downTo 1)if(button.getChildAt(i).tag==TAG_HINT)button.removeViewAt(i);(button.getChildAt(0) as? ImageView)?.layoutParams=FrameLayout.LayoutParams(dp(18),dp(18),Gravity.CENTER);button.addView(inlineHint(action),cornerParams(hintWidth(action)))}
 private fun updateShortcutVisibility(){if(!::toolbar.isInitialized)return;tabRow.descendantFocusability=if(screen=="page"&&chromeFocus&&popupLayer==null)ViewGroup.FOCUS_BEFORE_DESCENDANTS else ViewGroup.FOCUS_BLOCK_DESCENDANTS;fun walk(v:View){if(v.tag==TAG_HINT){val action=v.getTag(TAG_HINT_ACTION) as? Control;val usable=screen in listOf("page","tabs")&&popupLayer==null&&!addressEditing&&when(action){Control.HISTORY_BACK->backButton.isEnabled;Control.HISTORY_FORWARD->forwardButton.isEnabled;Control.SEARCH->screen=="page"&&!(chromeFocus&&focused===addressField);Control.MENU->screen=="page";Control.TABS->screen=="page";Control.CONTEXT->true;else->screen=="page"};v.visibility=if(usable)View.VISIBLE else View.INVISIBLE;return};if(v is ViewGroup)for(i in 0 until v.childCount)walk(v.getChildAt(i))};walk(toolbar);walk(tabNavigation);tabDock?.let{walk(it)}}
 private fun jumpHint(index:Int):FrameLayout=FrameLayout(this).also{jumpHints.add(index to it);updateJumpHint(index,it)}
 private fun updateJumpHint(index:Int,host:FrameLayout){host.removeAllViews();val order=if(screen=="tabs")tabs.indices.filter{tabs[it].title.contains(tabQuery,true)||tabs[it].url.contains(tabQuery,true)}else tabs.indices.toList();val reference=order.indexOf(if(screen=="tabs")selected else active);val action=when(index){order.getOrNull(reference-1)->Control.PREVIOUS_TAB;order.getOrNull(reference+1)->Control.NEXT_TAB;else->null};host.visibility=if(action==null)View.INVISIBLE else View.VISIBLE;host.minimumWidth=dp(max(hintWidth(Control.PREVIOUS_TAB),hintWidth(Control.NEXT_TAB)));if(action!=null)host.addView(inlineHint(action),FrameLayout.LayoutParams(dp(hintWidth(action)),dp(9),Gravity.CENTER))}
 private fun refreshJumpHints(){jumpHints.forEach{(i,v)->updateJumpHint(i,v)}}
 private fun iconButton(name:String,label:String,action:()->Unit):FrameLayout=FrameLayout(this).apply {
  minimumWidth=dp(48);minimumHeight=dp(48);isClickable=true;accessibilityDelegate=object:View.AccessibilityDelegate(){override fun onInitializeAccessibilityNodeInfo(host:View,info:android.view.accessibility.AccessibilityNodeInfo){super.onInitializeAccessibilityNodeInfo(host,info);info.className="android.widget.Button"}};contentDescription=label;tooltipText=label
  addView(icon(name),FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER))
  background=if(name=="close")null else android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf((theme.accent and 0x00ffffff) or 0x33000000),shape(theme.surface,12),shape(Color.WHITE,12));setOnClickListener{action()};when(label){"Settings"->addInlineShortcut(this,Control.MENU);"Browser menu"->addAndroidBackHint(this);"Open tabs"->addInlineShortcut(this,Control.TABS);"New tab"->addInlineShortcut(this,Control.CONTEXT)}
 }
 private fun tile(item:MenuItem,r:Int,c:Int):View=row().apply {
  setPadding(dp(18),dp(14),dp(18),dp(14));minimumHeight=dp(68)
  addView(item.siteUrl?.let{siteIcon(it,24)}?:icon(item.icon,24,theme.accent),margin(24,24,right=16))
  addView(column().apply {addView(text(item.title,16f,bold=true).apply {maxLines=1;ellipsize=TextUtils.TruncateAt.END});if(item.subtitle.isNotBlank())addView(text(item.subtitle,12f,theme.secondary).apply {maxLines=1;ellipsize=TextUtils.TruncateAt.END;setPadding(0,dp(5),0,0)})},LinearLayout.LayoutParams(0,-2,1f))
  addView(icon("forward",16,theme.secondary),margin(16,16,left=10));bindFocus(this,r,c,item.action)
 }
 private fun actionChip(title:String,icon:String,r:Int,c:Int,action:()->Unit)=row().apply {
  contentDescription=title
  minimumHeight=dp(48);gravity=Gravity.CENTER;setPadding(dp(16),dp(10),dp(16),dp(10));addView(this@BrowserActivity.icon(icon,18,theme.accent),margin(18,18,right=10));addView(text(title,14f,bold=true));bindFocus(this,r,c,action)
 }
 private fun focusFirst(){val saved=nativeFocus[screen];val node=focusNodes.firstOrNull{it.row==saved?.first&&it.col==saved?.second&&it.view.isEnabled}?:focusNodes.firstOrNull{it.row>=0&&it.view.isEnabled}?:focusNodes.firstOrNull();focused=node?.view;node?.view?.let{v->v.requestFocus();v.post{if(focused===v)v.requestFocus()}}}
 private fun isVisibleTree(view:View):Boolean {var v:View?=view;while(v!=null){if(v.visibility!=View.VISIBLE)return false;v=v.parent as? View};return true}
 private fun moveNative(direction:String){
  val current=focusNodes.firstOrNull{it.view===focused}?:focusNodes.firstOrNull{it.view.hasFocus()}
  if(current==null){focusFirst();return}
  if(screen=="page"&&popupLayer==null){
   if(direction=="up"&&current.row>=0&&focusNodes.none{it.row>=0&&it.row<current.row}){focusChrome();return}
   if(direction=="up"&&current.row==-2){chromeFocus=true;val target=focusNodes.firstOrNull{it.row==-3&&it.view.getTag(TAG_TAB)==active}?:focusNodes.firstOrNull{it.row==-3};target?.view?.let{focused=it;it.requestFocus()};return}
   if(direction=="down"&&current.row==-3){focusChrome();return}
   if(current.row==-1&&direction=="up"){focusChrome();return};if(current.row==-1&&direction=="down"){focusPageNeutral();return}
   if(direction=="down"&&current.row==-2){val favorite=focusNodes.firstOrNull{it.view.getTag(TAG_FAVORITE)==true};if(favorite!=null){chromeFocus=true;focused=favorite.view;favorite.view.requestFocus();return};chromeFocus=false;val first=focusNodes.firstOrNull{it.row>=0};if(first!=null){focused=first.view;first.view.requestFocus()}else{focused=null;web?.requestFocus();web?.evaluateJavascript("window.__pocketSurf && window.__pocketSurf.move('down')",null)};return}
  }
  val dx=when(direction){"left"->-1;"right"->1;else->0};val dy=when(direction){"up"->-1;"down"->1;else->0}
  val target=focusNodes.filter {it.view.visibility==View.VISIBLE&&isVisibleTree(it.view)&&it.view.isEnabled&&it!==current&&((direction!="left"&&direction!="right")||it.row==current.row)}.mapNotNull {n->
   val primary=if(dx!=0)(n.col-current.col)*dx else (n.row-current.row)*dy
   val cross=if(dx!=0)abs(n.row-current.row) else abs(n.col-current.col)
   if(primary<=0)null else n to (primary*10+cross*16)
  }.minByOrNull{it.second}?.first
  target?.view?.let {if(screen=="page")chromeFocus=target.row<0;focused=it;it.requestFocus();it.requestRectangleOnScreen(Rect(0,0,it.width,it.height),false)}
 }
 override fun onCreate(savedInstanceState:Bundle?){
  theme.applyPlatform(this)
  if(isPrivateSession&&(Build.VERSION.SDK_INT<28||(application as? SurfApplication)?.privateReady!=true)){super.onCreate(savedInstanceState);Toast.makeText(this,"Private browsing requires Android 9+ and a separate WebView data directory",Toast.LENGTH_LONG).show();super.finish();return}
  super.onCreate(savedInstanceState)
  if(isPrivateSession)window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
  // Upgrade the prototype's implicit DuckDuckGo default without overriding an explicitly chosen engine.
  if(!prefs.getBoolean("search_v2",false))prefs.edit().putBoolean("search_v2",true).apply()
  window.statusBarColor=theme.background;window.navigationBarColor=theme.background;window.decorView.systemUiVisibility=if(Color.luminance(theme.background)>.5)View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
  root=column().apply {setBackgroundColor(theme.background)}
  if(Build.VERSION.SDK_INT>=30)window.setDecorFitsSystemWindows(false)
  root.setOnApplyWindowInsetsListener {v,i->if(Build.VERSION.SDK_INT>=30){val bars=i.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout());val keyboard=i.getInsets(WindowInsets.Type.ime());onImeVisibilityChanged(i.isVisible(WindowInsets.Type.ime()));v.setPadding(bars.left,bars.top,bars.right,max(bars.bottom,keyboard.bottom));if(!i.isVisible(WindowInsets.Type.ime())&&!imeExpected&&webEditing)leaveWebEditing()}else v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom);i}
  tabRow=row().apply{setPadding(dp(10),dp(5),dp(10),dp(3))}
  tabStrip=HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false;addView(tabRow);setBackgroundColor(theme.background);addOnLayoutChangeListener{_,l,_,r,_,ol,_,orr,_->if(r-l!=orr-ol&&screen=="page")post{if(screen=="page")updateChrome()}}}
  tabActions=row();tabNavigation=row().apply{setBackgroundColor(theme.background);addView(tabStrip,LinearLayout.LayoutParams(0,-1,1f));addView(tabActions,margin(-2,-1))};root.addView(tabNavigation,margin(-1,36,bottom=4))
  buildToolbar();root.addView(toolbar,margin(-1,40));favoritesRow=row();favoritesBar=HorizontalScrollView(this).apply{contentDescription="Bookmarks bar";isHorizontalScrollBarEnabled=true;scrollBarStyle=View.SCROLLBARS_INSIDE_OVERLAY;isHorizontalFadingEdgeEnabled=true;setFadingEdgeLength(dp(16));addView(favoritesRow);visibility=View.GONE};root.addView(favoritesBar,margin(-1,30,bottom=3));buildFindBar();root.addView(findBar,margin(-1,40))
  stage=FrameLayout(this);root.addView(stage,LinearLayout.LayoutParams(-1,0,1f));content=FrameLayout(this).apply {setBackgroundColor(theme.background);isFocusable=true;isFocusableInTouchMode=true;descendantFocusability=ViewGroup.FOCUS_BEFORE_DESCENDANTS};stage.addView(content,FrameLayout.LayoutParams(-1,-1).apply{bottomMargin=footerHeight()})
  legendSurface=FrameLayout(this).apply{setBackgroundColor(Color.BLACK);importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO}
  legend=(if(layoutScale>1.3f)column()else SpacedLegendLayout(this)).apply {setPadding(0,dp(3),0,dp(3));gravity=Gravity.CENTER_VERTICAL};legendHost=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;isFillViewport=true;clipToPadding=false;addView(legend,ViewGroup.LayoutParams(-2,-1))};legendSurface.addView(legendHost,FrameLayout.LayoutParams(-1,-1));stage.addView(legendSurface,FrameLayout.LayoutParams(-1,footerHeight(),Gravity.BOTTOM))
  pointer=MousePointer(this).apply {isClickable=false;isFocusable=false;importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO}
  if(Build.VERSION.SDK_INT>=29){root.isForceDarkAllowed=false;window.decorView.isForceDarkAllowed=false}
  setContentView(root)
  getSystemService(android.hardware.input.InputManager::class.java).registerInputDeviceListener(deviceListener,handler)
  restoreSession();if(savedInstanceState==null&&intent?.action==Intent.ACTION_MAIN&&intent?.data==null)newTab()else showPage()
  if(savedInstanceState==null)intent?.data?.let {if(it.scheme in listOf("http","https"))openUrl(it.toString(),new=true)}
  when(savedInstanceState?.getString("destination")){"mouse_settings"->showMouseSettings();"bookmarks_bar"->showBookmarksBarSettings();"cookie_settings"->showCookieSettings();"private_info"->showPrivateInfo();"reading_settings"->showReadingSettings();"backup"->showBackup();"website_info"->showWebsiteInfo();"reader"->renderReader();"home_settings"->showHomeSettings();"adblock"->showAdblock();"shortcut_picker"->showShortcutPicker();"settings"->showSettings();"appearance"->showAppearance();"controller"->showController();"history"->showCollection("history");"bookmarks"->showCollection("bookmarks");"downloads"->showDownloads();"tabs"->showTabs()}
  if(Build.VERSION.SDK_INT>=33)onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT){systemBack()}

  if(!prefs.getBoolean(SetupActivity.COMPLETE,false)&&savedInstanceState==null){setupPending=true;startActivityForResult(Intent(this,SetupActivity::class.java),SETUP_REQUEST)}
 }
 private fun buildToolbar(){
  toolbar=row().apply{setPadding(dp(8),0,dp(8),0);setBackgroundColor(theme.background)}
  fun tool(name:String,label:String,action:()->Unit):View {val v=iconButton(name,label,action);v.minimumWidth=dp(36);v.minimumHeight=dp(36);(v.getChildAt(0) as ImageView).layoutParams=FrameLayout.LayoutParams(dp(18),dp(18),Gravity.CENTER);val shortcut=when(label){"Back"->Control.HISTORY_BACK;"Forward"->Control.HISTORY_FORWARD;"Open tabs"->Control.TABS;"Browser menu"->null;else->null};if(shortcut!=null)addInlineShortcut(v,shortcut);if(label=="Browser menu")addAndroidBackHint(v);toolbar.addView(v,margin(if(label=="Browser menu")36 else if(shortcut!=null)shortcutButtonWidth(shortcut) else 36,36,right=4));return v}
  backButton=tool("back","Back"){pageBack()};forwardButton=tool("forward","Forward"){pageForward()}
  reloadButton=tool("reload","Reload page"){tab?.let{t->if(t.loadError!=null)retryPage(t)else t.view?.let{if(t.progress<100){it.stopLoading();t.progress=100;updateNavigationState()}else it.reload()}}};reloadIcon=(reloadButton as ViewGroup).getChildAt(0) as ImageView
  addressField=EditText(this).apply{setSingleLine();textSize=13f;setTextColor(theme.text);setHintTextColor(theme.secondary);setPadding(dp(14),dp(6),dp(12),dp(6));background=shape(theme.surface,12);hint="Search ${engineName()} or enter address";contentDescription="Address or ${engineName()} search";showSoftInputOnFocus=false;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI;imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_GO or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN;importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO;setSelectAllOnFocus(true);setOnClickListener{val editing=addressEditing;showSearch();if(!editing)post{if(addressEditing)selectAll()}}}
  var addressTap=false;var tapX=0f;var tapY=0f
  addressField.setOnTouchListener{v,e->when(e.actionMasked){MotionEvent.ACTION_DOWN->{addressTap=true;tapX=e.x;tapY=e.y;false};MotionEvent.ACTION_MOVE->{if(abs(e.x-tapX)>ViewConfiguration.get(this).scaledTouchSlop||abs(e.y-tapY)>ViewConfiguration.get(this).scaledTouchSlop)addressTap=false;false};MotionEvent.ACTION_UP->{val activate=addressTap&&!addressEditing;addressTap=false;if(activate){showSearch();v.performClick();true}else false};MotionEvent.ACTION_CANCEL->{addressTap=false;false};else->false}}
  addressText=addressField;addressField.setPadding(dp(14),dp(6),dp(48),dp(6));addressHost=FrameLayout(this).apply{addView(addressField,FrameLayout.LayoutParams(-1,-1));addView(inlineHint(Control.SEARCH).apply{if(this is TextView)gravity=Gravity.CENTER},FrameLayout.LayoutParams(dp(hintWidth(Control.SEARCH)),dp(9),Gravity.END or Gravity.CENTER_VERTICAL).apply{rightMargin=dp(10)})};toolbar.addView(addressHost,LinearLayout.LayoutParams(0,uiDp(if(touchComfort)48 else 36),1f).apply{setMargins(dp(6),0,dp(8),0)})
  addressField.setOnEditorActionListener{_,_,_->submitAddress();true};addressField.addTextChangedListener(watcher{if(addressEditing)showAddressSuggestions(it)})
  goButton=tool("forward","Go"){submitAddress()};goButton.visibility=View.GONE
  tool("tabs","Open tabs"){showTabs()};tool("menu","Browser menu"){showMenu()}
 }

 private fun buildFindBar(){
  findBar=row().apply{visibility=View.GONE;setPadding(dp(8),0,dp(8),0);setBackgroundColor(theme.background);contentDescription="Find in page bar"}
  findField=EditText(this).apply{hint="Find in page";contentDescription="Find in page";setSingleLine();textSize=13f;setTextColor(theme.text);setHintTextColor(theme.secondary);background=shape(theme.surface,9);setPadding(dp(12),0,dp(8),0);imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN}
  findBar.addView(findField,LinearLayout.LayoutParams(0,uiDp(34),1f));findCount=text("0/0",11f,theme.secondary).apply{contentDescription="Match count";gravity=Gravity.CENTER};findBar.addView(findCount,margin(55,34,left=5))
  listOf(Triple("back","Previous match",{web?.findNext(false);Unit}),Triple("forward","Next match",{web?.findNext(true);Unit}),Triple("close","Close find",{closeFind()})).forEach{(name,label,action)->findBar.addView(iconButton(name,label,action).apply{minimumHeight=dp(34);minimumWidth=dp(36)},margin(36,34,left=4))}
  findField.setOnEditorActionListener{_,_,_->web?.findNext(true);true};findField.addTextChangedListener(watcher{query->findJob?.let{handler.removeCallbacks(it)};findJob=Runnable{if(findBar.visibility==View.VISIBLE){if(query.isEmpty()){web?.clearMatches();findCount.text="0/0"}else web?.findAllAsync(query)}}.also{handler.postDelayed(it,140)}})
 }
 private fun showFind(){if(tab?.home!=false)return;interactionEpoch++;hover.leave();closePopup();cancelAddress(false);if(webEditing)leaveWebEditing();stopInput();findBar.visibility=View.VISIBLE;updateFavoritesBar();focusNodes.removeAll{it.view.getTag(TAG_FIND)==true};for(i in 0 until findBar.childCount){val child=findBar.getChildAt(i);if(child===findCount)continue;child.setTag(TAG_FIND,true);bindFocus(child,-1,i,if(child===findField)null else ({child.performClickDirectFind()}))};chromeFocus=true;focused=findField;nativeTyping=true;imeExpected=true;findField.requestFocus();findField.selectAll();getSystemService(InputMethodManager::class.java).showSoftInput(findField,InputMethodManager.SHOW_IMPLICIT);if(findField.text.isNotEmpty())web?.findAllAsync(findField.text.toString());updateLegend()}
 private fun View.performClickDirectFind(){when(contentDescription?.toString()){ "Previous match"->web?.findNext(false);"Next match"->web?.findNext(true);"Close find"->closeFind()}}
 private fun closeFind(){if(!::findBar.isInitialized||findBar.visibility!=View.VISIBLE)return;findJob?.let{handler.removeCallbacks(it)};findJob=null;web?.clearMatches();findBar.visibility=View.GONE;focusNodes.removeAll{it.view.getTag(TAG_FIND)==true};nativeTyping=false;imeExpected=false;hideKeyboard();updateFavoritesBar();focusPageNeutral();updateLegend()}

 private fun tabJson(t:Tab):JSONObject=JSONObject().put("id",t.id).put("url",t.url).put("title",t.title).put("home",t.home).put("x",t.scrollX).put("y",t.scrollY).put("trail",JSONArray(t.trail)).put("trailIndex",t.trailIndex)
 private fun readTab(o:JSONObject):Tab {val history=o.optJSONArray("trail")?:JSONArray();val trail=(0 until history.length()).map{history.optString(it)}.filter{it.startsWith("http://")||it.startsWith("https://")}.take(200).toMutableList();return Tab(id=o.optString("id",UUID.randomUUID().toString()),url=o.optString("url"),title=o.optString("title","New tab"),home=o.optBoolean("home",false),scrollX=o.optInt("x").coerceAtLeast(0),scrollY=o.optInt("y").coerceAtLeast(0),restoreScroll=true,trail=trail,trailIndex=o.optInt("trailIndex").coerceIn(0,max(0,trail.lastIndex)),manualHistory=trail.isNotEmpty(),historyReplay=trail.isNotEmpty())}
 private fun captureTab(t:Tab){val v=t.view?:return;if(!t.restoreScroll){t.scrollX=v.scrollX;t.scrollY=v.scrollY};if(!t.manualHistory)runCatching{val h=v.copyBackForwardList();if(h.size>0){val entries=(0 until h.size).map{it to h.getItemAtIndex(it).url}.filter{it.second.startsWith("http://")||it.second.startsWith("https://")};val cursor=entries.indexOfLast{it.first<=h.currentIndex}.coerceAtLeast(0);val first=(cursor-99).coerceAtLeast(0);t.trail=entries.drop(first).take(200).map{it.second}.toMutableList();t.trailIndex=(cursor-first).coerceIn(0,max(0,t.trail.lastIndex))}}}
 private fun restoreSession(){if(isPrivateSession)return;try{val raw=prefs.getString("sessions_v2",null);if(raw!=null){val a=JSONArray(raw);for(i in 0 until a.length())tabs.add(readTab(a.getJSONObject(i)))}else{val a=JSONArray(prefs.getString("tabs","[]"));for(i in 0 until a.length()){val u=a.getString(i);if(u.isNotBlank()&&!u.contains("pocketsurf.invalid"))tabs.add(Tab(url=u,title=host(u)))}};active=if(tabs.isEmpty())-1 else prefs.getInt("active",0).coerceIn(0,tabs.lastIndex);val recent=JSONArray(prefs.getString("closed_v2","[]"));for(i in 0 until min(recent.length(),500))closed.addLast(readTab(recent.getJSONObject(i)));val batch=JSONArray(prefs.getString("last_closed_batch","[]"));lastClosedBatch=(0 until batch.length()).map{readTab(batch.getJSONObject(it))};lastClosedActive=prefs.getString("last_closed_active","").orEmpty()}catch(_:Exception){tabs.clear();active=-1}}
 private fun saveSession(){if(isPrivateSession)return;tabs.forEach{captureTab(it)};prefs.edit().putString("sessions_v2",JSONArray().apply{tabs.forEach{put(tabJson(it))}}.toString()).putInt("active",active).putString("closed_v2",JSONArray().apply{closed.forEach{put(tabJson(it))}}.toString()).putString("last_closed_batch",JSONArray().apply{lastClosedBatch.forEach{put(tabJson(it))}}.toString()).putString("last_closed_active",lastClosedActive).apply()}
 private fun restorePosition(t:Tab,v:WebView,attempt:Int=0){if(!t.restoreScroll||t.view!==v)return;v.postDelayed({if(t.restoreScroll&&t.view===v){v.scrollTo(t.scrollX,t.scrollY);if(abs(v.scrollY-t.scrollY)<=2||attempt>=7)t.restoreScroll=false else restorePosition(t,v,attempt+1)}},if(attempt==0)32 else 250)}
 private fun host(url:String)=PageIdentity.host(url)
 private fun updateChrome(){
  if(!::tabRow.isInitialized)return
  if(popupLayer!=null||addressEditing){chromeDirty=true;return}
  val previousChrome=focusNodes.firstOrNull{it.view===focused&&it.row<=-2}
  for(i in 0 until toolbar.childCount){val button=toolbar.getChildAt(i) as? FrameLayout?:continue;if(button===addressHost)continue;val action=when(button.contentDescription?.toString()){ "Back"->Control.HISTORY_BACK;"Forward"->Control.HISTORY_FORWARD;"Open tabs"->Control.TABS;else->null};if(action!=null){addInlineShortcut(button,action);button.layoutParams?.let{it.width=uiDp(if(touchComfort)max(48,shortcutButtonWidth(action))else shortcutButtonWidth(action))}};if(button.contentDescription=="Browser menu")addAndroidBackHint(button)}
  jumpHints.clear();focusNodes.removeAll{it.row<=-2};tabRow.removeAllViews();tabActions.removeAllViews();tabRow.setPadding(dp(6),0,dp(6),0)
  val stripWidth=if(tabStrip.width>0)(tabStrip.width/resources.displayMetrics.density).toInt()else resources.configuration.screenWidthDp-160
  val hintSlot=max(hintWidth(Control.PREVIOUS_TAB),hintWidth(Control.NEXT_TAB));val closeWidth=30;val minimumTab=8+16+4+hintSlot+4+closeWidth+6;val plan=TabStripPlan.create(stripWidth-12,tabs.size,active,minimumTab,minimumTab+uiDp(20),shortcutButtonWidth(Control.CONTEXT))
  plan.indices.forEachIndexed{position,i->val t=tabs[i];val chip=FrameLayout(this).apply{contentDescription="Tab ${i+1}: ${t.title}";setTag(TAG_TAB,i)};val line=row().apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),0,dp(6),0);addView(siteIcon(t.url,16),margin(16,16,right=4));if(plan.titles)addView(text(if(t.home)"New tab"else t.title,11f,if(i==active)theme.text else theme.secondary).apply{maxLines=1;ellipsize=TextUtils.TruncateAt.END;minWidth=0},LinearLayout.LayoutParams(0,-2,1f))else addView(Space(this@BrowserActivity),LinearLayout.LayoutParams(0,1,1f))}
   line.addView(jumpHint(i),margin(hintSlot,9,right=4));if(plan.closeButtons){val close=iconButton("close","Close ${t.title}"){closeTab(i,false)}.apply{minimumHeight=dp(32);minimumWidth=dp(30);setTag(TAG_CLOSE,i);(getChildAt(0) as ImageView).layoutParams=FrameLayout.LayoutParams(dp(14),dp(14),Gravity.CENTER)};close.isFocusable=false;close.isFocusableInTouchMode=false;line.addView(close,margin(closeWidth,32))};chip.addView(line,FrameLayout.LayoutParams(-1,-1));
   bindFocus(chip,-3,(i+1)*2,{if(i==active)focusPageNeutral()else switchTab(i)},i==active);chip.setOnLongClickListener{if(t===tab)showMenu()else showMenu(listOf(MenuItem("close","Close tab"){closeTab(tabs.indexOf(t),false)}),t.title,anchor=chip);true};tabRow.addView(chip,margin(plan.widthFor(position),34,right=4))
  }
  if(plan.hidden>0){val hiddenTargets=listOf(active-1,active+1).filter{it in tabs.indices&&it !in plan.indices};val overflow=FrameLayout(this).apply{contentDescription="${plan.hidden} more tabs — open tab overview";addView(text("+${plan.hidden}",11f,theme.text,true).apply{gravity=Gravity.CENTER;if(hiddenTargets.size==2)setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,8f)},FrameLayout.LayoutParams(-1,if(hiddenTargets.isEmpty())-1 else dp(if(hiddenTargets.size==2)10 else 18),if(hiddenTargets.isEmpty())Gravity.CENTER else Gravity.TOP));hiddenTargets.forEachIndexed{position,index->addView(jumpHint(index),FrameLayout.LayoutParams(-2,dp(12),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply{topMargin=dp(if(hiddenTargets.size==2)10+12*position else 22)})};bindFocus(this,-3,tabs.size*2+1,{showTabs()})};tabRow.addView(overflow,margin(40,34,right=4))}
  if(plan.showNewTabButton){val plus=iconButton("plus","New tab"){newTab()}.apply{minimumHeight=dp(32);minimumWidth=dp(34)};bindFocus(plus,-3,tabs.size*2+2,{newTab()});tabRow.addView(plus,margin(shortcutButtonWidth(Control.CONTEXT),34))}
  listOf(Triple("download","Downloads",{showDownloads();Unit}),Triple("share","Share page",{tab?.takeUnless{it.home}?.let{share(it.url)};Unit}),Triple("history","History",{showCollection("history")}),Triple("settings","Settings",{showSettings()})).forEachIndexed{i,(name,label,action)->val v=iconButton(name,label,action).apply{minimumHeight=dp(36);minimumWidth=dp(36);(getChildAt(0) as ImageView).layoutParams=FrameLayout.LayoutParams(dp(18),dp(18),Gravity.CENTER)};if(name=="share"){v.isEnabled=tab?.home==false;v.visibility=View.VISIBLE;v.alpha=if(v.isEnabled)1f else .32f};if(name=="settings")addInlineShortcut(v,Control.MENU);bindFocus(v,-3,tabs.size*2+3+i,action);tabActions.addView(v,margin(if(name=="settings")36 else 36,36,right=4))}
  if(isPrivateSession){val info=iconButton("private","Private session active"){showPrivateInfo()};bindFocus(info,-3,tabs.size*2+7,{showPrivateInfo()});tabActions.addView(info,margin(36,36,right=4))}
  for(i in 0 until toolbar.childCount){val child=toolbar.getChildAt(i);if(child===addressHost)bindFocus(addressField,-2,i,{showSearch()})else bindFocus(child,-2,i,{child.performClickDirect()})}
  if(chromeFocus&&previousChrome!=null)focusNodes.firstOrNull{it.row==previousChrome.row&&it.col==previousChrome.col}?.view?.let{focused=it;it.requestFocus()}
  if(screen!="page"&&!addressEditing)focusNodes.removeAll{it.row<=-2}
  for(i in addressHost.childCount-1 downTo 1)addressHost.removeViewAt(i);addressHost.addView(inlineHint(Control.SEARCH).apply{if(this is TextView)gravity=Gravity.CENTER},FrameLayout.LayoutParams(dp(hintWidth(Control.SEARCH)),dp(9),Gravity.END or Gravity.CENTER_VERTICAL).apply{rightMargin=dp(10)})
  updateFavoritesBar();(0 until toolbar.childCount).map{toolbar.getChildAt(it)}.firstOrNull{it.contentDescription=="Open tabs"}?.let{v->(v as? ViewGroup)?.getChildAt(0)?.let{(it as? ImageView)?.setImageDrawable(TabCountIcon(tabs.size,theme.text))}};updateNavigationState();updateLegend()
 }
 private fun View.performClickDirect(){when(contentDescription?.toString()){ "Back"->pageBack();"Forward"->pageForward();"Open tabs"->showTabs();"Browser menu"->showMenu();"Go"->submitAddress();else->tab?.let{t->if(t.loadError!=null)retryPage(t)else t.view?.let{if(t.progress<100){it.stopLoading();t.progress=100;updateNavigationState()}else it.reload()}}}}
 private fun siteIcon(url:String,size:Int):View=SiteIconView.create(this,url,dp(size),Color.luminance(theme.background)<.5).apply{setTag(TAG_ICON,url);refresh(favicons.get(url))}
 private fun refreshSiteIcons(){fun refresh(v:View){if(v is SiteIconView){val url=v.getTag(TAG_ICON) as? String;if(url!=null)v.refresh(favicons.get(url));return};if(v is ViewGroup)for(i in 0 until v.childCount)refresh(v.getChildAt(i))};refresh(root)}
 private fun updateFavoritesBar(){
  focusNodes.removeAll{it.view.getTag(TAG_FAVORITE)==true||it.view.contentDescription=="All bookmarks"};favoritesRow.removeAllViews()
  val visible=screen=="page"&&tab?.home==false&&prefs.getBoolean("favorites_bar",false)&&findBar.visibility!=View.VISIBLE;favoritesBar.visibility=if(visible)View.VISIBLE else View.GONE;if(!visible)return
  favoritesRow.setPadding(dp(8),0,dp(8),0)
  val all=row().apply{contentDescription="All bookmarks";setPadding(dp(8),0,dp(10),0);addView(icon("bookmark",16,theme.accent),margin(16,16,right=6));addView(text("All",10f));bindFocus(this,-1,-1,{collectionQueries.remove("bookmarks");showCollection("bookmarks")})};favoritesRow.addView(all,margin(-2,28,right=8))
  val sites=(homeStore.shortcuts().map{it.title to it.url}+data.entries("bookmarks").map{it.title to it.url}).distinctBy{it.second}
  sites.forEachIndexed{i,(title,url)->val chip=row().apply{contentDescription="Bookmark: $title";setTag(TAG_FAVORITE,true);setPadding(dp(8),0,dp(10),0);addView(siteIcon(url,16),margin(16,16,right=6));addView(text(title,10f).apply{maxLines=1;maxWidth=dp(130);ellipsize=TextUtils.TruncateAt.END});bindFocus(this,-1,i,{openUrl(url)})};favoritesRow.addView(chip,margin(-2,28,right=5))}

 }

 private fun updateNavigationState(){
  val t=tab;val address=t?.takeUnless{it.home}?.url?.let{PageIdentity.label(it)}?:"Search ${engineName()} or enter address"
  if(!addressEditing)addressText.text=if(t?.home==false&&t.loadError==null&&t.progress<100)"Loading ${t.progress}% · $address"else (if(isPrivateSession)"Private · "else"")+address
  backButton.isEnabled=t?.let{!it.home}==true;backButton.visibility=View.VISIBLE;backButton.alpha=if(backButton.isEnabled)1f else .32f
  forwardButton.isEnabled=t?.let{(it.home&&it.url.isNotBlank())||it.view?.canGoForward()==true||(it.manualHistory&&it.trailIndex<it.trail.lastIndex)}==true;forwardButton.visibility=View.VISIBLE;forwardButton.alpha=if(forwardButton.isEnabled)1f else .32f
  val loading=t?.let{!it.home&&it.loadError==null&&it.progress<100}==true
  reloadButton.isEnabled=t?.let{!it.home&&it.view!=null}==true;reloadButton.visibility=View.VISIBLE;reloadButton.alpha=if(reloadButton.isEnabled)1f else .32f
  reloadButton.contentDescription=if(loading)"Stop loading"else if(t?.loadError!=null)"Retry page"else"Reload page";reloadButton.tooltipText=reloadButton.contentDescription
  reloadIcon.setImageDrawable(SurfIcon(if(loading)"stop"else"reload",theme.text));updateShortcutVisibility()
 }
 private fun badge(action:Control,monochrome:Boolean=false,monochromeColor:Int=Color.WHITE):View {
  val label=bindings.label(action);val base=ControllerBindings.color(label);val color=if(monochrome)monochromeColor else if(bindings.playStation&&Color.luminance(theme.background)>.5)Color.rgb((Color.red(base)*.62f).toInt(),(Color.green(base)*.62f).toInt(),(Color.blue(base)*.62f).toInt())else base
  if(label in listOf("cross","circle","square","triangle"))return object:View(this){val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {this.color=color;style=Paint.Style.STROKE;strokeWidth=2f;strokeJoin=Paint.Join.ROUND;strokeCap=Paint.Cap.ROUND};override fun onDraw(c:Canvas){super.onDraw(c);val z=min(width,height)/24f;c.save();c.translate((width-24*z)/2,(height-24*z)/2);c.scale(z,z);when(label){"cross"->{c.drawLine(7f,7f,17f,17f,p);c.drawLine(17f,7f,7f,17f,p)};"circle"->c.drawCircle(12f,12f,7f,p);"square"->c.drawRoundRect(5f,5f,19f,19f,1f,1f,p);else->{val path=Path().apply {moveTo(12f,4f);lineTo(21f,20f);lineTo(3f,20f);close()};c.drawPath(path,p)}};c.restore()}}.apply {contentDescription=label}
  return text(if(monochrome&&label=="R1")"R"else if(monochrome&&label=="L1")"L"else label,if(monochrome&&label in listOf("Start","Select"))10f else if(monochrome&&label.length>1)8f else if(label.length>2)11f else if(monochrome)10f else 12f,if(monochrome)monochromeColor else if(label in listOf("A","B","X","Y"))Color.WHITE else theme.text,true).apply {gravity=Gravity.CENTER;background=shape(if(monochrome)Color.TRANSPARENT else if(label in listOf("A","B","X","Y"))when(label){"A"->0xffb52d45.toInt();"B"->0xff826000.toInt();"X"->0xff205ea8.toInt();else->0xff13704b.toInt()} else theme.surface,if(label.length==1)20 else 5,if(monochrome)monochromeColor else if(label.length>1)theme.secondary else Color.TRANSPARENT)}
 }
 private fun updateLegend(){
  if(!::legend.isInitialized)return
  updateShortcutVisibility()
  legendSurface.visibility=if(video!=null)View.GONE else View.VISIBLE;legendHost.visibility=if(video!=null)View.GONE else View.VISIBLE;legend.removeAllViews()
  val entries=when{
   popupLayer!=null->listOf(Control.CONFIRM to "Select",Control.BACK to "Close")
   addressEditing->listOf(Control.CONFIRM to if(addressKeyboardDismissed)"Select"else"Type",Control.BACK to if(addressKeyboardDismissed)"Cancel"else"Close keyboard",Control.SEARCH to "Address")
   captureBinding!=null->listOf(Control.BACK to "Cancel recording")
   webEditing->listOf(Control.CONFIRM to if(mouse)"Open"else"Type",Control.BACK to "Close keyboard")
   screen=="mouse_settings"->(if(focused is SeekBar)listOf(Control.LEFT to "Slower",Control.RIGHT to "Faster")else listOf(Control.CONFIRM to "Select"))+listOf(Control.BACK to "Back",Control.POINTER to if(mouse)"Stop test"else"Test mouse")
    screen=="tabs"->listOf(Control.CONFIRM to if(focused?.contentDescription=="New tab")"New tab"else"Select",Control.BACK to "Back")+(if(focused?.getTag(TAG_TAB)!=null)listOf(Control.HISTORY_FORWARD to "Close tab")else emptyList())+(if(queryField?.contentDescription=="Search tabs")listOf(Control.SEARCH to "Find tab")else emptyList())
   screen=="page"->listOf(Control.CONFIRM to if(tab?.home==false&&!chromeFocus)"Open"else"Select",Control.BACK to if(tabs.isEmpty())"Exit"else"Close")+(if(tab?.home==false&&tab?.loadError==null)listOf(Control.POINTER to if(mouse)"Elements"else"Mouse",Control.ZOOM to "Zoom")else emptyList())
   else->listOf(Control.CONFIRM to "Select",Control.BACK to "Back")+(if(screen in listOf("history","bookmarks"))listOf(Control.SEARCH to "Find")else emptyList())

  }
  (legendSurface.layoutParams as FrameLayout.LayoutParams).height=footerHeight();legendSurface.requestLayout();(content.layoutParams as FrameLayout.LayoutParams).bottomMargin=footerHeight()+dockHeight();tabDock?.let{(it.layoutParams as FrameLayout.LayoutParams).bottomMargin=footerHeight()};content.requestLayout()
  val showScroll=screen=="page"&&popupLayer==null&&((tab?.home==false&&tab?.loadError==null)||nativeScroll?.canScrollVertically(-1)==true||nativeScroll?.canScrollVertically(1)==true)
  val total=entries.size+if(showScroll)1 else 0;val split=(total+1)/2
  val lines=if(layoutScale>1.3f){val count=if(total>4)2 else 1;List(count){SpacedLegendLayout(this).also{legend.addView(it,LinearLayout.LayoutParams(-1,0,1f))}}}else listOf(legend)
  fun addHint(v:View,index:Int){lines[if(lines.size>1&&index>=split)1 else 0].addView(v,margin(-2,18))}
  entries.forEachIndexed{index,(a,label)->val hint=row().apply{gravity=Gravity.CENTER_VERTICAL;alpha=1f;if(a in listOf(Control.POINTER,Control.ZOOM))tag="legend-trailing"};val labelName=bindings.label(a);hint.addView(badge(a,true),margin(if(labelName in listOf("Start","Select"))38 else if(labelName in listOf("L1","L2","L3","R1","R2","R3","RS"))18 else if(labelName.length>2&&labelName !in listOf("cross","circle","square","triangle"))26 else 14,14,right=5));hint.addView(text(label,9f,Color.WHITE).apply{gravity=Gravity.CENTER_VERTICAL});addHint(hint,index)}
  if(showScroll){val hint=row().apply{gravity=Gravity.CENTER_VERTICAL;alpha=1f;tag="legend-trailing"};hint.addView(text("RS",8f,Color.WHITE,true).apply{gravity=Gravity.CENTER;background=shape(Color.TRANSPARENT,5,Color.WHITE)},margin(18,14,right=5));hint.addView(text("Scroll",9f,Color.WHITE));addHint(hint,entries.size)}
  val mode=if(touchMode)"Touch"else"Controller";legendHost.contentDescription="$mode controls: "+entries.joinToString{"${bindings.label(it.first)} ${it.second}"}+if(showScroll)"; Right stick scroll"else""
 }
 private fun stopInput(){scrollRouteGeneration++;scrollRoutePending=false;scrollRouteKnown=!mouse&&!webHasSelection;nativeRootScroll=true;editingProbe++;checkingEditing=false;controller.reset();sendScroll(0f,0f);lastNav=null;lastNavTime=-1000L;scrollRemainderX=0f;scrollRemainderY=0f}
 private val previewCacheHolder=lazy{TabPreviewCache(this)}
 private val previewCache get()=previewCacheHolder.value
 private var previewPending=false
 private var previewKey=""
 private fun capturePreview(force:Boolean=true){val t=tab?:return;val v=t.view?.takeUnless{t.home};if(screen!="page"||content.width<1||content.height<1||video!=null||popupLayer!=null||addressEditing||t.loadError!=null||(!t.home&&!t.previewReady))return;if(!force&&(SystemClock.uptimeMillis()-lastTouchTime<600||max(abs(controller.axes.rightX),abs(controller.axes.rightY))>.22f))return
  val key="${t.id}:${t.generation}:${v?.scrollX}:${v?.scrollY}:${content.width}:${content.height}";if(!force&&(previewPending||previewKey==key))return
  val scale=min(360f/content.width,480f/content.height);val width=max(1,(content.width*scale).roundToInt());val height=max(1,(content.height*scale).roundToInt());if(v!=null&&(v.width<1||v.height<1))return
  fun keep(bitmap:Bitmap){t.preview=bitmap;previewKey=key;if(!isPrivateSession)previewCache.save(t.id+"|"+if(t.home)"home"else t.url,bitmap);tabs.filter{it!==t&&it.preview!=null}.dropLast(15).forEach{it.preview=null}}
  if(!force&&v!=null&&v.isHardwareAccelerated&&!isPrivateSession){val position=IntArray(2);v.getLocationInWindow(position);val rect=Rect(position[0],position[1],position[0]+v.width,position[1]+v.height);if(rect.width()<1||rect.height()<1)return;val generation=t.generation;val x=v.scrollX;val y=v.scrollY;val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);previewPending=true;try{PixelCopy.request(window,rect,bitmap,{result->previewPending=false;if(result==PixelCopy.SUCCESS&&tab===t&&t.view===v&&t.generation==generation&&v.scrollX==x&&v.scrollY==y&&screen=="page"&&popupLayer==null&&!addressEditing)keep(bitmap)else bitmap.recycle()},handler)}catch(_:RuntimeException){previewPending=false;bitmap.recycle()};return}
  runCatching{val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);val target=v?:content;val canvas=Canvas(bitmap);canvas.scale(width.toFloat()/target.width,height.toFloat()/target.height);target.draw(canvas);keep(bitmap)}.onFailure{previewKey=key;diagnosticError="Preview capture failed: ${it.javaClass.simpleName}"}
 }
 private fun showPage(){
  resetArrival(content);clearTabDock()
  interactionEpoch++;hover.leave();tab?.lastUsed=SystemClock.elapsedRealtimeNanos()
  closeFind();closePopup();cancelAddress(false);rememberFocus()
  screen="page";chromeFocus=false;webEditing=false;nativeTyping=false;imeExpected=false;screenBack=null;stopInput();captureBinding=null;queryField=null;focused=null;focusNodes.clear();nativeScroll=null;nativeBody=null
  content.requestFocus();content.removeAllViews();toolbar.visibility=View.VISIBLE;tabNavigation.visibility=View.VISIBLE
  val t=tab
  if(t==null||t.home){showHome()}else if(t.loadError!=null){showPageError(t)}else {
   val v=t.view?:makeWeb(t).also {t.view=it;if(t.state!=null)it.restoreState(t.state!!)else it.loadUrl(t.url)}
   (v.parent as? ViewGroup)?.removeView(v);content.addView(v,FrameLayout.LayoutParams(-1,-1));v.onResume();addPointer()
  }
  updateChrome();if(t!=null&&!t.home&&t.loadError==null){focusPageNeutral();web?.evaluateJavascript("if(window.__pocketSurf){window.__pocketSurf.mode('${pageInputMode()}');window.__pocketSurf.resumeSelection();}",null)};saveSession()
 }
 private fun focusPageNeutral(clearSelection:Boolean=false){if(screen!="page"||tab?.home!=false||addressEditing||popupLayer!=null||video!=null)return;chromeFocus=false;focused=null;addressField.showSoftInputOnFocus=false;content.requestFocus();syncFocusMode();if(clearSelection){webHasSelection=false;nativeRootScroll=true;scrollRouteKnown=true};if(clearSelection)web?.evaluateJavascript("if(window.__pocketSurf){window.__pocketSurf.clearSelection();window.__pocketSurf.mode('${pageInputMode()}');}",null)}
 private fun addPointer(){(pointer.parent as? ViewGroup)?.removeView(pointer);content.addView(pointer,FrameLayout.LayoutParams(dp(10),dp(14)));pointer.visibility=if(mouse&&!touchMode)View.VISIBLE else View.GONE;pointer.translationX=pointerX;pointer.translationY=pointerY}
 private fun showPageError(t:Tab){
  val body=column().apply{setPadding(dp(32),dp(24),dp(32),dp(24));gravity=Gravity.CENTER_VERTICAL}
  body.addView(text("Can't open this page",25f,bold=true),margin(-1,-2,bottom=12));body.addView(text(t.loadError.orEmpty(),16f,theme.secondary),margin(-1,-2,bottom=10));body.addView(text(t.url,12f,theme.secondary).apply{maxLines=2;ellipsize=TextUtils.TruncateAt.END},margin(-1,-2,bottom=20))
  body.addView(actionChip("Try again","reload",0,0){retryPage(t)},margin(-1,50,bottom=10));body.addView(actionChip("Edit address","search",1,0){showSearch()},margin(-1,50,bottom=10));body.addView(actionChip("Start page","home",2,0){t.home=true;showPage()},margin(-1,50))
  nativeScroll=ScrollView(this).apply{isFillViewport=true;addView(body)};content.addView(nativeScroll,FrameLayout.LayoutParams(-1,-1));focusFirst()
 }
 private fun retryPage(t:Tab){if(t===tab)openUrl(t.url)}
 private fun pageProblem(t:Tab,message:String){diagnosticError=message;t.loadError=message;t.progress=100;if(t===tab&&screen=="page")showPage()}
 private fun showHome(){
  if(homeStore.homeMode=="blank"){content.setBackgroundColor(theme.background);return}
  val body=column().apply{setPadding(dp(28),dp(18),dp(28),dp(18));gravity=Gravity.CENTER_VERTICAL}
  val heading=row();heading.addView(text(if(isPrivateSession)"Private · Pinned bookmarks"else if(tabs.isEmpty())"PocketSurf · Pinned bookmarks"else"Pinned bookmarks",19f,bold=true),LinearLayout.LayoutParams(0,-2,1f))
  shortcutUndo?.let{removed->heading.addView(actionChip("Undo unpin","undo",0,3){homeStore.put(removed);shortcutUndo=null;showPage()},margin(-2,44))};body.addView(heading,margin(-1,-2,bottom=16))
  val shortcuts=homeStore.shortcuts();val cols=if(resources.configuration.screenWidthDp>=600)3 else 2
  repeat((6+cols-1)/cols){r->val line=row().apply{gravity=Gravity.TOP};repeat(cols){c->val i=r*cols+c;val shortcut=shortcuts.getOrNull(i);val card=column().apply{gravity=Gravity.CENTER;setPadding(dp(12),dp(12),dp(12),dp(12));contentDescription=shortcut?.let{"Pinned bookmark: ${it.title}"}?:"Pin bookmark ${i+1}";if(shortcut!=null)setTag(TAG_SHORTCUT,shortcut.id)
    if(shortcut==null)addView(icon("plus",24,theme.accent),margin(24,24,bottom=9))else addView(siteIcon(shortcut.url,30),margin(30,30,bottom=9))
    addView(text(shortcut?.title?:"Pin bookmark",13f,if(shortcut==null)theme.secondary else theme.text,true).apply{gravity=Gravity.CENTER;maxLines=1;ellipsize=TextUtils.TruncateAt.END})
    bindFocus(this,r,c,{if(shortcut==null)showShortcutPicker()else openUrl(shortcut.url)})
    if(shortcut!=null)setOnLongClickListener{shortcutActions(shortcut,this);true}
   };line.addView(card,LinearLayout.LayoutParams(0,dp(96),1f).apply{setMargins(0,0,if(c<cols-1)dp(12)else 0,dp(12))})};body.addView(line)}
  body.addView(actionChip("All bookmarks (${data.entries("bookmarks").size})","bookmark",(6+cols-1)/cols,0){collectionQueries.remove("bookmarks");showCollection("bookmarks")},margin(-1,40,top=4))
  nativeScroll=ScrollView(this).apply{isFillViewport=true;addView(body)};content.addView(nativeScroll,FrameLayout.LayoutParams(-1,-1));focusFirst()
 }
 private fun showShortcutPicker(){val body=screen("shortcut_picker","Pin a bookmark","Frequently visited sites, or popular sites when your history is empty",{showPage()});menuGrid(body,listOf(MenuItem("plus","Enter a website","Save and pin a bookmark"){shortcutForm()})+data.entries("bookmarks").filter{v->homeStore.shortcuts().none{it.url==v.url}}.map{v->MenuItem("bookmark",v.title,host(v.url),v.url){try{homeStore.put(v.title,v.url);showPage()}catch(e:Exception){toast(e.message?:"Six bookmarks are already pinned")}}}+homeStore.suggestions(data.entries("history")).map{v->MenuItem("globe",v.title,host(v.url),v.url){try{homeStore.put(v.title,v.url);favicons.fetch(v.url){refreshSiteIcons()};showPage()}catch(e:Exception){toast(e.message?:"Could not pin bookmark")}}},3)}
 private fun shortcutForm(existing:Shortcut?=null){editingShortcut=existing;val body=screen("shortcut_form",if(existing==null)"Add website"else"Edit pinned bookmark","HTTP or HTTPS address",{showPage()});val title=searchField("Name",existing?.title.orEmpty());val url=searchField("Website address",existing?.url.orEmpty());bindFocus(title,0,0);bindFocus(url,1,0);body.addView(title,margin(-1,50,bottom=12));body.addView(url,margin(-1,50,bottom=12));body.addView(actionChip("Save","check",2,0){try{val value=StartPageStore.requireUrl(if(url.text.toString().contains("://"))url.text.toString()else"https://${url.text}");homeStore.put(title.text.toString(),value,existing?.id);hideKeyboard();favicons.fetch(value){refreshSiteIcons()};showPage()}catch(e:Exception){toast(e.message?:"Check the address")}},margin(-1,50));focusFirst()}
 private fun shortcutActions(value:Shortcut,anchor:View?){openPopup("context","Context actions",value.title,listOf(MenuItem("globe","Open"){openUrl(value.url)},MenuItem("settings","Edit"){shortcutForm(value)},MenuItem("back","Move left"){val i=homeStore.shortcuts().indexOfFirst{it.id==value.id};homeStore.move(value.id,i-1);showPage()},MenuItem("forward","Move right"){val i=homeStore.shortcuts().indexOfFirst{it.id==value.id};homeStore.move(value.id,i+1);showPage()},MenuItem("trash","Unpin from start"){homeStore.remove(value.id);shortcutUndo=value;showPage()}),anchor=anchor)}

 private fun screen(kind:String,title:String,subtitle:String="",onBack:()->Unit={showPage()},headerActions:List<Pair<String,()->Unit>> = emptyList()):LinearLayout {
  clearTabDock();mousePreview=null;mousePreviewCursor=null;mouseTestSwitch=null;interactionEpoch++;hover.leave();closeFind();hideVideo();closePopup();cancelAddress(false);chromeFocus=false;webEditing=false;nativeTyping=false
  rememberFocus();if(kind=="confirm"||kind=="remap")nativeFocus.remove(kind)
  capturePreview();web?.onPause();stopInput();screen=kind;screenBack=onBack;captureBinding=null;queryField=null;focused=null;focusNodes.clear();content.removeAllViews();toolbar.visibility=View.GONE;tabNavigation.visibility=View.GONE;favoritesBar.visibility=View.GONE
  val page=column().apply {setBackgroundColor(theme.background);setPadding(dp(24),dp(16),dp(24),uiDp(20))}
  val header=row();val back=iconButton("back","Back"){this@BrowserActivity.back()};bindFocus(back,-1,0,{this@BrowserActivity.back()});header.addView(back,margin(48,48,right=16))
  header.addView(column().apply {addView(text(title,25f,bold=true).apply{maxLines=2;ellipsize=TextUtils.TruncateAt.END});if(subtitle.isNotBlank())addView(text(subtitle,12f,theme.secondary).apply{maxLines=1;ellipsize=TextUtils.TruncateAt.END;setPadding(0,dp(5),0,0)})},LinearLayout.LayoutParams(0,-2,1f))
  val secondaryActions=if(kind=="tabs"&&(resources.configuration.screenWidthDp<700||layoutScale>1.3f))headerActions.filter{it.first in listOf("history","close_all")}else emptyList()
  headerActions.filterNot{it in secondaryActions}.forEachIndexed {i,(name,f)->val label=when(name){"plus"->"New tab";"trash"->"Clear ${title.lowercase()}";"history"->"Recently closed tabs";"undo"->"Undo removal";"close_all"->"Close all tabs";else->name.replaceFirstChar{it.uppercase()}};val b=actionChip(label,if(name=="close_all")"trash"else name,-1,i+2,f);header.addView(b,margin(-2,48,left=8))}
  page.addView(header,margin(-1,-2,bottom=18))
  if(secondaryActions.isNotEmpty()){val actions=row();secondaryActions.forEachIndexed{i,(name,f)->val label=if(name=="history")"Recently closed tabs"else"Close all tabs";val chip=actionChip(label,if(name=="history")"history"else"trash",-1,i+3,f).apply{setPadding(dp(8),dp(6),dp(8),dp(6));val caption=getChildAt(1) as TextView;caption.textSize=12f;caption.maxLines=2;caption.layoutParams=LinearLayout.LayoutParams(0,-2,1f)};actions.addView(chip,LinearLayout.LayoutParams(0,uiDp(48),if(name=="history")1.35f else 1f).apply{if(i>0)leftMargin=dp(8)})};page.addView(actions,margin(-1,-2,bottom=12))}
  val body=column().apply {setPadding(0,dp(4),0,dp(12))};nativeBody=body
  nativeScroll=ScrollView(this).apply {isFillViewport=true;isVerticalScrollBarEnabled=false;isVerticalFadingEdgeEnabled=false;setFadingEdgeLength(0);clipToPadding=false;addView(body)};page.addView(nativeScroll,LinearLayout.LayoutParams(-1,0,1f));content.addView(page,FrameLayout.LayoutParams(-1,-1));updateLegend();animateArrival(page);return body
 }
 private fun menuGrid(body:LinearLayout,items:List<MenuItem>,cols:Int=2,startRow:Int=0){val count=if(resources.configuration.screenWidthDp/layoutScale<600)1 else cols;for(r in 0 until (items.size+count-1)/count){val line=row().apply{gravity=Gravity.TOP};for(c in 0 until count){val item=items.getOrNull(r*count+c);if(item!=null)line.addView(tile(item,r+startRow,c),LinearLayout.LayoutParams(0,-2,1f).apply {setMargins(0,0,if(c<count-1)dp(12)else 0,dp(12))})else line.addView(View(this),LinearLayout.LayoutParams(0,1,1f))};body.addView(line)};focusFirst()}
 private var menuContextItems=emptyList<MenuItem>()
 private var menuContextTitle=""
 private fun showMenu(){showMenu(emptyList())}
 private fun showMenu(contextItems:List<MenuItem>,contextTitle:String="",rect:RectF?=null,anchor:View?=null){
  if(screen!="page")return;if(popupKind=="menu"&&contextItems.isEmpty()){closePopup();return};menuContextItems=contextItems;menuContextTitle=contextTitle;val t=tab;val items=mutableListOf(MenuItem("bookmark","Bookmarks"){collectionQueries.remove("bookmarks");showCollection("bookmarks")},MenuItem("download","Downloads"){showDownloads()})
  if(!isPrivateSession)items.add(MenuItem("private","Private session"){startPrivate()})else items.add(MenuItem("private","End private session"){finish()})
  if(t!=null&&!t.home){items.add(MenuItem("search","Find in page"){showFind()});items.add(MenuItem("bookmark","Save bookmark"){saveBookmark(t.url,t.title);updateFavoritesBar();toast("Bookmark saved")});items.add(MenuItem("zoom-in","Reader view"){showReader()});items.add(MenuItem("shield","Website info and protection"){showWebsiteInfo()})}
  if(t!=null)items.add(MenuItem("close","Close current tab"){closeTab(active,false)})
  openPopup("menu","Browser menu",if(t?.home==false)host(t.url)else"PocketSurf",contextItems+items,rect,anchor)
 }

 private fun openPopup(kind:String,title:String,subtitle:String,items:List<MenuItem>,rect:RectF?=null,anchor:View?=null){
  if(kind=="context"){showMenu(items,subtitle,rect,anchor);return}
  interactionEpoch++;hover.leave();closePopup();if(addressEditing)cancelAddress(false);if(webEditing)leaveWebEditing();nativeTyping=false;imeExpected=false;hideKeyboard();stopInput();popupSavedNodes=focusNodes.toList();popupSavedFocus=focused;popupTarget=anchor;focusNodes=mutableListOf();popupKind=kind
  val host=window.decorView.findViewById<FrameLayout>(android.R.id.content);val contentXY=IntArray(2);val hostXY=IntArray(2);content.getLocationOnScreen(contentXY);host.getLocationOnScreen(hostXY)
  val layer=FrameLayout(this).apply{contentDescription="$title popup";importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO;if(Build.VERSION.SDK_INT>=28)accessibilityPaneTitle=title;setBackgroundColor(0x33000000);isClickable=true;setOnClickListener{closePopup()}};popupLayer=layer
  val targetRect=rect?.let{RectF(it).apply{offset((contentXY[0]-hostXY[0]).toFloat(),(contentXY[1]-hostXY[1]).toFloat())}}?:anchor?.let{val a=IntArray(2);val b=IntArray(2);it.getLocationOnScreen(a);host.getLocationOnScreen(b);RectF((a[0]-b[0]).toFloat(),(a[1]-b[1]).toFloat(),(a[0]-b[0]+it.width).toFloat(),(a[1]-b[1]+it.height).toFloat())}
  if(targetRect!=null&&targetRect.bottom>0&&targetRect.top<host.height){val highlight=View(this).apply{background=shape(Color.TRANSPARENT,5,theme.focus,3)};layer.addView(highlight,FrameLayout.LayoutParams(targetRect.width().toInt().coerceAtLeast(4),targetRect.height().toInt().coerceAtLeast(4)).apply{leftMargin=targetRect.left.toInt();topMargin=targetRect.top.toInt()})}
  fun category(item:MenuItem):String=if(kind=="menu")when(item.title){"Open tabs","New tab","Bookmarks","History","Downloads"->"Library";"Mouse mode","Element navigation"->"Privacy";else->if(item.icon in listOf("lock","shield","cookie","privacy","private"))"Privacy"else"Page"}else when{item.title in listOf("Edit","Remove","Unpin from start")||item.title.startsWith("Move ")->"Organize";item.title.startsWith("Share")||item.title.startsWith("Copy")->"Share";item.title.startsWith("Save")->"Save";item.title.contains("tab",true)->"Tabs";else->"Open"}
  val order=if(kind=="menu")listOf("Library","Page","Privacy")else listOf("Open","Tabs","Save","Share","Organize")
  val groups=(if(kind=="menu"&&menuContextItems.isNotEmpty())listOf(menuContextTitle.ifBlank{"Selected element"} to menuContextItems)else emptyList())+order.map{heading->heading to items.drop(if(kind=="menu")menuContextItems.size else 0).filter{category(it)==heading}}.filter{it.second.isNotEmpty()}
  val available=if(host.width>0)host.width else resources.displayMetrics.widthPixels
  val width=min(available-dp(24),dp(320)).coerceAtLeast(dp(240));val columns=1
  val panel=column().apply{contentDescription="$title panel";setPadding(dp(16),dp(14),dp(16),dp(14));background=shape(theme.background,18,theme.divider);elevation=dp(16).toFloat();isClickable=true}
  panel.addView(text(title,18f,bold=true),margin(-1,-2,bottom=4));if(subtitle.isNotBlank())panel.addView(text(subtitle,11f,theme.secondary).apply{maxLines=1;ellipsize=TextUtils.TruncateAt.END},margin(-1,-2,bottom=14))
  val sections=column();val stacks=(0 until columns).map{column()};val line=row().apply{gravity=Gravity.TOP};stacks.forEachIndexed{i,col->line.addView(col,LinearLayout.LayoutParams(0,-2,1f).apply{if(i<columns-1)rightMargin=dp(14)})};sections.addView(line)
  val rows=IntArray(columns)
  groups.forEachIndexed{index,(heading,actions)->val col=index%columns;val stack=stacks[col];stack.addView(text(heading,12f,theme.secondary,true),margin(-1,-2,top=if(rows[col]>0)8 else 0,bottom=6));actions.forEach{item->val r=rows[col]++;val cell=row().apply{contentDescription=item.title;minimumHeight=dp(36);setPadding(dp(10),dp(6),dp(10),dp(6));addView(icon(item.icon,18,theme.accent),margin(18,18,right=10));addView(text(item.title,13f).apply{maxLines=2;ellipsize=TextUtils.TruncateAt.END},LinearLayout.LayoutParams(0,-2,1f));bindFocus(this,r,col,{closePopup();item.action()})};stack.addView(cell,margin(-1,36,bottom=4))}}
  val maxHeight=max(dp(100),host.height-root.paddingTop-root.paddingBottom-footerHeight());panel.addView(ScrollView(this).apply{isFillViewport=false;addView(sections)},LinearLayout.LayoutParams(-1,0,1f))
  panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(maxHeight,View.MeasureSpec.EXACTLY))
  val x=available-root.paddingRight-width
  layer.addView(panel,FrameLayout.LayoutParams(width,-1).apply{topMargin=root.paddingTop;bottomMargin=root.paddingBottom+footerHeight();leftMargin=x.coerceAtLeast(root.paddingLeft)});host.addView(layer,FrameLayout.LayoutParams(-1,-1));val initial=focusNodes.firstOrNull()?.view;focused=initial;initial?.requestFocus();initial?.post{if(popupLayer===layer&&focused===initial)initial.requestFocus()};anchor?.foreground=shape(Color.TRANSPARENT,8,theme.focus,3);updateLegend()
  if(android.animation.ValueAnimator.areAnimatorsEnabled()){panel.alpha=0f;panel.translationX=width.toFloat();panel.animate().alpha(1f).translationX(0f).translationY(0f).setDuration(280).setInterpolator(android.view.animation.DecelerateInterpolator()).start()}
 }
 private fun closePopup(){val layer=popupLayer?:return;(layer.parent as? ViewGroup)?.removeView(layer);popupLayer=null;popupKind="";focusNodes=popupSavedNodes.toMutableList();focused=popupSavedFocus;syncFocusMode();popupSavedNodes=emptyList();popupSavedFocus=null;popupTarget?.foreground=shape(Color.TRANSPARENT,8,if(popupTarget?.isSelected==true)theme.accent else border());popupTarget=null;stopInput();if(!touchMode)focused?.requestFocus();if(chromeDirty){chromeDirty=false;updateChrome()};updateLegend()}

 private fun showSettings(){
  val body=screen("settings","Settings","",{showPage()})
  val groups=listOf(
   "Browsing" to listOf(MenuItem("theme","Appearance",prefs.getString("theme","Dark")!!){showAppearance()},MenuItem("search","Search engine",engineName()){showEngine()},MenuItem("home","Start page","New tabs and pinned bookmarks"){showHomeSettings()},MenuItem("bookmark","Bookmarks bar",if(prefs.getBoolean("favorites_bar",false))"Shown below the address"else"Hidden"){showBookmarksBarSettings()},MenuItem("zoom-in","Reading and accessibility","Text size and touch comfort"){showReadingSettings()}),
   "Privacy" to listOf(MenuItem("shield","Adblock",if(adblock.enabled)"Enabled"else"Disabled"){showAdblock()},MenuItem("cookie","Cookie consent",if(cookieConsent.enabled)"Necessary only on supported sites"else"Manual choices"){showCookieSettings()},MenuItem("privacy","Privacy and browsing data","History, cookies and website storage"){showPrivacy()}),
   "Controls" to listOf(MenuItem("controller","Controller","Remap buttons and choose labels"){showController()},MenuItem("mouse","Mouse","Movement speed and acceleration"){showMouseSettings()}),
   "Data" to listOf(MenuItem("download","Backup and restore","Export or import your personal setup"){showBackup()}),
   "About" to listOf(MenuItem("help","Help and controls","Navigation and troubleshooting"){showHelp()},MenuItem("info","About PocketSurf","Version ${BuildConfig.VERSION_NAME} · SilasDV"){showAbout()})
  )
  val wide=resources.configuration.screenWidthDp>=700&&resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE;val count=if(wide)3 else 1;val columns=(0 until count).map{column()};val line=row().apply{gravity=Gravity.TOP};columns.forEachIndexed{i,col->line.addView(col,LinearLayout.LayoutParams(0,-2,1f).apply{if(i<count-1)rightMargin=dp(18)})};body.addView(line);val rows=IntArray(count)
  groups.forEachIndexed{index,(heading,items)->val c=if(wide)when(index){0->0;1,3->1;else->2}else 0;val stack=columns[c];stack.addView(text(heading,14f,theme.accent,true).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,dp(12),0)},margin(-1,44,bottom=7));rows[c]++;items.forEach{item->val cell=row().apply{contentDescription=item.title;setPadding(dp(12),dp(9),dp(10),dp(9));addView(icon(item.icon,18,theme.accent),margin(18,18,right=10));addView(text(item.title,14f).apply{maxLines=1;ellipsize=TextUtils.TruncateAt.END},LinearLayout.LayoutParams(0,-2,1f));bindFocus(this,rows[c]++,c,item.action)};stack.addView(cell,margin(-1,44,bottom=7))}}

  focusFirst()
 }
 private fun showBookmarksBarSettings(){
  val body=screen("bookmarks_bar","Bookmarks bar","Your bookmarks below the address",{showSettings()})
  val toggle=Switch(this).apply{contentDescription="Show bookmarks bar while browsing";text=contentDescription;textSize=15f;setTextColor(theme.text);setPadding(dp(16),dp(8),dp(16),dp(8));isChecked=prefs.getBoolean("favorites_bar",false);thumbTintList=android.content.res.ColorStateList.valueOf(theme.accent);setOnCheckedChangeListener{_,enabled->prefs.edit().putBoolean("favorites_bar",enabled).apply()};bindFocus(this,0,0)}
  body.addView(toggle,margin(-1,54,bottom=16));body.addView(text("Pinned bookmarks appear first. All remaining bookmarks follow in a horizontally scrollable bar; none are hidden by a six-item limit.",14f,theme.secondary),margin(-1,-2,bottom=18))
  body.addView(actionChip("Manage bookmarks","bookmark",1,0){collectionQueries.remove("bookmarks");showCollection("bookmarks");screenBack={showBookmarksBarSettings()}},margin(260,48));focusFirst()
 }
 private fun showAppearance(){
  val body=screen("appearance","Appearance","Theme and accent are independent",{showSettings()});val base=BrowserTheme.baseName(prefs.getString("theme","Dark")!!);val accent=prefs.getString("accent",null)?:BrowserTheme.defaultAccentName(prefs.getString("theme","Dark")!!)
  body.addView(text("Theme",14f,theme.secondary,true),margin(-1,-2,bottom=10));val themes=row();BrowserTheme.names.forEachIndexed{i,name->val t=BrowserTheme.resolve(name,resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_YES,accent);val card=column().apply{contentDescription="$name theme";setPadding(dp(10),dp(8),dp(10),dp(8));addView(text("Aa",18f,t.text,true).apply{gravity=Gravity.CENTER;background=shape(t.background,7,t.divider)},margin(-1,44,bottom=8));addView(text(name,12f,theme.text,true).apply{gravity=Gravity.CENTER});bindFocus(this,0,i,{prefs.edit().putString("theme",name).putString("accent",accent).apply();applyTheme()},base==name)};themes.addView(card,LinearLayout.LayoutParams(0,dp(90),1f).apply{if(i<3)rightMargin=dp(10)})};body.addView(themes)
  body.addView(text("Accent colors",14f,theme.secondary,true),margin(-1,-2,top=22,bottom=12));val colors=row();BrowserTheme.accentNames.forEachIndexed{i,name->val swatch=column().apply{contentDescription="$name accent";setPadding(dp(5),dp(8),dp(5),dp(8));addView(View(this@BrowserActivity).apply{background=shape(BrowserTheme.accentColor(name,Color.luminance(theme.background)<.5),24)},margin(30,30,bottom=8));addView(text(name,11f).apply{gravity=Gravity.CENTER});gravity=Gravity.CENTER;bindFocus(this,1,i,{prefs.edit().putString("accent",name).apply();applyTheme()},accent==name)};colors.addView(swatch,LinearLayout.LayoutParams(0,dp(78),1f).apply{if(i<BrowserTheme.accentNames.lastIndex)rightMargin=dp(4)})};body.addView(colors);focusFirst()
 }

 private fun engineName()=SearchEngines.name(prefs)
 private fun showEngine(){val body=screen("engine","Search engine","Queries are sent only when you submit",{showSettings()});menuGrid(body,SearchEngines.entries.map{e->MenuItem("search",e.name,if(SearchEngines.resolve(prefs).id==e.id)"Selected"else"",e.search("")){SearchEngines.select(prefs,e);showEngine()}}+MenuItem("plus","Custom search engine","Use your own query URL"){showCustomEngine()},3)}
 private fun showCustomEngine(){val body=screen("custom_engine","Custom search engine","Use {query} where the search text belongs",{showEngine()});val name=searchField("Name");val url=searchField("https://example.com/search?q={query}");bindFocus(name,0,0);bindFocus(url,1,0);body.addView(name,margin(-1,50,bottom=12));body.addView(url,margin(-1,50,bottom=12));body.addView(actionChip("Save","check",2,0){try{SearchEngines.select(prefs,SearchEngines.custom(name.text.toString(),url.text.toString()));hideKeyboard();showEngine()}catch(e:Exception){toast(e.message?:"Check the query URL")}},margin(-1,50));focusFirst()}
 private fun showHomeUrl(){val b=screen("home_url","Start page address","HTTP or HTTPS address",{showHomeSettings()});val field=searchField("https://example.com",homeStore.homeUrl);bindFocus(field,0,0);b.addView(field,margin(-1,50,bottom=14));b.addView(actionChip("Save","check",1,0){try{homeStore.homeUrl=if(field.text.contains("://"))field.text.toString()else"https://${field.text}";homeStore.homeMode="custom";hideKeyboard();showHomeSettings()}catch(e:Exception){toast(e.message?:"Check the address")}},margin(-1,50));focusFirst()}
 private fun showHomeSettings(){val body=screen("home_settings","Start page","Choose what a new tab opens",{showSettings()});menuGrid(body,listOf(MenuItem("home","PocketSurf start page",if(homeStore.homeMode=="pocket")"Selected"else""){homeStore.homeMode="pocket";showHomeSettings()},MenuItem("globe","Blank page",if(homeStore.homeMode=="blank")"Selected"else""){homeStore.homeMode="blank";showHomeSettings()},MenuItem("search","Custom website",homeStore.homeUrl.ifBlank{"Choose an address"}){showHomeUrl()}),1)}
 private fun showAdblock(){val status=adblock.status();val body=screen("adblock","Adblock",if(adblock.enabled)"Protection enabled"else"Protection disabled",{showSettings()});body.addView(text("${status.source} · ${status.supportedRules} supported rules · ${status.blockedRequests} blocked requests this session",14f,theme.secondary),margin(-1,-2,bottom=14));status.error?.let{body.addView(text(it,14f,theme.error),margin(-1,-2,bottom=14))};menuGrid(body,listOf(MenuItem("shield",if(adblock.enabled)"Turn protection off"else"Turn protection on"){adblock.enabled=!adblock.enabled;web?.evaluateJavascript(adblock.cosmeticScript(tab?.url.orEmpty()),null);showAdblock()},MenuItem("reload",if(status.updating)"Updating filters…"else"Update filters","EasyList · explicit HTTPS download"){adblock.updateFilters{_,message->if(screen=="adblock")showAdblock();toast(message)};showAdblock()}),1);body.addView(text("Limited filter syntax; some ads and site-generated requests cannot be blocked. Video advertising is not reliably blocked. Old filters remain active if an update fails.",13f,theme.secondary),margin(-1,-2,top=14))}

 private fun showCookieSettings(){val body=screen("cookie_settings","Cookie consent","Automatically choose necessary cookies only",{showSettings()});menuGrid(body,listOf(MenuItem("cookie","Automatic rejection",if(cookieConsent.enabled)"Enabled"else"Disabled"){cookieConsent.enabled=!cookieConsent.enabled;web?.evaluateJavascript(cookieConsent.script(tab?.url.orEmpty()),null);showCookieSettings()})+(tab?.takeUnless{it.home}?.let{t->listOf(MenuItem("cookie",if(cookieConsent.isSiteExcluded(t.url))"Enable for this website"else"Choose manually on this website",host(t.url)){cookieConsent.toggleSiteException(t.url);web?.evaluateJavascript(cookieConsent.script(t.url),null);showCookieSettings()})}?:emptyList()),1);body.addView(text("Supported banners include Twitch, OneTrust, Cookiebot, CookieYes, iubenda, Didomi, Complianz and Usercentrics. Optional purposes are rejected; unknown banners stay available for your choice. Google/YouTube consent and some embedded content require manual choices. This does not erase cookies already stored.",14f,theme.secondary),margin(-1,-2,top=16))}
 private fun showController(){
  val body=screen("controller","Controller","D-pad / left stick selects · right stick scrolls",{showSettings()})
  val chips=row().apply{gravity=Gravity.TOP};chips.addView(actionChip("${if(bindings.playStation)"PlayStation"else"SNES"} labels","gamepad",0,0){bindings.playStation=!bindings.playStation;showController()},LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(0,0,dp(10),dp(14))});chips.addView(actionChip("Restore defaults","reload",0,1){confirm("Restore default controls?","Your custom assignments and button labels will be reset.",{bindings.reset();bindings.playStation=false;showController()},{showController()},"Restore defaults")},LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(0,0,0,dp(14))});body.addView(chips)
  Control.entries.forEachIndexed {i,a->val line=row().apply {setPadding(dp(18),dp(12),dp(18),dp(12));minimumHeight=dp(60);addView(text(a.title,15f,bold=true),LinearLayout.LayoutParams(0,-2,1f));addView(badge(a),margin(42,28,right=16));addView(icon("forward",16,theme.secondary));bindFocus(this,i+1,0,{showRemap(a)})};body.addView(line,margin(-1,-2,bottom=8))}
  focusFirst()
 }
 private var mousePreview:FrameLayout?=null
 private var mousePreviewCursor:MousePointer?=null
 private var mouseTestSwitch:Switch?=null
 private var mousePreviewX=0f
 private var mousePreviewY=0f
 private fun mouseSpeed()=prefs.getInt("mouse_speed",600).coerceIn(300,1800)
 private fun mouseVelocity(dt:Float):Pair<Float,Float>{val a=controller.axes;val moving=max(abs(a.leftX),abs(a.leftY))>.22f;mouseHeldSeconds=if(moving)mouseHeldSeconds+dt else 0f;val acceleration=if(prefs.getBoolean("mouse_acceleration",false))1f+.8f*(mouseHeldSeconds/.6f).coerceIn(0f,1f)else 1f;return BrowserLogic.velocity(a.leftX,a.leftY,.22f,mouseSpeed()*acceleration*resources.displayMetrics.density)}
 private fun setPreviewMouse(enabled:Boolean){if(mouse!=enabled){mouse=enabled;mouseHeldSeconds=0f;stopInput()};mouseTestSwitch?.isChecked=enabled;mousePreviewCursor?.visibility=if(enabled)View.VISIBLE else View.GONE;updateLegend()}
 private fun moveMousePreview(dt:Float){val area=mousePreview?:return;val cursor=mousePreviewCursor?:return;val v=mouseVelocity(dt);mousePreviewX=(mousePreviewX+v.first*dt).coerceIn(0f,max(0f,area.width-dp(10).toFloat()));mousePreviewY=(mousePreviewY+v.second*dt).coerceIn(0f,max(0f,area.height-dp(14).toFloat()));cursor.translationX=mousePreviewX;cursor.translationY=mousePreviewY}
 private fun showMouseSettings(){
  val body=screen("mouse_settings","Mouse","Adjust speed and test with the left stick",{showSettings()});val controls=column()
  controls.addView(text("Mouse movement speed",16f,bold=true),margin(-1,-2,bottom=6));val value=text("${(mouseSpeed()/6f).roundToInt()}%",14f,theme.secondary).apply{contentDescription="Mouse speed value"};controls.addView(value,margin(-1,-2,bottom=4))
  val slider=SeekBar(this).apply{contentDescription="Mouse movement speed";max=1500;progress=mouseSpeed()-300;keyProgressIncrement=50;progressTintList=android.content.res.ColorStateList.valueOf(theme.accent);thumbTintList=android.content.res.ColorStateList.valueOf(theme.accent);progressBackgroundTintList=android.content.res.ColorStateList.valueOf(theme.divider);bindFocus(this,0,0);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(bar:SeekBar,position:Int,fromUser:Boolean){val speed=300+position;prefs.edit().putInt("mouse_speed",speed).apply();value.text="${(speed/6f).roundToInt()}%"};override fun onStartTrackingTouch(bar:SeekBar){};override fun onStopTrackingTouch(bar:SeekBar){}})};controls.addView(slider,margin(-1,44,bottom=4));controls.addView(row().apply{addView(text("Slower",11f,theme.secondary),LinearLayout.LayoutParams(0,-2,1f));addView(text("Faster · Default 100%",11f,theme.secondary))},margin(-1,-2,bottom=8))
  val test=Switch(this).apply{text="Test mouse navigation";contentDescription=text;textSize=15f;setTextColor(theme.text);isChecked=mouse;bindFocus(this,1,0);setOnCheckedChangeListener{_,enabled->setPreviewMouse(enabled)}};mouseTestSwitch=test;controls.addView(test,margin(-1,48))
  val acceleration=Switch(this).apply{text="Mouse Acceleration";contentDescription=text;textSize=15f;setTextColor(theme.text);isChecked=prefs.getBoolean("mouse_acceleration",false);bindFocus(this,2,0);setOnCheckedChangeListener{_,enabled->prefs.edit().putBoolean("mouse_acceleration",enabled).apply()}};controls.addView(acceleration,margin(-1,48))
  val area=object:FrameLayout(this){override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){super.onSizeChanged(w,h,oldw,oldh);mousePreviewX=(w-dp(10)).coerceAtLeast(0)/2f;mousePreviewY=(h-dp(14)).coerceAtLeast(0)/2f;mousePreviewCursor?.translationX=mousePreviewX;mousePreviewCursor?.translationY=mousePreviewY}}.apply{contentDescription="Mouse test area";background=shape(theme.surface,14,theme.divider);clipChildren=true;addView(text("Move the left stick to test\n${bindings.label(Control.POINTER)} toggles mouse navigation",13f,theme.secondary).apply{gravity=Gravity.CENTER},FrameLayout.LayoutParams(-1,-1))};mousePreview=area;mousePreviewCursor=MousePointer(this).also{it.visibility=if(mouse)View.VISIBLE else View.GONE;area.addView(it,FrameLayout.LayoutParams(dp(10),dp(14)))}
  if(resources.configuration.screenWidthDp>=600){val layout=row().apply{gravity=Gravity.TOP;addView(controls,LinearLayout.LayoutParams(0,-2,1f).apply{rightMargin=dp(20)});addView(area,LinearLayout.LayoutParams(0,dp(210),1f))};body.addView(layout)}else{body.addView(controls);body.addView(area,margin(-1,160,top=12))};focusFirst();updateLegend()
 }

 private fun setMouseMode(value:Boolean){cancelAddress(false);closeFind();interactionEpoch++;hover.leave();stopInput();mouse=value;mouseHeldSeconds=0f;webHasSelection=false;webEditing=false;nativeTyping=false;imeExpected=false;hideKeyboard();chromeFocus=false;focused=null;content.requestFocus();web?.evaluateJavascript("if(window.__pocketSurf){window.__pocketSurf.clearSelection();window.__pocketSurf.releaseEditing();window.__pocketSurf.mode('${if(value)"mouse"else if(touchMode)"touch"else"controller"}');window.__pocketSurf.point(null);}",null);pointer.visibility=if(value&&!touchMode&&tab?.home==false)View.VISIBLE else View.GONE;if(value&&!touchMode&&tab?.home==false)web?.let{hover.move(it,pointerX,pointerY,true)};updateLegend()}
 private fun pageInputMode()=if(touchMode)"touch"else if(mouse)"mouse"else"controller"
 private fun onImeVisibilityChanged(visible:Boolean){if(imeWasVisible&&!visible){imeExpected=false;if(addressEditing){addressKeyboardDismissed=true;nativeTyping=false;stopInput();focused=addressField;addressField.showSoftInputOnFocus=false;syncFocusMode();updateLegend()}else if(webEditing)leaveWebEditing()else if(nativeTyping){nativeTyping=false;stopInput();(root.findFocus() as? EditText)?.let{focused=it};syncFocusMode();updateLegend()}};imeWasVisible=visible}
 private fun showRemap(action:Control,listening:Boolean=false){
  remapTarget=action
  val body=screen("remap","Remap ${action.title}",if(listening)"Press a button, or use Back to cancel"else"Choose how to assign a button",{showController()})
  if(!listening){menuGrid(body,listOf(MenuItem("controller","Record a button","Use your controller"){showRemap(action,true)},MenuItem("controller","Choose from list","Works with touch or controller"){showButtonPicker(action)}),1);return}
  body.addView(text("Listening for a button…",24f,bold=true),margin(-1,-2,top=20,bottom=16))
  body.addView(text("Your current Back button cancels recording. To assign that button, use Choose from list. Conflicting assignments are shown before you confirm.",15f,theme.secondary),margin(-1,-2,bottom=24))
  body.addView(actionChip("Cancel recording","back",0,0){showController()},margin(-1,50));captureBinding=action;updateLegend();focusFirst()
 }
 private fun showButtonPicker(action:Control){
  val body=screen("button_picker","Choose a button",action.title,{showRemap(action)})
  menuGrid(body,(Control.entries.map{it.defaultKey}+Control.entries.map{bindings.key(it)}+REMAPPABLE.sorted()).distinct().map{key->val other=bindings.action(key);MenuItem("controller",(if(key in 188..203)"Button ${key-187}"else bindings.labelForKey(key)).replaceFirstChar{it.uppercase()},other?.let{"Currently: ${it.title}"}?:"Not assigned"){assignButton(action,key)}},2)
 }
 private fun assignButton(action:Control,key:Int){
  captureBinding=null;val other=bindings.action(key)?.takeIf{it!=action}
  fun assign(){bindings.assign(action,key);showController();toast("${action.title}: ${bindings.label(action)}")}
  if(other==null)assign()else confirm("Swap button assignments?","${action.title} will use ${bindings.labelForKey(key)}. ${other.title} will use ${bindings.label(action)}.",{assign()},{showRemap(action)},"Swap buttons")
 }

 private fun showRightAxes(){val body=screen("axes","Right stick layout","Keep Automatic unless the right stick does not respond",{showController()});menuGrid(body,listOf(Triple("Automatic","Detect the usual controller layout","auto"),Triple("Standard layout","Right stick uses Android axes Z / RZ","z"),Triple("Alternate layout","For controllers using Android axes RX / RY","rx")).map{(name,detail,value)->MenuItem("controller",name,if(prefs.getString("right_axes","auto")==value)"Selected · $detail"else detail){prefs.edit().putString("right_axes",value).apply();showRightAxes()}},1)}
 private fun showSpeed(){val body=screen("speed","Scrolling","Constant speed above the deadzone; no acceleration",{showController()});menuGrid(body,listOf(300,600,900,1200).map{s->MenuItem("controller","$s px / second",if(prefs.getInt("speed",600)==s)"Selected"else""){prefs.edit().putInt("speed",s).apply();showSpeed()}}+listOf(.15f,.22f,.3f,.4f).map{d->MenuItem("controller","Deadzone ${(d*100).roundToInt()}%",if(abs(prefs.getFloat("deadzone",.22f)-d)<.001)"Selected"else""){prefs.edit().putFloat("deadzone",d).apply();showSpeed()}},2)}
 private fun showPrivacy(){val body=screen("privacy","Privacy and browsing data","Saved locally on this device",{showSettings()});menuGrid(body,listOf(
  MenuItem("history","Clear history","Remove visited-page records"){confirm("Clear browsing history?","This removes the history saved by PocketSurf.",{collectionUndo=null;data.write("history",emptyList());showPrivacy()},{showPrivacy()},"Clear history")},
  MenuItem("cookie","Clear cookies and site data","Sign out of websites and remove their storage"){confirm("Clear website data?","You will be signed out of websites.",{CookieManager.getInstance().removeAllCookies(null);WebStorage.getInstance().deleteAllData();web?.clearCache(true);showPrivacy()},{showPrivacy()},"Clear data")}
 ),1)}
 private fun undoTabs(){val batch=lastClosedBatch;if(batch.isEmpty())return;val ids=batch.map{it.id}.toSet();closed.removeAll{it.id in ids};for(t in batch)if(tabs.none{it.id==t.id})tabs.add(t.copy(view=null,state=null,restoreScroll=true));active=tabs.indexOfFirst{it.id==lastClosedActive}.takeIf{it>=0}?:tabs.lastIndex;lastClosedBatch=emptyList();saveSession();renderTabs(active)}
 private fun textZoom(url:String):Int=runCatching{JSONObject(prefs.getString("site_text_zoom","{}")).optInt(PageIdentity.origin(url)?:"",prefs.getInt("default_text_zoom",100))}.getOrDefault(prefs.getInt("default_text_zoom",100)).coerceIn(75,200)
 private fun setTextZoom(url:String,value:Int){val origin=PageIdentity.origin(url)?:return;val map=runCatching{JSONObject(prefs.getString("site_text_zoom","{}"))}.getOrDefault(JSONObject());map.put(origin,value.coerceIn(75,200));prefs.edit().putString("site_text_zoom",map.toString()).apply();if(PageIdentity.origin(tab?.url.orEmpty())==origin)web?.settings?.textZoom=value.coerceIn(75,200)}
 private fun showReadingSettings(){val body=screen("reading_settings","Reading and accessibility","Your compact layout stays the default",{showSettings()});body.addView(actionChip(if(prefs.getBoolean("comfort_touch",false))"Touch comfort: on"else"Touch comfort: off","check",0,0){prefs.edit().putBoolean("comfort_touch",!prefs.getBoolean("comfort_touch",false)).apply();recreate()},margin(-1,48,bottom=12));body.addView(text("Default website text size",16f,bold=true),margin(-1,-2,bottom=10));menuGrid(body,listOf(75,100,125,150,175,200).map{size->MenuItem("zoom-in","$size%",if(prefs.getInt("default_text_zoom",100)==size)"Selected"else""){prefs.edit().putInt("default_text_zoom",size).apply();tabs.forEach{t->t.view?.settings?.textZoom=textZoom(t.url)};showReadingSettings()}},3);body.addView(text("System font size is respected. Touch comfort enlarges controls; website-specific sizes are available in Website info.",13f,theme.secondary),margin(-1,-2,top=12));focusFirst()}
 private fun showBackup(){val body=screen("backup","Backup and restore","Bookmarks, Quick access, controller and appearance",{showSettings()});body.addView(text("Choose a local file. Backups exclude browsing history, open tabs, cookies, passwords and website storage.",14f,theme.secondary),margin(-1,-2,bottom=16));menuGrid(body,listOf(MenuItem("download","Export backup","Create a JSON file"){startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"PocketSurf-backup.json"),71)},MenuItem("download","Import backup","Validate and preview before applying"){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),72)}),2)}
 private fun saveBookmark(url:String,title:String){data.record(url,title,"bookmarks");if(isPrivateSession)try{contentResolver.call(Uri.parse("content://$packageName.bookmarks"),"save_bookmark",null,Bundle().apply{putString("url",url);putString("title",title)})}catch(_:Exception){toast("Bookmark is temporary; export it before ending this private session")}}
 private fun copyDiagnostics(){val provider=WebView.getCurrentWebViewPackage();val message="PocketSurf ${BuildConfig.VERSION_NAME}\nAndroid ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nWebView ${provider?.packageName.orEmpty()} ${provider?.versionName.orEmpty()}\nWebsite ${PageIdentity.host(tab?.url.orEmpty())}\nAdblock ${adblock.enabled} / site exception ${adblock.isSiteAllowed(tab?.url.orEmpty())}\nCookie automation ${cookieConsent.enabled}\nError ${diagnosticError.ifBlank{"none recorded"}}";getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("PocketSurf diagnostics",message));toast("Diagnostics copied; no form values or URL query included")}
 private fun showWebsiteInfo(){val t=tab?:return;if(t.home)return;val url=t.url;val body=screen("website_info","Website info",PageIdentity.origin(url)?:host(url),{showPage()});body.addView(text(if(t.loadError!=null)"Connection error — review the website address"else if(Uri.parse(url).scheme=="https")"HTTPS connection"else"Unencrypted HTTP connection",16f,bold=true),margin(-1,-2,bottom=12));menuGrid(body,listOf(MenuItem("shield",if(!adblock.enabled)"Adblock: globally off"else if(adblock.isSiteAllowed(url))"Adblock: off here"else"Adblock: on here","Change this website only"){if(!adblock.enabled){showAdblock();return@MenuItem};adblock.toggleSiteException(url);t.view?.evaluateJavascript(adblock.cosmeticScript(url),null);showWebsiteInfo()},MenuItem("cookie",if(!cookieConsent.enabled||cookieConsent.isSiteExcluded(url))"Cookie choices: manual"else"Cookie choices: automatic","Supported banners only"){if(!cookieConsent.enabled){showCookieSettings();return@MenuItem};cookieConsent.toggleSiteException(url);t.view?.evaluateJavascript(cookieConsent.script(url),null);showWebsiteInfo()},MenuItem("cookie","Reset this website","Cookies, cache and storage; related subdomains included"){resetWebsite(url)},MenuItem("help","Copy diagnostics","App, provider and error details"){copyDiagnostics()}),2);val cookieState=text("Cookie helper: checking status…",12f,theme.secondary);body.addView(cookieState,margin(-1,-2,top=8));val source=t.view;val generation=t.generation;source?.evaluateJavascript("window.__pocketSurfCookies ? JSON.stringify(window.__pocketSurfCookies.status()) : null"){raw->if(screen=="website_info"&&tab===t&&t.view===source&&t.generation==generation)try{val value=org.json.JSONTokener(raw).nextValue() as? String?:throw IllegalArgumentException();val status=JSONObject(value);cookieState.text=if(!status.optBoolean("enabled"))"Cookie helper: manual choices"else if(status.optInt("actions")>0)"Necessary-only choice sent (${status.optString("lastProvider")}); ${status.optString("outcome","choice-sent")} · website decides the result"else"No supported choice recorded; use the website dialog if visible"}catch(_:Exception){cookieState.text="Cookie helper status unavailable; manual choices remain possible"}}
  body.addView(text("${adblock.blockedFor(url)} requests blocked for this host in this session",12f,theme.secondary),margin(-1,-2,top=8));val zoom=row();listOf(-25,25).forEachIndexed{i,d->zoom.addView(actionChip(if(d<0)"Smaller text"else"Larger text","zoom-in",4,i){setTextZoom(url,textZoom(url)+d);showWebsiteInfo()},LinearLayout.LayoutParams(0,dp(48),1f).apply{if(i==0)rightMargin=dp(10)} )};body.addView(text("Website text: ${textZoom(url)}%",14f,theme.secondary),margin(-1,-2,top=12,bottom=8));body.addView(zoom);focusFirst()}
 private fun resetWebsite(url:String){if(!runCatching{androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DELETE_BROWSING_DATA)}.getOrDefault(false)){toast("Your WebView cannot reset one website safely. Update Android System WebView; other sites were not changed.");return};confirm("Reset ${host(url)}?","Cookies, cache and storage for this site and related subdomains will be removed. You may be signed out of those subdomains. Other sites are retained.",{try{if(androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DELETE_BROWSING_DATA))androidx.webkit.WebStorageCompat.deleteBrowsingDataForSite(WebStorage.getInstance(),url){toast("Website data removed");showPage();web?.reload()}else toast("Website reset is unavailable; other sites were not changed")}catch(_:RuntimeException){toast("Website reset failed; try updating Android System WebView")}},{showWebsiteInfo()},"Reset website")}
 private fun showReader(){val p=lease()?:return;p.view.evaluateJavascript("""(()=>{const root=document.querySelector('article')||document.querySelector('main');if(!root)return null;const copy=root.cloneNode(true);copy.querySelectorAll('script,style,nav,form,aside,header,footer').forEach(e=>e.remove());const text=(copy.innerText||copy.textContent||'').trim();return text.length>=300?JSON.stringify({title:document.title,text:text.slice(0,200000)}):null;})()"""){raw->if(!valid(p))return@evaluateJavascript;try{val decoded=org.json.JSONTokener(raw).nextValue() as? String?:throw IllegalArgumentException();val o=JSONObject(decoded);readerTitle=o.getString("title").take(200);readerText=o.getString("text");renderReader()}catch(_:Exception){toast("No readable article found on this page")}}}
 private fun renderReader(){val body=screen("reader","Reader",readerTitle,{showPage()});val size=prefs.getInt("reader_size",18).coerceIn(14,32);val controls=row();listOf(-2,2).forEachIndexed{i,delta->controls.addView(actionChip(if(delta<0)"Smaller text"else"Larger text",if(delta<0)"zoom-out"else"zoom-in",0,i){prefs.edit().putInt("reader_size",(size+delta).coerceIn(14,32)).apply();renderReader()},LinearLayout.LayoutParams(0,dp(48),1f).apply{if(i==0)rightMargin=dp(10)})};body.addView(controls,margin(-1,-2,bottom=16));body.addView(text(readerTitle,22f,bold=true),margin(-1,-2,bottom=16));body.addView(text(readerText,size.toFloat()).apply{setTextIsSelectable(true);setLineSpacing(dp(4).toFloat(),1.15f)},margin(-1,-2,bottom=20));focusFirst()}
 private fun showPrivateInfo(){val body=screen("private_info","Private session","Separate cookies and website storage",{showPage()});body.addView(text("History and open tabs are not saved by PocketSurf. Session data is cleared when this window closes. Downloads, exported files and bookmarks you explicitly save remain. Private browsing does not hide activity from websites or your network.",15f,theme.secondary),margin(-1,-2,bottom=20));body.addView(actionChip("End private session","private",0,0){finish()},margin(-1,48));focusFirst()}
 private fun startPrivate(){if(isPrivateSession){newTab();return};if(Build.VERSION.SDK_INT<28){toast("Private sessions require Android 9 or newer for isolated browser storage");return};startActivity(Intent(this,PrivateBrowserActivity::class.java))}
 private fun requestWebsitePermission(r:PermissionRequest){val p=lease();if(p==null||!resumed||!valid(p)||PageIdentity.origin(r.origin.toString())!=PageIdentity.origin(p.target.url)||r.origin.scheme!="https"){r.deny();return};val requested=r.resources.filter{it in listOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE,PermissionRequest.RESOURCE_AUDIO_CAPTURE)}.toTypedArray();if(requested.isEmpty()){r.deny();return};pendingPermission?.deny();pendingPermission=r;permissionLease=p;permissionEpoch=interactionEpoch;showWebPrompt(r.origin.toString(),"Requests "+requested.joinToString{if(it==PermissionRequest.RESOURCE_VIDEO_CAPTURE)"camera"else"microphone"}+". Allow for this page visit only?",{if(pendingPermission!==r||!valid(p)){r.deny();pendingPermission=null;return@showWebPrompt};val permissions=requested.map{if(it==PermissionRequest.RESOURCE_VIDEO_CAPTURE)android.Manifest.permission.CAMERA else android.Manifest.permission.RECORD_AUDIO}.filter{checkSelfPermission(it)!=android.content.pm.PackageManager.PERMISSION_GRANTED};if(permissions.isEmpty()){r.grant(requested);pendingPermission=null}else requestPermissions(permissions.toTypedArray(),73)},{r.deny();pendingPermission=null},"Allow once","Deny")}
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==73){val r=pendingPermission?:return;pendingPermission=null;if(permissionLease?.let{tab!==it.target||web!==it.view||it.target.generation!=it.generation}!=false||PageIdentity.origin(tab?.url.orEmpty())!=PageIdentity.origin(r.origin.toString())){r.deny();return};val allowed=r.resources.filter{(it==PermissionRequest.RESOURCE_VIDEO_CAPTURE&&checkSelfPermission(android.Manifest.permission.CAMERA)==android.content.pm.PackageManager.PERMISSION_GRANTED)||(it==PermissionRequest.RESOURCE_AUDIO_CAPTURE&&checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED)};if(allowed.isEmpty())r.deny()else r.grant(allowed.toTypedArray())}}
 override fun finish(){if(isPrivateSession&&!privateFinishing){privateFinishing=true;generatedTransfer?.close();handler.postDelayed({if(!isFinishing)super.finish()},3000);hover.leave();tabs.forEach{it.view?.stopLoading();it.view?.clearCache(true);it.view?.destroy();it.view=null};val done={runCatching{CookieManager.getInstance().removeAllCookies{runCatching{CookieManager.getInstance().flush()};super.finish()}}.onFailure{super.finish()};Unit};try{if(androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DELETE_BROWSING_DATA))androidx.webkit.WebStorageCompat.deleteBrowsingData(WebStorage.getInstance()){done()}else{WebStorage.getInstance().deleteAllData();done()}}catch(_:RuntimeException){runCatching{WebStorage.getInstance().deleteAllData()};done()}}else super.finish()}
 private fun showAbout(){val body=screen("about","About PocketSurf","${BuildConfig.VERSION_NAME} · by SilasDV · MIT",{showSettings()});body.addView(text("Built for browsing on a handheld.",24f,bold=true),margin(-1,-2,bottom=12));body.addView(actionChip("Default browser","globe",0,0){defaultBrowser()},margin(-1,48,bottom=10));body.addView(text(defaultBrowserStatus(),13f,theme.secondary),margin(-1,-2,bottom=14));body.addView(actionChip("Run setup again","setup",1,0){setupPending=true;startActivityForResult(Intent(this,SetupActivity::class.java),SETUP_REQUEST)},margin(-1,48,bottom=10));body.addView(actionChip("Copy diagnostics","share",2,0){copyDiagnostics()},margin(-1,44,bottom=10));body.addView(text("Open source, vibecoded with AI assistance.\nSystem WebView supplies the browser engine.",15f,theme.secondary),margin(-1,-2,bottom=24));menuGrid(body,listOf(MenuItem("github","GitHub","github.com/silasdv"){openUrl("https://github.com/silasdv")},MenuItem("kofi","Support development","ko-fi.com/silasdv"){openUrl("https://ko-fi.com/silasdv")}),2,3)}
 private fun defaultBrowserStatus():String=if(Build.VERSION.SDK_INT>=29){val r=getSystemService(RoleManager::class.java);when{!r.isRoleAvailable(RoleManager.ROLE_BROWSER)->"Unavailable on this Android device";r.isRoleHeld(RoleManager.ROLE_BROWSER)->"PocketSurf is your default browser";else->"Not set · choose PocketSurf"}}else "Choose in Android settings"
 private fun defaultBrowser(){try{startActivityForResult(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),8)}catch(_:Exception){if(Build.VERSION.SDK_INT>=29){val r=getSystemService(RoleManager::class.java);if(r.isRoleAvailable(RoleManager.ROLE_BROWSER)&&!r.isRoleHeld(RoleManager.ROLE_BROWSER)){startActivityForResult(r.createRequestRoleIntent(RoleManager.ROLE_BROWSER),8);return}};toast("Default-browser settings are unavailable on this device")}}

 private fun confirm(title:String,message:String,yes:()->Unit,no:()->Unit,confirmLabel:String="Continue"){
  val body=screen("confirm",title,"",no);confirmScreen={confirm(title,message,yes,no,confirmLabel)};body.addView(text(message,17f,theme.secondary),margin(-1,-2,top=20,bottom=28));val line=row().apply{gravity=Gravity.TOP}
  line.addView(actionChip("Cancel","back",0,0,no).apply{setPadding(dp(10),dp(10),dp(10),dp(10))},LinearLayout.LayoutParams(0,dp(54),1f).apply{setMargins(0,0,dp(12),0)})
  line.addView(actionChip(confirmLabel,"check",0,1,yes).apply{setPadding(dp(10),dp(10),dp(10),dp(10))},LinearLayout.LayoutParams(0,dp(54),1f));body.addView(line);focusFirst()
 }

 private fun searchField(hint:String,value:String=""):EditText=EditText(this).apply {
  setSingleLine();setTextColor(theme.text);setHintTextColor(theme.secondary);textSize=16f;setPadding(dp(18),dp(10),dp(18),dp(10));background=shape(theme.surface,14,border());this.hint=hint;setText(value);imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN;importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
 }
 private fun showSearch(target:Tab?=tab,onReturn:(()->Unit)?=null,typed:String?=null){
  if(addressEditing){if(addressKeyboardDismissed){addressKeyboardDismissed=false;imeExpected=true;addressField.showSoftInputOnFocus=true;addressField.requestFocus();getSystemService(InputMethodManager::class.java).showSoftInput(addressField,InputMethodManager.SHOW_IMPLICIT)};return}
  interactionEpoch++;hover.leave();closePopup();stopInput();webEditing=false;addressSavedNodes=focusNodes.toList();addressSavedFocus=focused;focusNodes=focusNodes.filter{it.row<=-2}.toMutableList();searchTarget=target;searchReturn=onReturn;addressEditing=true;addressKeyboardDismissed=false;chromeFocus=true;toolbar.visibility=View.VISIBLE;tabNavigation.visibility=View.VISIBLE;goButton.visibility=View.VISIBLE
  addressField.showSoftInputOnFocus=true;addressField.setText(typed?:target?.takeUnless{it.home}?.url.orEmpty());addressField.requestFocus();addressField.selectAll();focused=addressField;imeExpected=true;addressField.post{if(addressEditing){addressField.selectAll();getSystemService(InputMethodManager::class.java).showSoftInput(addressField,InputMethodManager.SHOW_IMPLICIT)}};showAddressSuggestions(addressField.text.toString());updateLegend()
 }
 private fun submitAddress(){if(!addressEditing)return;val value=addressField.text.toString().trim();if(value.isBlank())return;val target=searchTarget;cancelAddress(false);target?.let{val i=tabs.indexOf(it);if(i>=0)active=i};val candidate=BrowserLogic.address(value,"pocketsurf-search:");val result=if(candidate.startsWith("pocketsurf-search:"))SearchEngines.resolve(prefs).search(value)else candidate;openUrl(result)}
 private fun cancelAddress(restore:Boolean=true){if(!addressEditing)return;addressEditing=false;addressKeyboardDismissed=false;imeExpected=false;addressField.showSoftInputOnFocus=false;hideKeyboard();suggestions?.let{content.removeView(it)};suggestions=null;focusNodes=addressSavedNodes.toMutableList();focused=addressSavedFocus;syncFocusMode();addressSavedNodes=emptyList();addressSavedFocus=null;goButton.visibility=View.GONE;chromeFocus=false;val returning=searchReturn;searchReturn=null;if(restore&&returning!=null)returning();if(::addressText.isInitialized)updateNavigationState();if(chromeDirty){chromeDirty=false;updateChrome()};updateLegend()}
 private fun showAddressSuggestions(value:String){
  suggestions?.let{content.removeView(it)};focusNodes.removeAll{it.row>=0};val list=column().apply{setPadding(dp(12),dp(6),dp(12),dp(6));background=shape(theme.background,12,theme.divider);elevation=dp(12).toFloat()}
  val entries=(data.search("bookmarks",value)+data.search("history",value)).distinctBy{it.url}.take(4)
  val choices=mutableListOf(MenuItem("search","Search ${engineName()} for “$value”"){submitAddress()});choices.addAll(entries.map{v->MenuItem("history",v.title,host(v.url),v.url){val target=searchTarget;cancelAddress(false);target?.let{val i=tabs.indexOf(it);if(i>=0)active=i};openUrl(v.url)}})
  choices.forEachIndexed{i,item->val cell=row().apply{contentDescription=item.title+if(item.subtitle.isNotBlank())" · ${item.subtitle}"else"";setPadding(dp(10),dp(3),dp(10),dp(3));addView(icon(item.icon,18),margin(18,18,right=10));addView(column().apply{addView(text(item.title,12f).apply{maxLines=1;ellipsize=TextUtils.TruncateAt.END});if(item.subtitle.isNotBlank())addView(text(item.subtitle,10f,theme.secondary).apply{maxLines=1;ellipsize=TextUtils.TruncateAt.END})},LinearLayout.LayoutParams(0,-2,1f));bindFocus(this,i,0,item.action)};list.addView(cell,margin(-1,40))}
  val scroll=ScrollView(this).apply{addView(list)};suggestions=scroll;val maxHeight=min(dp(210),max(dp(80),(content.height*.65f).toInt()));content.addView(scroll,FrameLayout.LayoutParams(-1,min(maxHeight,dp(choices.size*40+12)),Gravity.TOP).apply{setMargins(dp(12),dp(4),dp(12),0)})
 }

 private fun watcher(change:(String)->Unit)=object:TextWatcher {override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){change(s.toString())};override fun afterTextChanged(s:Editable?){} }
 private fun hideKeyboard(){getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(root.windowToken,0)}
 private fun undoCollection(){
  val (key,visit)=collectionUndo?:return;val current=data.entries(key)
  val already=if(key=="bookmarks")current.any{it.url==visit.url}else visit in current
  if(!already)data.write(key,(current+visit).sortedByDescending{it.time})
  collectionUndoPin?.let{pin->runCatching{homeStore.put(pin)}.onFailure{toast("Bookmark restored; unpin another bookmark to restore its start-page pin")}};collectionUndoPin=null
  collectionUndo=null;nativeFocus[key]=1 to 0;focused=null;showCollection(key)
 }
 private fun showCollection(key:String){
  val title=if(key=="history")"History"else"Bookmarks"
  val actions=mutableListOf<Pair<String,()->Unit>>()
  if(collectionUndo?.first==key)actions.add("undo" to {undoCollection()})
  actions.add("trash" to {confirm("Clear ${title.lowercase()}?","This removes all saved ${title.lowercase()} entries.",{collectionUndo=null;data.write(key,emptyList());showCollection(key)},{showCollection(key)},"Clear ${title.lowercase()}")})
  val body=screen(key,title,if(key=="history")"Your browsing, organised by day"else"${data.entries(key).size} bookmarks · up to six pinned on your start page",{showPage()},actions)
  val field=searchField("Search ${title.lowercase()}",collectionQueries[key].orEmpty());queryField=field;bindFocus(field,0,0);body.addView(field,margin(-1,50,bottom=12))
  if(collectionUndo?.first==key)body.addView(text("Removed. Use Undo above to restore.",12f,theme.secondary),margin(-1,-2,bottom=12))
  val results=column();body.addView(results)
  fun remove(visit:Visit,index:Int){collectionQueries[key]=field.text.toString();collectionUndo=key to visit;collectionUndoPin=if(key=="bookmarks")homeStore.shortcuts().firstOrNull{it.url==visit.url}else null;data.write(key,data.entries(key).filterNot{it==visit});nativeFocus[key]=index to 0;focused=null;showCollection(key)}
  fun render(query:String){
   if(focusNodes.any{it.view===focused&&it.row>=1}){focused=field;field.requestFocus()}
   focusNodes.removeAll{it.row>=1};results.removeAllViews();val entries=data.search(key,query);var group="";var index=1
   if(entries.isEmpty())results.addView(emptyState(if(query.isBlank())"No ${title.lowercase()} yet"else"No matching pages",if(query.isBlank())"The pages you ${if(key=="history")"visit"else"save"} will appear here."else"Try another title or website."))
   entries.forEach{visit->val day=BrowserData.day(visit.time);if(key=="history"&&day!=group){group=day;results.addView(text(day.uppercase(),11f,theme.secondary,true),margin(-1,-2,top=12,bottom=10))}
    val rowIndex=index;val line=row();line.addView(tile(MenuItem(if(key=="history")"history"else"bookmark",visit.title.ifBlank{host(visit.url)},visit.url,visit.url){hideKeyboard();openUrl(visit.url)},rowIndex,0),LinearLayout.LayoutParams(0,-2,1f))
    if(key=="bookmarks"){val pin=homeStore.shortcuts().firstOrNull{it.url==visit.url};val label=if(pin!=null)"Unpin ${visit.title}"else"Pin ${visit.title}";val pinButton=actionChip(if(pin!=null)"Unpin"else"Pin","pin",rowIndex,1){try{if(pin!=null)homeStore.remove(pin.id)else homeStore.put(visit.title,visit.url);showCollection(key)}catch(_:IllegalArgumentException){toast("Six bookmarks are already pinned. Unpin one first; this bookmark remains saved.")}}.apply{contentDescription=label};line.addView(pinButton,margin(100,48,left=8))}
    val delete=iconButton("close","Remove ${visit.title}"){remove(visit,rowIndex)};bindFocus(delete,rowIndex,if(key=="bookmarks")2 else 1,{remove(visit,rowIndex)});line.addView(delete,margin(48,48,left=10));results.addView(line,margin(-1,-2,bottom=8));index++
   }
  }
  render(field.text.toString());field.addTextChangedListener(watcher{collectionQueries[key]=it;render(it)});field.setOnEditorActionListener{_,_,_->hideKeyboard();focusNodes.firstOrNull{it.row>0}?.view?.let{focused=it;it.requestFocus()};true};focusFirst();updateLegend()
 }

 private fun emptyState(title:String,subtitle:String)=column().apply {gravity=Gravity.CENTER;setPadding(dp(20),dp(36),dp(20),dp(36));addView(icon("globe",40,theme.secondary),margin(40,40,bottom=18));addView(text(title,22f,bold=true),margin(-2,-2,bottom=8));addView(text(subtitle,14f,theme.secondary))}
 private fun showDownloads(onBack:()->Unit={showPage()}){
  val body=screen("downloads","Downloads","Files stay here while you keep browsing",onBack)
  renderDownloads(body);downloadPoll=SystemClock.uptimeMillis()
 }
 private fun renderDownloads(body:LinearLayout){
  val items=downloads.items();val signature=items.toString()+downloadProblem;if(signature==downloadSignature&&body.childCount>0)return;downloadSignature=signature;val oldFocus=focused?.getTag(TAG_DOWNLOAD) as? String;val headerFocus=focusNodes.firstOrNull{it.view===focused&&it.row<0}?.view;val sy=nativeScroll?.scrollY?:0;focusNodes.removeAll{it.row>=0};focused=headerFocus;body.removeAllViews()
  downloadProblem?.let{problem->val notice=column().apply{setPadding(dp(18),dp(14),dp(18),dp(14));background=shape(theme.surface,14,theme.error)};notice.addView(text(problem,15f,theme.text),margin(-1,-2,bottom=12));val actions=row().apply{gravity=Gravity.TOP};downloadRetry?.let{retry->actions.addView(actionChip("Try again","reload",-0,0){retry()},LinearLayout.LayoutParams(0,dp(50),1f).apply{setMargins(0,0,dp(10),0)})};actions.addView(actionChip("Dismiss","close",0,1){downloadProblem=null;downloadRetry=null;renderDownloads(body)},LinearLayout.LayoutParams(0,dp(50),1f));notice.addView(actions);body.addView(notice,margin(-1,-2,bottom=14))}
  if(items.isEmpty())body.addView(emptyState("No downloads yet","Files you download will appear here, with their progress."))
  items.forEachIndexed {i,item->
   val card=column().apply {setPadding(dp(20),dp(16),dp(20),dp(16));background=shape(theme.surface,16,border())}
   val header=row();header.addView(icon("download",25,theme.accent),margin(25,25,right=14));header.addView(column().apply{addView(text(item.name,16f,bold=true).apply {maxLines=2;ellipsize=TextUtils.TruncateAt.END});addView(text(host(item.url),12f,theme.secondary).apply{setPadding(0,dp(5),0,0)})},LinearLayout.LayoutParams(0,-2,1f));header.addView(text(SurfDownloads.status(item),12f,if(item.status==DownloadManager.STATUS_FAILED)theme.error else theme.secondary),margin(-2,-2,left=16));card.addView(header)
   if(item.status in listOf(DownloadManager.STATUS_RUNNING,DownloadManager.STATUS_PENDING,DownloadManager.STATUS_PAUSED)){card.addView(object:View(this@BrowserActivity){private val paint=Paint(Paint.ANTI_ALIAS_FLAG);override fun onDraw(c:Canvas){super.onDraw(c);val radius=height/2f;paint.color=(theme.secondary and 0x00ffffff) or 0x33000000;c.drawRoundRect(0f,0f,width.toFloat(),height.toFloat(),radius,radius,paint);paint.color=theme.accent;val fraction=if(item.total>0)(item.received.toFloat()/item.total).coerceIn(0f,1f)else 0f;c.drawRoundRect(0f,0f,width*fraction,height.toFloat(),radius,radius,paint)}},margin(-1,4,top=16,bottom=8))}
   val actions=row().apply{gravity=Gravity.END;setPadding(0,dp(10),0,0)}
   fun add(label:String,icon:String,col:Int,f:()->Unit){val v=actionChip(label,icon,i+1,col,f);v.setTag(TAG_DOWNLOAD,"${item.id}:$label");actions.addView(v,margin(-2,48,left=8));if(oldFocus?.substringBefore(':')==item.id.toString())focused=v}
   when(item.status){DownloadManager.STATUS_SUCCESSFUL->add("Open file","forward",0){try{downloads.open(item)}catch(e:Exception){downloadProblem="The file could not be opened. It may be missing, or no compatible viewer is installed. The download list remains available.";downloadRetry=null;renderDownloads(body)}};DownloadManager.STATUS_RUNNING,DownloadManager.STATUS_PENDING,DownloadManager.STATUS_PAUSED->add("Cancel","close",0){downloads.cancel(item.id);if(item.localUri.isNotBlank())generatedTransfer?.close();renderDownloads(body)};else->add("Retry","reload",0){try{if(item.localUri.isNotBlank()){toast("Generate this file again on the original website");return@add};downloads.retry(item,userAgent());renderDownloads(body)}catch(e:Exception){downloadProblem="The download could not restart. Check your internet connection and free storage, then try Retry again.";downloadRetry=null;renderDownloads(body)}}}
   card.addView(actions);body.addView(card,margin(-1,-2,bottom=12))
  }
  if(headerFocus!=null){focused=headerFocus;headerFocus.requestFocus()}else if(focused==null)focusFirst()else focused?.requestFocus();nativeScroll?.post{nativeScroll?.scrollTo(0,sy)}
 }
 private fun rememberFocus(){focusNodes.firstOrNull{it.view===focused}?.let{nativeFocus[screen]=it.row to it.col}}
 private fun activateNative(){
  val v=focused?:return
  val activeLayer=popupLayer?:root;var ancestor:View?=v;while(ancestor!=null&&ancestor!==activeLayer)ancestor=ancestor.parent as? View
  if(v===addressField){showSearch();return}
  if(ancestor!==activeLayer||focusNodes.none{it.view===v}||!v.isEnabled){focused=null;focusFirst();return}
  val visible=Rect();val shown=v.getGlobalVisibleRect(visible)
  if(!shown||visible.height()<min(v.height,nativeScroll?.height?:v.height)-dp(2)||visible.width()<v.width-dp(2)){
   v.requestRectangleOnScreen(Rect(0,0,v.width,v.height),true);v.requestFocus();toast("Selection brought into view. Press ${bindings.label(Control.CONFIRM)} to activate.");return
  }
  if(v is EditText){nativeTyping=true;imeExpected=true;stopInput();v.requestFocus();getSystemService(InputMethodManager::class.java).showSoftInput(v,InputMethodManager.SHOW_IMPLICIT)}else v.performClick()
 }
 private fun restoreDestination(destination:String){when(destination){
  "mouse_settings"->showMouseSettings();"bookmarks_bar"->showBookmarksBarSettings();"cookie_settings"->showCookieSettings();"private_info"->showPrivateInfo();"reading_settings"->showReadingSettings();"backup"->showBackup();"website_info"->showWebsiteInfo();"reader"->renderReader();"home_settings"->showHomeSettings();"adblock"->showAdblock();"shortcut_picker"->showShortcutPicker();"settings"->showSettings();"appearance"->showAppearance();"controller"->showController();"history"->showCollection("history");"bookmarks"->showCollection("bookmarks");"downloads"->showDownloads();"tabs"->renderTabs();"closed"->showClosed();"menu"->{showPage();showMenu()};"engine"->showEngine();"axes"->showRightAxes();"speed"->showSpeed();"privacy"->showPrivacy();"about"->showAbout();"help"->showHelp();else->showPage()
 }}
 private fun applyTheme(){
  val destination=screen;val oldBack=screenBack;val typed=queryField?.text?.toString();val target=searchTarget;val returning=searchReturn;val wasAddress=addressEditing;val addressDraft=if(wasAddress)addressField.text.toString()else null;if(wasAddress)cancelAddress(false)
  val formDrafts=if(destination in listOf("shortcut_form","home_url","custom_engine"))focusNodes.map{it.view}.filterIsInstance<EditText>().map{Triple(it.text.toString(),it.selectionStart,it.selectionEnd)}else emptyList()
  val formTyping=nativeTyping;val formFocus=focusNodes.map{it.view}.filterIsInstance<EditText>().indexOf(focused)
  theme.applyPlatform(this)
  root.setBackgroundColor(theme.background);content.setBackgroundColor(theme.background);tabNavigation.setBackgroundColor(theme.background);window.statusBarColor=theme.background;window.navigationBarColor=theme.background
  window.decorView.systemUiVisibility=if(Color.luminance(theme.background)>.5)View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
  val findIndex=root.indexOfChild(findBar);root.removeView(findBar);buildFindBar();root.addView(findBar,findIndex,margin(-1,40));val index=root.indexOfChild(toolbar);root.removeView(toolbar);buildToolbar();root.addView(toolbar,index,margin(-1,40));tabStrip.setBackgroundColor(theme.background)
  when(destination){"search"->showSearch(target,returning,typed);"downloads"->showDownloads(oldBack?:{showMenu()});"remap"->remapTarget?.let{showRemap(it,captureBinding!=null)};"shortcut_form"->shortcutForm(editingShortcut);"home_url"->showHomeUrl();"custom_engine"->showCustomEngine();"confirm"->confirmScreen?.invoke();"button_picker"->remapTarget?.let{showButtonPicker(it)};else->restoreDestination(destination)}
  val newFields=focusNodes.map{it.view}.filterIsInstance<EditText>()
  formDrafts.zip(newFields).forEach{(draft,field)->field.setText(draft.first);field.setSelection(draft.second.coerceIn(0,field.length()),draft.third.coerceIn(0,field.length()))}
  if(formDrafts.isNotEmpty()&&formTyping)newFields.getOrNull(formFocus)?.let{field->focused=field;nativeTyping=true;imeExpected=true;field.requestFocus();getSystemService(InputMethodManager::class.java).showSoftInput(field,InputMethodManager.SHOW_IMPLICIT)}
  updateChrome();if(wasAddress)showSearch(target,returning,addressDraft)
 }
 private fun showHelp(){
  val body=screen("help","Help and controls","Quick answers for browsing on a handheld",{showSettings()})
  listOf("Browse with a controller" to "D-pad / left stick selects links and controls. The right stick scrolls at a constant speed. If a page cannot be navigated as elements, switch to Mouse mode with ${bindings.label(Control.POINTER)}.",
   "Search and tabs" to "${bindings.label(Control.SEARCH)} focuses address/search. ${bindings.label(Control.TABS)} opens tab cards. ${bindings.label(Control.PREVIOUS_TAB)} / ${bindings.label(Control.NEXT_TAB)} select a card; ${bindings.label(Control.CONFIRM)} opens it. ${bindings.label(Control.CONTEXT)} creates a new tab. In the overview, ${bindings.label(Control.HISTORY_FORWARD)} closes the selected card immediately; Undo restores it. ${bindings.label(Control.TABS)} returns to browsing. ${bindings.label(Control.MENU)} opens Settings; ${bindings.label(Control.SEARCH)} focuses the address/search field.",
   "Go back or leave" to "${bindings.label(Control.BACK)} closes the keyboard or current layer first. On a page ${bindings.label(Control.BACK)} closes the current tab, or exits with no tabs. Android Back opens the browser menu. Use ${bindings.label(Control.HISTORY_BACK)} / ${bindings.label(Control.HISTORY_FORWARD)} for page history. Hold ${bindings.label(Control.CONFIRM)} on a link, image or pinned bookmark for its options.",
   "Change your controller" to "Settings → Controller remaps actions and switches SNES/PlayStation labels. Your current Back button cancels recording. Choose from list can assign that button too. The right stick is detected automatically; no technical axis settings are required.",
   "Downloads and problems" to "Menu → Downloads shows progress, Cancel, Retry and Open file. Opening a completed file may use an installed viewer. HTTP(S) file links are supported; Blob files can be saved through Android’s file picker (64 MB limit).",
   "Touch and known limits" to "Use the tab strip, address bar, menu icons and on-screen Back controls directly. Menus scroll normally by touch. Cross-origin frames, canvas content and some custom controls may need Mouse mode. Camera and microphone requests ask per page visit and also need Android permission.").forEach{(title,copy)->body.addView(text(title,17f,bold=true),margin(-1,-2,top=14,bottom=8));body.addView(text(copy,14f,theme.secondary).apply{setLineSpacing(dp(3).toFloat(),1f)},margin(-1,-2,bottom=12))};focusFirst()
 }
 private fun userAgent()=web?.settings?.userAgentString?:WebSettings.getDefaultUserAgent(this)
 private fun download(url:String,ua:String=userAgent(),disposition:String?=null,mime:String?=null){
  if(url.startsWith("blob:")||url.startsWith("data:")){if(!resumed||pendingGenerated!=null){toast("Finish the current file selection first");return};val p=lease()?:return;val origin=PageIdentity.origin(p.target.url)?:return;if(url.startsWith("blob:")&&!url.startsWith("blob:$origin/")){toast("Generated file belongs to another website");return};if(url.startsWith("data:")&&url.length>524288){toast("Embedded file address is too large; use the website's normal download link");return};val type=mime?:"application/octet-stream";val name=URLUtil.guessFileName(url,disposition,type).replace(Regex("[\\\\/:*?\"<>|]"),"_").take(120);pendingGenerated=PendingGenerated(p,url,name,type);startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(type).putExtra(Intent.EXTRA_TITLE,name),74);return}
  try {downloads.start(url,ua,disposition,mime,PageIdentity.referer(web?.url.orEmpty(),url));downloadProblem=null;downloadRetry=null;if(screen=="downloads")nativeBody?.let{renderDownloads(it)}else toast("Download started · available in Downloads")}
  catch(e:Exception){downloadProblem=if(e is IllegalArgumentException)"This download type is not supported. Try a direct HTTP or HTTPS file link from the website."else"The download could not start. Check your connection and free storage, then try again.";downloadRetry=if(e is IllegalArgumentException)null else ({download(url,ua,disposition,mime)});showDownloads{showPage()}}
 }
 private fun showTabs(){tabQuery="";capturePreview();returnFromTabs=tab;selected=active.coerceAtLeast(0);renderTabs()}
 private fun renderTabs(focusTabAfterClose:Int?=null){
  jumpHints.clear()
  val body=screen("tabs","Open tabs","${tabs.size} ${if(tabs.size==1)"tab"else"tabs"} · choose a page to continue",{showPage()})
  installTabDock()
  if(tabs.isEmpty()){body.addView(emptyState("A fresh start","Open a new tab to begin browsing."));focusNodes.firstOrNull{it.view.contentDescription=="New tab"}?.view?.requestFocus();return}
  val search=if(tabs.size>8)searchField("Search tabs by title or website",tabQuery).also{field->field.inputType=android.text.InputType.TYPE_CLASS_TEXT;field.imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH or android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN;field.contentDescription="Search tabs";queryField=field;bindFocus(field,-1,7);body.addView(field,margin(-1,46,bottom=12))}else null
  val horizontal=tabs.size>3
  val cards=column();val strip=if(horizontal)HorizontalScrollView(this).apply{contentDescription="Horizontal tab overview";isHorizontalScrollBarEnabled=false;isFillViewport=false;clipToPadding=false;addView(cards)}else null
  body.addView(strip?:cards,margin(-1,-2))
  val cardWidth=dp(((resources.configuration.screenWidthDp-76)/3).coerceIn(180,320))
  val cols=if(horizontal)Int.MAX_VALUE else if(resources.configuration.screenWidthDp>=700)3 else 2
  fun drawCards(){focusNodes.removeAll{it.view.getTag(TAG_TAB)!=null||it.view.getTag(TAG_CLOSE)!=null};cards.removeAllViews();val visible=tabs.withIndex().filter{it.value.title.contains(tabQuery,true)||it.value.url.contains(tabQuery,true)}
  if(visible.isEmpty())cards.addView(emptyState("No matching tabs","Try a title or website."))
  visible.forEachIndexed {position,entry->val i=entry.index;val t=entry.value
   if(position%cols==0)cards.addView(row().apply{gravity=Gravity.TOP})
   val line=cards.getChildAt(cards.childCount-1) as LinearLayout
   val card=column().apply {clipToOutline=true;contentDescription="${if(i==active)"Current tab: "else"Tab: "}${t.title}";setTag(TAG_TAB,i)}
   val preview=object:FrameLayout(this@BrowserActivity){override fun draw(canvas:Canvas){val saved=canvas.save();val r=dp(14).toFloat();val path=Path().apply{addRoundRect(RectF(0f,0f,width.toFloat(),height.toFloat()),floatArrayOf(r,r,r,r,0f,0f,0f,0f),Path.Direction.CW)};canvas.clipPath(path);super.draw(canvas);canvas.restoreToCount(saved)}}.apply {setBackgroundColor(theme.surface)}
   if(t.preview==null&&!isPrivateSession)t.preview=previewCache.load(t.id+"|"+if(t.home)"home"else t.url)
   if(t.preview!=null)preview.addView(ImageView(this).apply{setImageBitmap(t.preview);scaleType=ImageView.ScaleType.FIT_CENTER},FrameLayout.LayoutParams(-1,-1))else preview.addView(column().apply {gravity=Gravity.CENTER;addView(if(t.home)icon("home",32,theme.accent)else siteIcon(t.url,32),margin(36,36,bottom=10));addView(text(if(t.home)"New tab"else"Preview available after loading",13f,theme.secondary).apply{gravity=Gravity.CENTER})},FrameLayout.LayoutParams(-1,-1))
   val close=iconButton("close","Close ${t.title}"){closeTab(i,true)};close.setTag(TAG_CLOSE,i);close.isFocusable=false;close.isFocusableInTouchMode=false;(close.getChildAt(0) as ImageView).layoutParams=FrameLayout.LayoutParams(dp(14),dp(14),Gravity.CENTER);preview.addView(close,FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP or Gravity.END).apply {setMargins(dp(8),dp(8),dp(8),dp(8))})
   val jump=jumpHint(i);preview.addView(jump,FrameLayout.LayoutParams(-2,dp(9),Gravity.BOTTOM or Gravity.END).apply{rightMargin=dp(8);bottomMargin=dp(8)});card.addView(preview,margin(-1,142))
   val footer=column().apply {setPadding(dp(16),dp(13),dp(16),dp(14));addView(text(if(t.home)"New tab"else t.title,15f,bold=true).apply {maxLines=1;ellipsize=TextUtils.TruncateAt.END});addView(text(if(i==active)"Current tab"else if(t.home)"PocketSurf"else host(t.url),11f,if(i==active)theme.accent else theme.secondary).apply {setPadding(0,dp(6),0,0)})};card.addView(footer)
   bindFocus(card,position/cols,(position%cols)*2,{switchTab(i)},i==active);line.addView(card,LinearLayout.LayoutParams(if(horizontal)cardWidth else 0,-2,if(horizontal)0f else 1f).apply{setMargins(0,0,if(position%cols<cols-1)dp(14)else 0,dp(14))})
   if(!horizontal&&position==visible.lastIndex)repeat(cols-1-position%cols){line.addView(View(this),LinearLayout.LayoutParams(0,1,1f))}
  }
  };drawCards();search?.addTextChangedListener(watcher{tabQuery=it;drawCards()})
  val target=focusNodes.firstOrNull{it.view.getTag(TAG_TAB)==(focusTabAfterClose?:active)}?.view?:focusNodes.firstOrNull{it.view.getTag(TAG_TAB)!=null}?.view?:focusNodes.firstOrNull{it.view.contentDescription=="New tab"}?.view;target?.let{focused=it;it.requestFocus();it.post{if(focused===it){it.requestFocus();it.requestRectangleOnScreen(Rect(0,0,it.width,it.height),true)}}}?:focusFirst();updateLegend()
 }
 private fun clearTabDock(){tabDock?.let{(it.parent as? ViewGroup)?.removeView(it)};tabDock=null;if(::content.isInitialized)(content.layoutParams as FrameLayout.LayoutParams).bottomMargin=footerHeight()}
 private fun installTabDock(){val dock=column().apply{setPadding(dp(12),dp(4),dp(12),dp(4));setBackgroundColor(theme.background)};tabDock=dock;val items=listOf(MenuItem("undo","Undo"){undoTabs()},MenuItem("plus","New tab"){newTab()},MenuItem("history","Recently closed tabs"){showClosed()},MenuItem("close_all","Close all tabs"){closeAllTabs()});val cols=if(resources.configuration.screenWidthDp<600)2 else 4;items.chunked(cols).forEachIndexed{r,group->val row=row();group.forEachIndexed{c,item->val button=actionChip(item.title,item.icon,10000+r,c,item.action);button.contentDescription=if(item.icon=="history")"Recently closed tabs"else item.title;button.tooltipText=button.contentDescription;if(item.title=="New tab"){val glyph=button.getChildAt(0);button.removeView(glyph);val glyphHost=FrameLayout(this);glyphHost.addView(glyph,FrameLayout.LayoutParams(dp(18),dp(18),Gravity.CENTER));glyphHost.addView(inlineHint(Control.CONTEXT),cornerParams(hintWidth(Control.CONTEXT)));button.addView(glyphHost,0,margin(36,32,right=8))};if(item.icon=="undo"&&lastClosedBatch.isEmpty()){button.isEnabled=false;button.visibility=View.INVISIBLE;focusNodes.removeAll{it.view===button}};if(item.icon=="close_all"&&tabs.isEmpty()){button.isEnabled=false;button.visibility=View.INVISIBLE;focusNodes.removeAll{it.view===button}};row.addView(button,LinearLayout.LayoutParams(0,-1,1f).apply{if(c>0)leftMargin=dp(8)})};dock.addView(row,LinearLayout.LayoutParams(-1,0,1f).apply{if(r>0)topMargin=dp(4)})};stage.addView(dock,FrameLayout.LayoutParams(-1,dockHeight(),Gravity.BOTTOM).apply{bottomMargin=footerHeight()});updateLegend()}
 private fun closeAllTabs(){
  if(tabs.isEmpty())return
   stopInput();lastClosedActive=tab?.id.orEmpty();lastClosedBatch=tabs.map{captureTab(it);it.copy(view=null,state=null,preview=null)};tabs.forEach{t->closed.addLast(t.copy(view=null,state=null,preview=null));(t.view?.parent as? ViewGroup)?.removeView(t.view);t.view?.destroy()};while(closed.size>500)closed.removeFirst();tabs.clear();active=-1;selected=0;returnFromTabs=null;saveSession();renderTabs()
 }
 private fun showClosed(){val body=screen("closed","Recently closed","Restore the pages you closed",{renderTabs()});if(closed.isEmpty())body.addView(emptyState("No recently closed tabs","Closed tabs from this session appear here."))else menuGrid(body,closed.reversed().map{t->MenuItem("history",t.title,host(t.url)){closed.remove(t);lastClosedBatch=lastClosedBatch.filter{it.id!=t.id};tabs.add((active+1).coerceIn(0,tabs.size),t.copy(view=null,state=null,restoreScroll=true));active=(active+1).coerceAtMost(tabs.lastIndex);showPage()}},1)}
 private fun newTab(){capturePreview();stopInput();tabs.add((active+1).coerceIn(0,tabs.size),if(homeStore.homeMode=="custom")Tab(url=homeStore.homeUrl,title=host(homeStore.homeUrl),home=false)else Tab());active=(active+1).coerceAtMost(tabs.lastIndex);showPage();if(tab?.home==true)focusChrome();animateArrival(content)}
 private fun switchTab(index:Int){if(index !in tabs.indices)return;if(index==active&&screen=="page")return;tab?.let{t->captureTab(t);val v=t.view;val generation=t.generation;v?.evaluateJavascript("Array.from(document.querySelectorAll('video,audio')).some(m=>!m.paused&&!m.ended)"){value->if(t.view===v&&t.generation==generation)t.mediaProtected=value=="true"}};capturePreview();stopInput();web?.evaluateJavascript("window.__pocketSurf && window.__pocketSurf.suspendSelection()",null);web?.onPause();active=index;showPage();evict()}
 private fun closeTab(index:Int,overview:Boolean){if(index in tabs.indices){stopInput();destroyTab(index,overview)}}
 private fun destroyTab(index:Int,overview:Boolean){if(index !in tabs.indices)return;val old=tab;val t=tabs[index];if(t.transferActive)generatedTransfer?.close();captureTab(t);tabs.removeAt(index);lastClosedActive=old?.id.orEmpty();lastClosedBatch=listOf(t.copy(view=null,state=null,preview=null));closed.addLast(t.copy(view=null,state=null,preview=null));while(closed.size>500)closed.removeFirst();(t.view?.parent as? ViewGroup)?.removeView(t.view);t.view?.destroy();active=if(tabs.isEmpty())-1 else if(old===t)index.coerceAtMost(tabs.lastIndex)else tabs.indexOf(old).coerceAtLeast(0);selected=index.coerceAtMost(max(0,tabs.lastIndex));saveSession();if(overview)renderTabs(selected)else showPage()}
 private fun openUrl(url:String,new:Boolean=false){
  hideKeyboard();if(new||tab==null)newTab();val t=tab?:return;val needsNavigation=t.view!=null||t.state!=null
  t.restoreScroll=false;t.historyReplay=false;t.scrollX=0;t.scrollY=0;t.preview=null;t.previewReady=false;t.generation++;t.formProtected=false;t.mediaProtected=false;t.url=url;t.home=false;t.title=host(url);t.loadError=null;t.progress=0;showPage()
  if(needsNavigation)t.view?.loadUrl(url)
  saveSession();evict()
 }
 private fun evict(){tabs.filter{it!==tab&&it.view!=null}.sortedWith(compareByDescending<Tab>{it.transferActive}.thenByDescending{it.formProtected||it.mediaProtected}.thenByDescending{it.lastUsed}).drop(2).forEach{t->captureTab(t);t.state=Bundle().also{t.view?.saveState(it)};t.restoreScroll=true;t.view?.destroy();t.view=null}}
 private fun pageBack(){val t=tab?:return;if(t.home)return;if(t.manualHistory&&t.trailIndex>0){t.trailIndex--;t.historyReplay=true;t.restoreScroll=false;t.view?.loadUrl(t.trail[t.trailIndex]);updateNavigationState();return};val v=t.view;if(v?.canGoBack()==true){if(t.loadError!=null){t.loadError=null;showPage()};v.goBack()}else {t.home=true;showPage()};updateChrome()}
 private fun pageForward(){val t=tab?:return;if(t.manualHistory&&t.trailIndex<t.trail.lastIndex){t.trailIndex++;t.historyReplay=true;t.restoreScroll=false;t.view?.loadUrl(t.trail[t.trailIndex]);updateNavigationState();return};if(t.home&&t.url.isNotBlank()){t.home=false;showPage()}else t.view?.takeIf{it.canGoForward()}?.let{if(t.loadError!=null){t.loadError=null;showPage()};it.goForward()};updateChrome()}
 private fun back(){

  if(popupLayer!=null){closePopup();return}

  if(addressEditing){if(imeExpected||imeVisible()){imeExpected=false;hideKeyboard();return};cancelAddress();return}

  if(findBar.visibility==View.VISIBLE){if(nativeTyping||imeVisible()){nativeTyping=false;imeExpected=false;hideKeyboard()}else closeFind();return}

  if(webEditing){leaveWebEditing();hideKeyboard();return}

  if(nativeTyping){nativeTyping=false;imeExpected=false;hideKeyboard();stopInput();return}

  if(prompt!=null){prompt?.cancel();return}

  if(video!=null){hideVideo();return}

  if(screen!="page"){

   if(imeVisible()){hideKeyboard();return}

   hideKeyboard();if(screen=="tabs"){returnFromTabs?.let{val i=tabs.indexOf(it);if(i>=0)active=i};showPage()}else screenBack?.invoke()?:showPage();return

  }

  if(tabs.isEmpty()){finish();return}

  closeTab(active,false)

 }

 private fun systemBack(){if(video!=null){hideVideo();return};if(prompt!=null){prompt?.cancel();return};if(screen!="page"){back();return};if(popupKind=="menu"){closePopup();return};if(addressEditing)cancelAddress(false);if(webEditing)leaveWebEditing();if(nativeTyping){nativeTyping=false;hideKeyboard()};if(findBar.visibility==View.VISIBLE)closeFind();showMenu()}
 private fun imeVisible()=if(Build.VERSION.SDK_INT>=30)root.rootWindowInsets?.isVisible(WindowInsets.Type.ime())==true else {val r=Rect();root.getWindowVisibleDisplayFrame(r);resources.displayMetrics.heightPixels-r.bottom>dp(120)}
 @Deprecated("Legacy Android back") override fun onBackPressed(){systemBack()}
 private fun navigate(direction:String,source:String="key"){
  if(prompt!=null){val d=when(direction){"up"->View.FOCUS_UP;"down"->View.FOCUS_DOWN;"left"->View.FOCUS_LEFT;else->View.FOCUS_RIGHT};prompt?.window?.decorView?.findFocus()?.focusSearch(d)?.requestFocus();return}
  if(addressTyping||webEditing&&!mouse||nativeTyping)return
  if(screen=="mouse_settings"&&focused is SeekBar&&direction in listOf("left","right")){val slider=focused as SeekBar;slider.progress=(slider.progress+if(direction=="left")-50 else 50).coerceIn(0,slider.max);return}
  if(addressEditing){moveNative(direction);return}
  if(mouse&&screen=="page"&&tab?.home==false&&popupLayer==null&&!chromeFocus)return
  if(popupLayer!=null){moveNative(direction);return}
  val now=SystemClock.uptimeMillis();if(lastNav==direction&&lastNavSource!=source&&now-lastNavTime<80)return;lastNav=direction;lastNavTime=now;lastNavSource=source
  if(prompt!=null){val androidDirection=when(direction){"up"->View.FOCUS_UP;"down"->View.FOCUS_DOWN;"left"->View.FOCUS_LEFT;else->View.FOCUS_RIGHT};prompt?.window?.decorView?.findFocus()?.focusSearch(androidDirection)?.requestFocus();return}
  if(screen!="page"||tab==null||tab?.home==true||tab?.loadError!=null||chromeFocus){moveNative(direction);return}
  if(direction=="up"&&(web?.scrollY?:0)<=dp(2)){focusPageControls();return};webHasSelection=true;scrollRouteKnown=false
  web?.evaluateJavascript("window.__pocketSurf ? window.__pocketSurf.move('$direction') : false"){result->if(result=="false"&&direction=="up")focusChrome()}
 }
 private fun perform(action:Control,source:String="key"){
  if(action==Control.MENU&&screen=="settings")return
  if(screen!="page"&&screen!="tabs"&&action==Control.TABS)return
  if(screen!="page"&&popupLayer==null&&action==Control.SEARCH){if(action==Control.SEARCH&&screen in listOf("history","bookmarks","tabs"))queryField?.let{nativeTyping=false;imeExpected=false;it.requestFocus()};return}
  if(popupLayer!=null){when(action){Control.BACK->closePopup();Control.CONFIRM->activateNative();Control.UP->moveNative("up");Control.DOWN->moveNative("down");Control.LEFT->moveNative("left");Control.RIGHT->moveNative("right");Control.MENU->{closePopup();showSettings()};else->{}};return}
  if((addressTyping||webEditing&&!mouse||nativeTyping)&&action !in listOf(Control.BACK,Control.MENU,Control.SEARCH,Control.POINTER))return

  if(video!=null){when(action){Control.BACK->hideVideo();Control.CONFIRM->{val v=video;v?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER));v?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER))};Control.UP,Control.DOWN,Control.LEFT,Control.RIGHT->{val direction=when(action){Control.UP->View.FOCUS_UP;Control.DOWN->View.FOCUS_DOWN;Control.LEFT->View.FOCUS_LEFT;else->View.FOCUS_RIGHT};video?.findFocus()?.focusSearch(direction)?.requestFocus()};else->{}};return}
  if(prompt!=null){when(action){Control.CONFIRM->prompt?.window?.decorView?.findFocus()?.performClick();Control.BACK->back();Control.UP->navigate("up",source);Control.DOWN->navigate("down",source);Control.LEFT->navigate("left",source);Control.RIGHT->navigate("right",source);else->{}};return}
  when(action){
   Control.UP->navigate("up",source);Control.DOWN->navigate("down",source);Control.LEFT->navigate("left",source);Control.RIGHT->navigate("right",source)
   Control.BACK->back()
   Control.CONFIRM->{if(addressEditing||screen!="page"||tab==null||tab?.home==true||tab?.loadError!=null||chromeFocus){activateNative()}else if(mouse){clickAt(pointerX,pointerY)}else activateElement()}
   Control.TABS->{if(screen=="tabs")back()else showTabs()}
   Control.CONTEXT->{if(screen in listOf("page","tabs"))newTab()}
   Control.SEARCH->{if(screen=="page"){if(addressEditing){addressField.requestFocus();addressField.selectAll()}else focusChrome()}else if(screen=="tabs")queryField?.let{nativeTyping=false;imeExpected=false;focused=it;it.requestFocus()}}
   Control.MENU->showSettings()

   Control.HISTORY_BACK->{if(screen=="page")pageBack()}
   Control.HISTORY_FORWARD->{if(screen=="tabs"){val card=focused?.getTag(TAG_TAB) as? Int;if(card!=null)closeTab(card,true)}else if(screen=="page")pageForward()}
   Control.PREVIOUS_TAB->tabStep(-1)
   Control.NEXT_TAB->tabStep(1)
   Control.POINTER->{if(screen=="mouse_settings")setPreviewMouse(!mouse)else if(screen=="page")setMouseMode(!mouse)}
   Control.ZOOM->{if(screen=="page"){interactionEpoch++;web?.let{if(it.scale>resources.displayMetrics.density*1.15f)it.zoomBy(resources.displayMetrics.density/it.scale)else it.zoomBy(1.5f)}}}
  }
 }
 private fun canTabStep(delta:Int):Boolean {if(screen=="tabs"){val visible=focusNodes.mapNotNull{it.view.getTag(TAG_TAB) as? Int}.distinct();return visible.indexOf(selected)+delta in visible.indices};return screen=="page"&&tabs.size>1&&active+delta in tabs.indices}
 private fun tabStep(delta:Int){if(screen=="tabs"){val visible=focusNodes.mapNotNull{it.view.getTag(TAG_TAB) as? Int}.distinct();if(visible.isEmpty())return;val index=visible.indexOf(selected);val next=if(index<0)visible.first()else visible[(index+delta).coerceIn(0,visible.lastIndex)];if(next==selected)return;selected=next;focusNodes.firstOrNull{it.view.getTag(TAG_TAB)==selected}?.view?.let{focused=it;it.requestFocus();refreshJumpHints();it.requestRectangleOnScreen(Rect(0,0,it.width,it.height),false)}}else if(screen=="page"&&tabs.size>1)switchTab(BrowserLogic.adjacent(active,delta,tabs.size))}
 private var heldConfirmKey=-1
 private var heldConfirmLease:PageLease?=null
 private var heldConfirmView:View?=null
 private var heldConfirmTab:String?=null
 private var heldConfirmEpoch=0L
 private var heldConfirmHandled=false
 private var heldConfirmPoint:FloatArray?=null
 private val holdConfirm=Runnable{if(heldConfirmKey>=0){heldConfirmHandled=true;val target=heldConfirmView;if(target!=null){if(resumed&&interactionEpoch==heldConfirmEpoch&&screen=="page"&&tab?.id==heldConfirmTab&&focused===target)target.performLongClick()}else heldConfirmLease?.let{if(valid(it))contextMenu(heldConfirmPoint)}}}
 private fun confirmContextView()=focused?.takeIf{it.isLongClickable&&(chromeFocus||tab?.home!=false)&&(it.getTag(TAG_TAB)!=null||it.getTag(TAG_SHORTCUT)!=null)}
 private fun shouldHoldConfirm()=screen=="page"&&popupLayer==null&&prompt==null&&video==null&&!addressEditing&&!nativeTyping&&(!webEditing||mouse)&&((tab?.home==false&&!chromeFocus)||confirmContextView()!=null)
 override fun dispatchKeyEvent(e:KeyEvent):Boolean {
  wakeInput()
  if(e.keyCode==KeyEvent.KEYCODE_BACK){if(e.action==KeyEvent.ACTION_UP&&!e.isCanceled)systemBack();return true}
  captureBinding?.let {target->if(e.action==KeyEvent.ACTION_DOWN&&e.repeatCount==0){if(e.keyCode==bindings.key(Control.BACK)){captureBinding=null;showController()}else if(e.keyCode in REMAPPABLE)assignButton(target,e.keyCode)};return true}
  if(e.keyCode==heldConfirmKey){if(e.action==KeyEvent.ACTION_UP){handler.removeCallbacks(holdConfirm);val run=!e.isCanceled&&!heldConfirmHandled;val lease=heldConfirmLease;val target=heldConfirmView;val owner=heldConfirmTab;heldConfirmKey=-1;heldConfirmLease=null;heldConfirmView=null;if(run&&((target!=null&&resumed&&interactionEpoch==heldConfirmEpoch&&screen=="page"&&tab?.id==owner&&focused===target)||(target==null&&lease!=null&&valid(lease))))perform(Control.CONFIRM);return true};return true}
  val action=bindings.action(e.keyCode)?:return super.dispatchKeyEvent(e)
  val tabAction=action==Control.PREVIOUS_TAB||action==Control.NEXT_TAB
  if(tabAction&&!canTabStep(if(action==Control.PREVIOUS_TAB)-1 else 1))return true
  setInputMode(false,!tabAction)
  if(action==Control.CONFIRM&&e.action==KeyEvent.ACTION_DOWN&&e.repeatCount==0&&shouldHoldConfirm()){heldConfirmKey=e.keyCode;heldConfirmEpoch=interactionEpoch;heldConfirmHandled=false;heldConfirmView=confirmContextView();heldConfirmLease=if(heldConfirmView==null)lease()else null;heldConfirmTab=tab?.id;heldConfirmPoint=if(mouse)floatArrayOf(pointerX,pointerY,content.width.toFloat().coerceAtLeast(1f))else null;handler.postDelayed(holdConfirm,500);return true}
  if((addressTyping||webEditing&&!mouse||nativeTyping)&&action in listOf(Control.UP,Control.DOWN,Control.LEFT,Control.RIGHT,Control.CONFIRM))return super.dispatchKeyEvent(e)
  if(e.keyCode in listOf(KeyEvent.KEYCODE_BUTTON_L2,KeyEvent.KEYCODE_BUTTON_R2)){if(e.action==KeyEvent.ACTION_DOWN&&e.repeatCount==0)controller.trigger(if(e.keyCode==KeyEvent.KEYCODE_BUTTON_L2)-1 else 1,SystemClock.uptimeMillis()){perform(action)};return true}
  if(e.action==KeyEvent.ACTION_DOWN&&(e.repeatCount==0||action in listOf(Control.UP,Control.DOWN,Control.LEFT,Control.RIGHT)))dispatchMapped(e.keyCode,"key",e.repeatCount>0)
  return true
 }
 private fun dispatchMapped(code:Int,source:String,repeat:Boolean=false){
  val action=bindings.action(code)?:return;val now=SystemClock.uptimeMillis();val last=rawEvents[code];if(last!=null&&last.first!=source&&now-last.second<80)return
  if(repeat&&action !in listOf(Control.UP,Control.DOWN,Control.LEFT,Control.RIGHT))return
  rawEvents[code]=source to now;perform(action,source)
 }
 private fun axisDirection(direction:String,repeat:Boolean=false){
  val a=controller.axes
  if(max(abs(a.hatX),abs(a.hatY))>.5f){val code=when(direction){"up"->19;"down"->20;"left"->21;else->22};dispatchMapped(code,"axis",repeat)}else navigate(direction,"axis")
 }
 override fun dispatchGenericMotionEvent(e:MotionEvent):Boolean {
  if(ControllerInput.isController(e)){
   wakeInput()
   var axes=controller.read(e);if(max(abs(axes.rightX),abs(axes.rightY))>.22f)tab?.restoreScroll=false;val activeAxis=max(max(abs(axes.leftX),abs(axes.leftY)),max(abs(axes.rightX),abs(axes.rightY)))>.22f||max(abs(axes.hatX),abs(axes.hatY))>.5f||max(axes.leftTrigger,axes.rightTrigger)>.5f;if(activeAxis){val directionInput=max(max(abs(axes.leftX),abs(axes.leftY)),max(abs(axes.rightX),abs(axes.rightY)))>.22f||max(abs(axes.hatX),abs(axes.hatY))>.5f;if(directionInput)setInputMode(false)else if((axes.leftTrigger>.5f&&canTabStep(-1))||(axes.rightTrigger>.5f&&canTabStep(1)))setInputMode(false,false)}
   if(addressTyping||webEditing&&!mouse||nativeTyping){controller.reset();sendScroll(0f,0f);return true}
   axes=when(prefs.getString("right_axes","auto")){"rx"->axes.copy(rightX=e.getAxisValue(MotionEvent.AXIS_RX),rightY=e.getAxisValue(MotionEvent.AXIS_RY));"z"->axes.copy(rightX=e.getAxisValue(MotionEvent.AXIS_Z),rightY=e.getAxisValue(MotionEvent.AXIS_RZ));else->axes}
   controller.update(axes,SystemClock.uptimeMillis(),mouse&&(screen=="mouse_settings"||(screen=="page"&&tab?.home==false&&tab?.loadError==null)),{axisDirection(it)}){delta->bindings.action(if(delta<0)KeyEvent.KEYCODE_BUTTON_L2 else KeyEvent.KEYCODE_BUTTON_R2)?.let {a->perform(a)}}
   return true
  }
  return super.dispatchGenericMotionEvent(e)
 }
 private fun scheduleFrame(){if(resumed&&!frameScheduled){frameScheduled=true;Choreographer.getInstance().postFrameCallback(frame)}}
 private fun wakeInput(){lastInputTime=SystemClock.uptimeMillis();scheduleFrame()}
 private val maintenance=object:Runnable{override fun run(){if(!resumed)return;if(Build.VERSION.SDK_INT<30&&(addressEditing||nativeTyping||imeWasVisible))onImeVisibilityChanged(imeVisible());if(screen=="page"&&popupLayer==null&&!addressEditing&&!nativeTyping&&!chromeFocus)checkWebEditing();if(screen=="page")capturePreview(false);if(screen=="downloads")nativeBody?.let{renderDownloads(it)};handler.postDelayed(this,750)}}
 private val frame=object:Choreographer.FrameCallback {override fun doFrame(frameTimeNanos:Long){
  frameScheduled=false
  val now=SystemClock.uptimeMillis();val dt=if(lastFrame==0L)0f else ((frameTimeNanos-lastFrame)/1_000_000_000f).coerceIn(0f,.05f);lastFrame=frameTimeNanos
  if(resumed&&prompt==null&&video==null){
   // No page rasterization or backdrop capture in the input/frame loop.
   if(!addressTyping&&(!webEditing||mouse)&&!nativeTyping)controller.repeat(now){axisDirection(it,true)}
   val velocity=if(popupLayer==null&&!addressEditing&&!webEditing&&!nativeTyping)controller.scroll(1800f*resources.displayMetrics.density,.22f).let{v->val amount=((max(abs(controller.axes.rightX),abs(controller.axes.rightY))-.22f)/.78f).coerceIn(0f,1f);val scale=.25f+.75f*amount;v.first*scale to v.second*scale}else 0f to 0f
   if(screen=="mouse_settings"&&mouse&&!touchMode)moveMousePreview(dt)
   if(screen=="page"&&tab?.home==false&&tab?.loadError==null&&!chromeFocus&&popupLayer==null&&!addressEditing&&(!webEditing||mouse)){
    var sx=velocity.first;var sy=velocity.second
    if(mouse&&!touchMode){val v=mouseVelocity(dt);pointerX=(pointerX+v.first*dt).coerceIn(0f,max(0f,content.width-dp(10).toFloat()));pointerY=(pointerY+v.second*dt).coerceIn(0f,max(0f,content.height-dp(14).toFloat()));pointer.translationX=pointerX;pointer.translationY=pointerY;web?.let{hover.move(it,pointerX,pointerY,velocity.first!=0f||velocity.second!=0f||v.first!=0f||v.second!=0f)}
     if(v.first!=0f||v.second!=0f)web?.evaluateJavascript("window.__pocketSurf && window.__pocketSurf.point([$pointerX,$pointerY,${content.width}])",null)
     val edge=dp(18);val ex=if(pointerX<edge&&v.first<0)-1f else if(pointerX>content.width-dp(10)-edge&&v.first>0)1f else 0f;val ey=if(pointerY<edge&&v.second<0)-1f else if(pointerY>content.height-dp(14)-edge&&v.second>0)1f else 0f
     if(sx==0f&&sy==0f&&(ex!=0f||ey!=0f)){val e=BrowserLogic.velocity(ex,ey,0f,1200f*resources.displayMetrics.density);sx=e.first;sy=e.second}
    }
    scrollPage(sx,sy,dt)
   }else{sendScroll(0f,0f);if(popupLayer==null&&!addressEditing&&!webEditing)nativeScroll?.scrollBy(0,(velocity.second*dt).roundToInt())}
   if(screen=="page"&&popupLayer==null&&!addressEditing&&!nativeTyping&&!chromeFocus&&now>nextEditCheck){checkWebEditing();nextEditCheck=now+700}
   if(screen=="downloads"&&now-downloadPoll>750){nativeBody?.let{renderDownloads(it)};downloadPoll=now}
  }
  if(resumed&&(max(max(abs(controller.axes.leftX),abs(controller.axes.leftY)),max(abs(controller.axes.rightX),abs(controller.axes.rightY)))>.22f||max(abs(controller.axes.hatX),abs(controller.axes.hatY))>.5f||SystemClock.uptimeMillis()-lastInputTime<200))scheduleFrame()
 }}
 private fun scrollPage(x:Float,y:Float,dt:Float){
  if(x==0f&&y==0f){sendScroll(0f,0f);scrollRouteKnown=!mouse&&!webHasSelection;scrollRoutePending=false;scrollRouteGeneration++;return}
  if(!scrollRouteKnown&&!scrollRoutePending){scrollRoutePending=true;scrollRouteRequestedAt=SystemClock.uptimeMillis();val generation=scrollRouteGeneration;val v=web;v?.evaluateJavascript("window.__pocketSurf ? window.__pocketSurf.canNativeScroll($x,$y) : true"){value->if(generation==scrollRouteGeneration&&v===web){nativeRootScroll=value!="false";scrollRouteKnown=true;scrollRoutePending=false}}}
  // A busy page JS thread must not suspend document scrolling for the entire load.
  val useNative=if(scrollRouteKnown)nativeRootScroll else if(scrollRoutePending&&SystemClock.uptimeMillis()-scrollRouteRequestedAt>=64)true else return
  if(useNative){sendScroll(0f,0f);val px=x*dt+scrollRemainderX;val py=y*dt+scrollRemainderY;val dx=px.toInt();val dy=py.toInt();scrollRemainderX=px-dx;scrollRemainderY=py-dy;if(dx!=0||dy!=0)web?.scrollBy(dx,dy)}else sendScroll(x,y)
 }
 private fun sendScroll(x:Float,y:Float){if(lastScroll==(x to y))return;lastScroll=x to y;val density=resources.displayMetrics.density;web?.evaluateJavascript("window.__pocketSurf && window.__pocketSurf.setScroll(${x/density},${y/density})",null)}
 private fun checkWebEditing(){if(checkingEditing||screen!="page"||tab?.home!=false||addressEditing||popupLayer!=null)return;checkingEditing=true;val probe=editingProbe;web?.evaluateJavascript("window.__pocketSurf && window.__pocketSurf.editing()"){value->checkingEditing=false;if(probe!=editingProbe)return@evaluateJavascript;if(value=="true"&&!webEditing){webEditing=true;tab?.formProtected=true;imeExpected=true;stopInput()}else if(value!="true"&&webEditing){webEditing=false;imeExpected=false;stopInput()}}?:run{checkingEditing=false}}
 private fun leaveWebEditing(){webEditing=false;imeExpected=false;stopInput();web?.evaluateJavascript("window.__pocketSurf && window.__pocketSurf.releaseEditing()",null)}
 private fun focusPageControls(){val first=focusNodes.firstOrNull{it.view.getTag(TAG_FAVORITE)==true};if(first!=null){chromeFocus=true;focused=first.view;first.view.requestFocus();updateLegend()}else focusChrome()}
 private fun focusChrome(){chromeFocus=true;focused=addressField;addressField.showSoftInputOnFocus=false;addressField.requestFocus();updateLegend()}
 private fun syncFocusMode(){for(n in focusNodes){n.view.isFocusableInTouchMode=n.view is EditText||!touchMode;n.view.foreground=focusOutline(n.view,n.row,!touchMode&&n.view===focused&&(n.row>-2||chromeFocus))}}
 private fun setInputMode(touch:Boolean,updatePage:Boolean=true){if(touchMode==touch)return;interactionEpoch++;hover.leave();stopInput();touchMode=touch;syncFocusMode();if(touch)chromeFocus=false;else if(mouse&&screen=="page"&&!addressEditing&&popupLayer==null){webEditing=false;chromeFocus=false;focused=null;content.requestFocus()};if(updatePage)web?.evaluateJavascript("window.__pocketSurf && window.__pocketSurf.mode('${pageInputMode()}')",null);if(::pointer.isInitialized)pointer.visibility=if(!touch&&mouse&&screen=="page"&&tab?.home==false)View.VISIBLE else View.GONE;updateLegend()}

 override fun dispatchTouchEvent(e:MotionEvent):Boolean {wakeInput();if(dismissPopupGesture){if(e.actionMasked==MotionEvent.ACTION_UP||e.actionMasked==MotionEvent.ACTION_CANCEL)dismissPopupGesture=false;return true};if(e.actionMasked==MotionEvent.ACTION_DOWN&&e.source and InputDevice.SOURCE_TOUCHSCREEN==InputDevice.SOURCE_TOUCHSCREEN){setInputMode(true)};val result=super.dispatchTouchEvent(e);if(e.actionMasked==MotionEvent.ACTION_UP){val field=root.findFocus();if(field is EditText&&field!==addressField){nativeTyping=true;imeExpected=true;stopInput()}};if(e.actionMasked in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP))lastTouchTime=SystemClock.uptimeMillis();if(e.actionMasked==MotionEvent.ACTION_UP&&screen=="page")handler.postDelayed({checkWebEditing()},60);return result}

 private data class PageLease(val target:Tab,val view:WebView,val generation:Long,val epoch:Long)
 private fun lease():PageLease? {val t=tab?:return null;val v=t.view?:return null;return PageLease(t,v,t.generation,interactionEpoch)}
 private fun valid(p:PageLease)=resumed&&screen=="page"&&tab===p.target&&web===p.view&&p.generation==p.target.generation&&p.epoch==interactionEpoch&&!addressEditing&&popupLayer==null
 private fun activateElement(){val p=lease()?:return;p.view.evaluateJavascript("window.__pocketSurf ? window.__pocketSurf.activationPoint() : null"){value->if(valid(p))try{val o=JSONObject(value);val ratio=p.view.width/o.getDouble("width");if(o.optBoolean("editable")){webEditing=true;p.target.formProtected=true;imeExpected=true;stopInput()};clickAt((o.getDouble("x")*ratio).toFloat(),(o.getDouble("y")*ratio).toFloat())}catch(_:Exception){}}}
 private fun clickAt(x:Float,y:Float){val v=web?:return;if(mouse){val p=lease()?:return;v.evaluateJavascript("window.__pocketSurf && window.__pocketSurf.prepareClick()"){if(valid(p)&&mouse&&!touchMode){hover.click(v,x,y);handler.postDelayed({checkWebEditing()},60)}};return};val now=SystemClock.uptimeMillis();for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){val e=MotionEvent.obtain(now,now,action,x,y,0);v.dispatchTouchEvent(e);e.recycle()}}
 private fun showPageContext(){showMenu()}
 private fun contextMenu(){contextMenu(null)}
 private fun contextMenu(point:FloatArray?){
  if(tab==null||tab?.home==true){val items=mutableListOf(MenuItem("plus","Pin a bookmark"){showShortcutPicker()},MenuItem("home","Start page settings"){showHomeSettings()});if(tab!=null)items.add(MenuItem("close","Close tab"){closeTab(active,false)});openPopup("context","Context actions","Start page",items);return}
  val p=lease()?:return;p.view.evaluateJavascript(if(point==null)"window.__pocketSurf ? JSON.stringify(window.__pocketSurf.target()) : '{}'"else"window.__pocketSurf ? JSON.stringify(window.__pocketSurf.targetAt([${point[0]},${point[1]},${point[2]}])) : '{}'"){value->if(!valid(p))return@evaluateJavascript;try{val decoded=org.json.JSONTokener(value).nextValue() as? String?:"{}";val o=JSONObject(decoded);if(o.optBoolean("editable")){if(mouse&&point!=null)clickAt(point[0],point[1])else activateElement();return@evaluateJavascript};val items=mutableListOf<MenuItem>();val link=o.optString("link");val image=o.optString("image")
   if(link.startsWith("http")){items.add(MenuItem("globe","Open link"){openUrl(link)});items.add(MenuItem("plus","Open in new tab"){openUrl(link,true)});items.add(MenuItem("share","Copy link address"){getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Link",link));toast("Link copied")});items.add(MenuItem("share","Share link"){share(link)})}
   if(image.startsWith("http")){items.add(MenuItem("globe","Open image"){openUrl(image,true)});items.add(MenuItem("download","Save image"){download(image)});items.add(MenuItem("share","Copy image address"){getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Image",image));toast("Image address copied")})}
   if(items.isEmpty()){showPageContext();return@evaluateJavascript};val r=o.optJSONObject("rect");val scale=content.width/o.optDouble("viewportWidth",content.width.toDouble());val rect=r?.let{RectF((it.optDouble("left")*scale).toFloat(),(it.optDouble("top")*scale).toFloat(),((it.optDouble("left")+it.optDouble("width"))*scale).toFloat(),((it.optDouble("top")+it.optDouble("height"))*scale).toFloat())};openPopup("context","Context actions",host(link.ifBlank{image})+" · "+o.optString("title"),items,rect)
  }catch(_:Exception){showPageContext()}}
 }

 private fun makeWeb(t:Tab)=WebView(this).apply {
  t.previewReady=false
  settings.textZoom=textZoom(t.url);settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.allowFileAccess=false;settings.allowContentAccess=true;settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;settings.safeBrowsingEnabled=true
  settings.setSupportZoom(true);settings.builtInZoomControls=true;settings.displayZoomControls=false;settings.mediaPlaybackRequiresUserGesture=true;settings.setSupportMultipleWindows(false)
  setFindListener{index,total,done->if(this===web&&::findCount.isInitialized&&findBar.visibility==View.VISIBLE){findCount.text=if(total>0)"${index+1}/$total"else"0/0";findCount.contentDescription="Match count";if(Build.VERSION.SDK_INT>=30)findCount.stateDescription=findCount.text.toString()}}
  importantForAutofill=if(isPrivateSession)View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS else View.IMPORTANT_FOR_AUTOFILL_YES;CookieManager.getInstance().setAcceptThirdPartyCookies(this,false)
  webViewClient=object:WebViewClient(){
   override fun shouldInterceptRequest(v:WebView,r:WebResourceRequest):WebResourceResponse?=if(adblock.shouldBlock(r.url.toString(),t.url,r.isForMainFrame))WebResourceResponse("text/plain","UTF-8",java.io.ByteArrayInputStream(ByteArray(0)))else null
   override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest):Boolean {if(r.url.scheme in listOf("http","https"))return false;if(r.isForMainFrame&&r.hasGesture())confirm("Open external app?","This link uses ${r.url.scheme}.",{showPage();try {startActivity(Intent(Intent.ACTION_VIEW,r.url).addCategory(Intent.CATEGORY_BROWSABLE))}catch(_:Exception){toast("No app can open this link")}},{showPage()},"Open app");return true}
   override fun onPageCommitVisible(v:WebView,url:String){t.previewReady=true}
   override fun onPageStarted(v:WebView,url:String,favicon:Bitmap?){t.generation++;t.previewReady=false;t.preview=null;t.formProtected=false;t.mediaProtected=false;if(t.transferActive)generatedTransfer?.close();if(t===tab){val held=controller.axes;val permission=pendingPermission;pendingPermission=null;if(permission!=null){prompt?.dismiss();prompt=null;permission.deny()};interactionEpoch++;hover.leave();if(t.manualHistory){if(t.historyReplay){if(t.trail.isNotEmpty())t.trail[t.trailIndex]=url}else if(t.trail.getOrNull(t.trailIndex)!=url){while(t.trail.size>t.trailIndex+1)t.trail.removeAt(t.trail.lastIndex);t.trail.add(url);t.trailIndex=t.trail.lastIndex}};webEditing=false;nativeTyping=false;webHasSelection=false;if(!addressEditing)imeExpected=false;stopInput();if(screen=="page"&&!addressEditing)controller.update(ControllerInput.Axes(rightX=held.rightX,rightY=held.rightY),SystemClock.uptimeMillis(),mouse,{},{})};val wasError=t.loadError!=null;t.url=url;t.home=false;t.loadError=null;t.progress=0;if(t===tab&&screen=="page"){if(wasError)showPage()else{if(!addressEditing)focusPageNeutral();updateChrome()}}}
   override fun onPageFinished(v:WebView,url:String){t.previewReady=true;t.url=url;t.progress=100;t.historyReplay=false;restorePosition(t,v);if(t.loadError==null){t.title=v.title?.takeIf{it.isNotBlank()}?:host(url);inject(v);if(!isPrivateSession)data.record(url,t.title)};saveSession();if(t===tab&&screen=="page"){updateChrome()}}
   override fun doUpdateVisitedHistory(v:WebView,url:String,isReload:Boolean){if(t.manualHistory&&t.url!=url){if(t.historyReplay){if(t.trail.isNotEmpty())t.trail[t.trailIndex]=url}else{while(t.trail.size>t.trailIndex+1)t.trail.removeAt(t.trail.lastIndex);t.trail.add(url);t.trailIndex=t.trail.lastIndex};t.url=url;t.generation++;t.formProtected=false;t.mediaProtected=false;if(t===tab){interactionEpoch++;updateNavigationState()};saveSession()}}
   override fun onReceivedSslError(v:WebView,h:SslErrorHandler,e:android.net.http.SslError){h.cancel();pageProblem(t,"The website certificate could not be verified. The connection was blocked to protect your data. Try again later or check the address.")}
   override fun onReceivedError(v:WebView,r:WebResourceRequest,e:WebResourceError){if(r.isForMainFrame)diagnosticError="WebView error ${e.errorCode}";if(r.isForMainFrame&&t.loadError==null)pageProblem(t,when(e.errorCode){WebViewClient.ERROR_HOST_LOOKUP->"The website could not be found. Check the address and your internet connection, then try again.";WebViewClient.ERROR_TIMEOUT->"The website took too long to respond. Check your connection and try again.";WebViewClient.ERROR_CONNECT->"The website could not be reached. Check your internet connection and try again.";else->"The page could not be loaded. Check the address or try again."})}
   override fun onRenderProcessGone(v:WebView,d:RenderProcessGoneDetail):Boolean {(v.parent as? ViewGroup)?.removeView(v);v.destroy();t.view=null;t.state=null;pageProblem(t,"This page stopped responding. Try again to reload it, or return to the start page.");return true}
  }
  webChromeClient=object:WebChromeClient(){
   override fun onProgressChanged(v:WebView,p:Int){t.progress=p;if(t===tab&&screen=="page")updateNavigationState()}
   override fun onReceivedIcon(v:WebView,bitmap:Bitmap){favicons.put(t.url,bitmap);refreshSiteIcons()}
   override fun onReceivedTitle(v:WebView,title:String){if(t.loadError==null){t.title=title;if(t===tab&&screen=="page")updateChrome()}}
   override fun onShowFileChooser(v:WebView,c:ValueCallback<Array<Uri>>,p:FileChooserParams):Boolean {val target=lease();if(target==null||!valid(target)||target.view!==v){c.onReceiveValue(null);return true};fileCallback?.onReceiveValue(null);fileCallback=c;fileLease=target;try {startActivityForResult(p.createIntent(),7)}catch(_:Exception){c.onReceiveValue(null);fileCallback=null};return true}
   override fun onPermissionRequest(r:PermissionRequest){runOnUiThread{if(t!==tab)r.deny()else requestWebsitePermission(r)}}
   override fun onPermissionRequestCanceled(r:PermissionRequest){if(pendingPermission===r){pendingPermission=null;prompt?.dismiss();prompt=null}}
   override fun onShowCustomView(v:View,c:CustomViewCallback){if(video!=null){c.onCustomViewHidden();return};closePopup();cancelAddress(false);stopInput();video=v;videoCallback=c;fullscreenFlags=window.attributes.flags;fullscreenSystemUi=window.decorView.systemUiVisibility;window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);window.decorView.systemUiVisibility=fullscreenSystemUi or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;(content.layoutParams as FrameLayout.LayoutParams).bottomMargin=0;content.requestLayout();content.addView(v,FrameLayout.LayoutParams(-1,-1));v.keepScreenOn=true;v.isFocusableInTouchMode=true;v.requestFocus();toolbar.visibility=View.GONE;tabNavigation.visibility=View.GONE;favoritesBar.visibility=View.GONE;legendSurface.visibility=View.GONE;legendHost.visibility=View.GONE}
   override fun onHideCustomView(){hideVideo()}
   override fun onJsAlert(v:WebView,url:String,message:String,r:JsResult):Boolean {showWebPrompt(url,message,{r.confirm()},{r.cancel()});return true}
   override fun onJsConfirm(v:WebView,url:String,message:String,r:JsResult):Boolean {showWebPrompt(url,message,{r.confirm()},{r.cancel()});return true}
   override fun onJsBeforeUnload(v:WebView,url:String,message:String,r:JsResult):Boolean {showWebPrompt(url,message,{r.confirm()},{r.cancel()});return true}
  }
  setDownloadListener{url,ua,disposition,mime,_->download(url,ua,disposition,mime)}
  var touchPoint=floatArrayOf(0f,0f,1f);setOnTouchListener{_,event->if(event.actionMasked==MotionEvent.ACTION_DOWN)touchPoint=floatArrayOf(event.x,event.y,width.toFloat().coerceAtLeast(1f));false}
  setOnLongClickListener {val hit=hitTestResult;if(hit.type==WebView.HitTestResult.EDIT_TEXT_TYPE)false else{if(hit.type in listOf(WebView.HitTestResult.SRC_ANCHOR_TYPE,WebView.HitTestResult.IMAGE_TYPE,WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE))contextMenu(touchPoint)else showMenu();true}}
 }
 private fun showWebPrompt(url:String,message:String,yes:()->Unit,no:()->Unit,positiveLabel:String="Continue",negativeLabel:String="Cancel"){
  stopInput()
  val d=object:android.app.AlertDialog(this@BrowserActivity){
   override fun dispatchKeyEvent(e:KeyEvent):Boolean=if(bindings.action(e.keyCode)!=null||e.keyCode==KeyEvent.KEYCODE_BACK)this@BrowserActivity.dispatchKeyEvent(e)else super.dispatchKeyEvent(e)
   override fun dispatchGenericMotionEvent(e:MotionEvent):Boolean=if(ControllerInput.isController(e))this@BrowserActivity.dispatchGenericMotionEvent(e)else super.dispatchGenericMotionEvent(e)
  }
  d.setTitle(host(url));d.setMessage(message)
  d.setButton(android.app.AlertDialog.BUTTON_POSITIVE,positiveLabel,DialogInterface.OnClickListener{_,_->prompt=null;yes()})
  d.setButton(android.app.AlertDialog.BUTTON_NEGATIVE,negativeLabel,DialogInterface.OnClickListener{_,_->prompt=null;no()})
  d.setOnCancelListener{prompt=null;no()};prompt=d;d.show();d.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).requestFocus()
 }
 private fun inject(v:WebView){lastScroll=null;v.evaluateJavascript(assets.open("navigation.js").bufferedReader().readText(),null);v.evaluateJavascript("window.__pocketSurf && window.__pocketSurf.mode('${pageInputMode()}')",null);v.evaluateJavascript(adblock.cosmeticScript(v.url.orEmpty()),null);v.evaluateJavascript(cookieConsent.script(v.url.orEmpty()),null);v.evaluateJavascript("document.documentElement.style.scrollPaddingBottom='${(footerHeight()/resources.displayMetrics.density).roundToInt()+12}px'",null)}
 private fun hideVideo(){val current=video?:return;val callback=videoCallback;video=null;videoCallback=null;current.keepScreenOn=false;content.removeView(current);window.setFlags(fullscreenFlags,WindowManager.LayoutParams.FLAG_FULLSCREEN);window.decorView.systemUiVisibility=fullscreenSystemUi;toolbar.visibility=if(screen=="page"||addressEditing)View.VISIBLE else View.GONE;tabNavigation.visibility=toolbar.visibility;updateFavoritesBar();web?.requestFocus();updateLegend();stopInput();callback?.onCustomViewHidden()}
 private fun requestHighRefreshRate(){
  val screenDisplay=(window.decorView.display?:windowManager.defaultDisplay)?:return
  val current=screenDisplay.mode;val rate=screenDisplay.supportedModes.filter{it.physicalWidth==current.physicalWidth&&it.physicalHeight==current.physicalHeight}.maxOfOrNull{it.refreshRate}?:screenDisplay.refreshRate
  if(rate>0)window.attributes=window.attributes.apply{preferredDisplayModeId=0;preferredRefreshRate=rate}
 }
 private fun share(url:String){try {startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,url),"Share page"))}catch(_:Exception){toast("Sharing is unavailable")}}
 private fun toast(message:String)=Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
 override fun onPause(){interactionEpoch++;hover.leave();resumed=false;Choreographer.getInstance().removeFrameCallback(frame);frameScheduled=false;handler.removeCallbacks(maintenance);stopInput();capturePreview();saveSession();web?.onPause();super.onPause()}
 override fun onResume(){super.onResume();resumed=true;lastFrame=0;stopInput();requestHighRefreshRate();Choreographer.getInstance().removeFrameCallback(frame);frameScheduled=false;lastInputTime=SystemClock.uptimeMillis();scheduleFrame();handler.removeCallbacks(maintenance);handler.postDelayed(maintenance,750);if(screen=="page"){web?.onResume();updateChrome()};if(!isPrivateSession)handler.postDelayed({(application as? SurfApplication)?.cleanupAbandonedPrivate()},750)}
 override fun onDestroy(){if(previewCacheHolder.isInitialized())previewCache.close();generatedTransfer?.close();pendingPermission?.deny();pendingPermission=null;Choreographer.getInstance().removeFrameCallback(frame);hideVideo();favicons.close();adblock.close();getSystemService(android.hardware.input.InputManager::class.java).unregisterInputDeviceListener(deviceListener);handler.removeCallbacksAndMessages(null);prompt?.dismiss();tabs.forEach{it.view?.destroy()};fileCallback?.onReceiveValue(null);super.onDestroy();if(isPrivateSession&&(application as? SurfApplication)?.privateProcess==true&&isFinishing&&!isChangingConfigurations)handler.postDelayed({android.os.Process.killProcess(android.os.Process.myPid())},150)}
 override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(!hasFocus)stopInput()}
 override fun onSaveInstanceState(out:Bundle){saveSession();out.putString("destination",screen);super.onSaveInstanceState(out)}
 override fun onConfigurationChanged(c:Configuration){super.onConfigurationChanged(c);requestHighRefreshRate();saveSession();applyTheme()}
 override fun onNewIntent(i:Intent){super.onNewIntent(i);setIntent(i);if(i.action==Intent.ACTION_MAIN&&i.data==null){newTab();return};i.data?.let{if(it.scheme in listOf("http","https"))openUrl(it.toString(),true)}}
 override fun onActivityResult(request:Int,result:Int,data:Intent?){super.onActivityResult(request,result,data);if(request==SETUP_REQUEST){setupPending=false;homeStore.shortcuts().forEach{favicons.fetch(it.url){refreshSiteIcons()}};val draft=if(addressEditing)addressField.text.toString()else null;if(screen=="page")showPage()else if(screen=="settings")showSettings()else if(screen=="about")showAbout();if(draft!=null)showSearch(typed=draft)}else if(request==71){if(result==RESULT_OK)data?.data?.let{uri->try{contentResolver.openOutputStream(uri,"wt")?.use{it.write(BackupCodec.export(prefs).toByteArray(Charsets.UTF_8))}?:error("Cannot write file");toast("Backup exported")}catch(_:Exception){toast("Backup could not be written")}}}else if(request==72){if(result==RESULT_OK)data?.data?.let{uri->val epoch=interactionEpoch;Thread({try{val bytes=contentResolver.openInputStream(uri)?.use{input->val out=java.io.ByteArrayOutputStream();val buf=ByteArray(8192);while(true){val n=input.read(buf);if(n<0)break;require(out.size()+n<=BackupCodec.MAX_BYTES);out.write(buf,0,n)};out.toByteArray()}?:error("Cannot read file");val values=BackupCodec.validate(bytes.toString(Charsets.UTF_8));runOnUiThread{if(!isFinishing&&epoch==interactionEpoch)confirm("Import backup?",BackupCodec.summary(values)+". Matching saved settings and lists will be replaced.",{BackupCodec.apply(prefs,values);showSettings();applyTheme();toast("Backup restored")},{showBackup()},"Import")}}catch(_:Exception){runOnUiThread{toast("Invalid or unreadable backup; no settings changed")}}},"PocketSurf backup").start()}}else if(request==74){val pending=pendingGenerated;pendingGenerated=null;if(result==RESULT_OK&&pending!=null)data?.data?.let{uri->if(tab!==pending.lease.target||web!==pending.lease.view||pending.lease.generation!=pending.lease.target.generation||screen!="page"){toast("Source page changed; generated download cancelled");return@let};if((data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0)runCatching{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};generatedTransfer?.close();val t=pending.lease.target;t.transferActive=true;generatedTransfer=GeneratedFileTransfer(this,pending.lease.view,pending.url,uri,pending.name,pending.mime,downloads,{!isFinishing&&tabs.contains(t)&&t.view===pending.lease.view&&t.generation==pending.lease.generation},{message->t.transferActive=false;toast(message);if(screen=="downloads")nativeBody?.let{renderDownloads(it)}}).also{it.start()}}}else if(request==7){val p=fileLease;fileCallback?.onReceiveValue(if(p!=null&&tab===p.target&&web===p.view&&p.target.generation==p.generation&&screen=="page")WebChromeClient.FileChooserParams.parseResult(result,data)else null);fileCallback=null;fileLease=null}else if(request==8&&screen=="about")showAbout()}
 companion object {private const val SETUP_REQUEST=41;private const val GOOGLE="https://www.google.com/search?q=";private const val TAG_TAB=0x7f0f0101;private const val TAG_DOWNLOAD=0x7f0f0102;private const val TAG_SHORTCUT=0x7f0f0103;private const val TAG_ICON=0x7f0f0104;private const val TAG_FAVORITE=0x7f0f0105;private const val TAG_FIND=0x7f0f0106;private const val TAG_CLOSE=0x7f0f0107;private const val TAG_HINT=0x7f0f0108;private const val TAG_HINT_ACTION=0x7f0f0109;private val REMAPPABLE=(19..23).toSet()+(96..110).toSet()+(188..203).toSet()}
}
