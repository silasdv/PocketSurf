package org.pocketsurf.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.widget.*

/** Visual focus belongs to the circle, not a filled rectangular card. */
class HistoryBubbleView(context:Context,val destination:String,private val colors:BrowserTheme):LinearLayout(context){
 private fun d(n:Int)=(n*resources.displayMetrics.density).toInt()
 private val circle=FrameLayout(context)
 private val outline=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(Color.TRANSPARENT)}
 private val image=ImageView(context).apply{scaleType=ImageView.ScaleType.FIT_CENTER;setImageDrawable(SurfIcon("globe",colors.text));importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
 private val label=TextView(context).apply{textSize=12f;setTextColor(colors.text);gravity=Gravity.CENTER;maxLines=1;ellipsize=TextUtils.TruncateAt.END;includeFontPadding=false}
 private var controllerFocus=false
 private var paintedEmphasis:Boolean?=null
 init{
  setDefaultFocusHighlightEnabled(false)
  orientation=VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;background=null
  circle.background=outline;circle.addView(image,FrameLayout.LayoutParams(-1,-1));addView(circle)
  addView(label,LayoutParams(-1,-2).apply{topMargin=d(4)});update(false)
 }
 fun presentation(name:String,compact:Boolean){
  label.text=name;contentDescription=name
  val side=d(if(compact)40 else 52);circle.layoutParams=LayoutParams(side,side)
  val inset=d(if(compact)8 else 12);image.setPadding(inset,inset,inset,inset)
 }
 fun artwork(bitmap:Bitmap?){if(bitmap!=null&&!bitmap.isRecycled){image.imageTintList=null;image.clearColorFilter();image.setImageBitmap(bitmap)}}
 fun update(focused:Boolean){controllerFocus=focused;paint()}
 private fun paint(){val emphasis=controllerFocus||isPressed||isSelected;if(paintedEmphasis==emphasis)return;paintedEmphasis=emphasis;outline.setStroke(d(if(emphasis)2 else 1).coerceAtLeast(1),if(emphasis)colors.accent else colors.secondary)}
 override fun drawableStateChanged(){super.drawableStateChanged();if(childCount>0)paint()}
}

/** A compact inset box inside a full, stable 44-dp-or-larger hit target. */
class SearchSuggestionView(context:Context,val query:String,private val colors:BrowserTheme,val recent:Boolean):FrameLayout(context){
 private fun d(n:Int)=(n*resources.displayMetrics.density).toInt()
 private val box=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(d(10),0,d(8),0);isBaselineAligned=false}
 private val glyph=ImageView(context).apply{setImageDrawable(SurfIcon(if(recent)"history"else "search",colors.text));importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
 private var controllerFocus=false
 private var paintedEmphasis:Boolean?=null
 init{
  setDefaultFocusHighlightEnabled(false)
  contentDescription=query
  box.addView(glyph,LinearLayout.LayoutParams(d(16),d(16)).apply{rightMargin=d(8)})
  box.addView(TextView(context).apply{text=query;textSize=14f;setTextColor(colors.text);maxLines=1;ellipsize=TextUtils.TruncateAt.END;includeFontPadding=false},LinearLayout.LayoutParams(0,-2,1f))
  addView(box,LayoutParams(-1,-1).apply{topMargin=d(4);bottomMargin=d(4)})
  update(false)
 }
 fun update(focused:Boolean){controllerFocus=focused;paint()}
 private fun paint(){val emphasis=controllerFocus||isPressed;if(paintedEmphasis==emphasis)return;paintedEmphasis=emphasis;box.background=GradientDrawable().apply{setColor(this@SearchSuggestionView.colors.surface);cornerRadius=d(10).toFloat();setStroke(d(1).coerceAtLeast(1),if(emphasis)this@SearchSuggestionView.colors.accent else Color.TRANSPARENT)};glyph.imageTintList=android.content.res.ColorStateList.valueOf(if(emphasis)colors.accent else colors.text)}
 override fun drawableStateChanged(){super.drawableStateChanged();if(childCount>0)paint()}
}
