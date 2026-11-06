package org.pocketsurf.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import java.net.URI

/** Local icon presentation. Never recolors a site's artwork and never starts a network request. */
class SiteIconView(context:Context,val url:String,val sizePx:Int,val dark:Boolean):FrameLayout(context) {
 private val artwork=ImageView(context).apply{scaleType=ImageView.ScaleType.FIT_CENTER;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
 private val initial=TextView(context).apply{gravity=Gravity.CENTER;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
 init {
  minimumWidth=sizePx;minimumHeight=sizePx
  layoutParams=LayoutParams(sizePx,sizePx)
  val inset=(sizePx*.14f).toInt().coerceAtLeast(1)
  addView(initial,LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT))
  addView(artwork,LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT).apply{setMargins(inset,inset,inset,inset)})
  contentDescription=null
  refresh(null)
 }
 fun refresh(bitmap:Bitmap?) {
  if(url=="pocketsurf:new-tab") {
   artwork.visibility=GONE
   initial.visibility=VISIBLE
   initial.text="PS"
   initial.textSize=sizePx/context.resources.displayMetrics.scaledDensity*.42f
   initial.setTextColor(Color.BLACK)
   background=tile(Color.WHITE)
   return
  }
  val valid=bitmap?.takeUnless{it.isRecycled}
  if(valid==null){
   artwork.setImageDrawable(null);artwork.visibility=GONE;initial.visibility=VISIBLE
   val identity=identity(url);initial.text=identity.first;initial.textSize=(sizePx/context.resources.displayMetrics.scaledDensity*.48f)
   background=tile(identity.second)
  }else{
   initial.visibility=GONE;artwork.visibility=VISIBLE;artwork.setImageBitmap(valid)
   background=tile(backingColor(valid,dark))
  }
 }
 private fun tile(color:Int)=GradientDrawable().apply{setColor(color);cornerRadius=sizePx*.22f;setStroke((resources.displayMetrics.density*.75f).toInt().coerceAtLeast(1),if(dark)0x337F8DA6 else 0x227F8DA6)}
 companion object {
  @JvmStatic fun create(context:Context,url:String,sizePx:Int,dark:Boolean)=SiteIconView(context,url,sizePx,dark)
  /** Branded initials are intentional placeholders, never claimed to be official logos. */
  internal fun identity(url:String):Pair<String,Int> {
   val host=try{URI(url).host?.lowercase()?.removePrefix("www.").orEmpty()}catch(_:Exception){""}
   val known=listOf("google.com" to ("G" to 0xFF3267C8.toInt()),"bing.com" to ("b" to 0xFF007A83.toInt()),"duckduckgo.com" to ("D" to 0xFFBB4026.toInt()),"brave.com" to ("B" to 0xFFBA390F.toInt()),"ecosia.org" to ("E" to 0xFF28734A.toInt()),"startpage.com" to ("S" to 0xFF5745B8.toInt()),"yahoo.com" to ("Y" to 0xFF6A28B8.toInt()),"wikipedia.org" to ("W" to 0xFF46505B.toInt()),"youtube.com" to ("Y" to 0xFFC42525.toInt()),"reddit.com" to ("R" to 0xFFB83C12.toInt()),"github.com" to ("GH" to 0xFF424954.toInt()),"twitch.tv" to ("T" to 0xFF7640B8.toInt()),"amazon.com" to ("a" to 0xFF815309.toInt()),"stackoverflow.com" to ("S" to 0xFFA74C13.toInt()))
   known.firstOrNull{host==it.first||host.endsWith("."+it.first)}?.let{return it.second}
   val palette=intArrayOf(0xFF375DA8.toInt(),0xFF347656.toInt(),0xFF82519B.toInt(),0xFF976023.toInt(),0xFF9A445D.toInt(),0xFF286D80.toInt())
   return (host.firstOrNull()?.uppercaseChar()?.toString()?:"?") to palette[(host.hashCode() and Int.MAX_VALUE)%palette.size]
  }
  /** Alpha-weighted artwork luminance chooses a neutral backing with useful contrast. */
  internal fun backingColor(bitmap:Bitmap,dark:Boolean):Int {
   var weight=0.0;var luminance=0.0
   val stepX=(bitmap.width/24).coerceAtLeast(1);val stepY=(bitmap.height/24).coerceAtLeast(1)
   for(y in 0 until bitmap.height step stepY)for(x in 0 until bitmap.width step stepX){val c=bitmap.getPixel(x,y);val a=Color.alpha(c)/255.0;if(a<.08)continue;weight+=a;luminance+=a*(.2126*linear(Color.red(c))+.7152*linear(Color.green(c))+.0722*linear(Color.blue(c)))}
   if(weight==0.0)return if(dark)0xFF303743.toInt() else 0xFFF0F2F5.toInt()
   return if(luminance/weight<.36)0xFFF1F3F5.toInt() else if(luminance/weight>.65)0xFF303743.toInt() else if(dark)0xFFE6E9EE.toInt() else 0xFF292F39.toInt()
  }
  private fun linear(channel:Int):Double {val v=channel/255.0;return if(v<=.04045)v/12.92 else Math.pow((v+.055)/1.055,2.4)}
 }
}
