package org.pocketsurf.browser
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.view.View

/** Original vector shapes. Every style shares the top-left hotspot. */
class MousePointer(context:Context,var previewStyle:String?=null):View(context){
 companion object {
  val styles=linkedMapOf("windows" to "Classic","pixel" to "Pixel","rock" to "Rock hand","diamond" to "Diamond")
  val legacy=setOf("linux","black","white_plain","black_plain")
  fun form(p:SharedPreferences)=p.getString("pointer_style","windows").let{if(it in styles)it!! else "windows"}
  fun fill(p:SharedPreferences)=p.getString("pointer_fill",null)?:if(p.getString("pointer_style","") in setOf("macos","black","black_plain"))"black"else"white"
  fun outline(p:SharedPreferences)=p.getString("pointer_outline",null)?:if(p.getString("pointer_style","")?.endsWith("plain")==true)"none"else if(fill(p)=="black")"white"else"black"
 }
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 private val emp by lazy { CursorAsset.load(context) }
 private var coloredHand:Bitmap?=null
 private var handColors:Pair<Int,Int>?=null
 private fun hand(prefs:SharedPreferences):Bitmap {
  val fillColor=when(fill(prefs)){"black"->Color.BLACK;"accent"->BrowserTheme.resolve(prefs.getString("theme","OLED")!!,resources.configuration.uiMode and 48==32,prefs.getString("accent",null)).accent;else->Color.WHITE}
  val edgeColor=when(outline(prefs)){"white"->Color.WHITE;"none"->Color.TRANSPARENT;else->Color.BLACK}
  val key=fillColor to edgeColor;if(handColors==key)return coloredHand!!
  val pixels=IntArray(emp.width*emp.height);emp.getPixels(pixels,0,emp.width,0,0,emp.width,emp.height)
  for(i in pixels.indices)if(Color.alpha(pixels[i])>0)pixels[i]=if(Color.red(pixels[i])>127)fillColor else edgeColor
  return Bitmap.createBitmap(pixels,emp.width,emp.height,Bitmap.Config.ARGB_8888).also{coloredHand=it;handColors=key}
 }
 private val empBounds by lazy {var left=emp.width;var top=emp.height;var right=0;var bottom=0;for(y in 0 until emp.height)for(x in 0 until emp.width)if(Color.alpha(emp.getPixel(x,y))>0){left=minOf(left,x);top=minOf(top,y);right=maxOf(right,x+1);bottom=maxOf(bottom,y+1)};Rect(left,top,right,bottom)}
 val hotspotX:Float get()=if((previewStyle?:form(context.getSharedPreferences("browser",0)))=="rock")11f*minOf(width.toFloat()/emp.width,height.toFloat()/emp.height)else 0f
 val hotspotY:Float get()=if((previewStyle?:form(context.getSharedPreferences("browser",0)))=="rock")5f*minOf(width.toFloat()/emp.width,height.toFloat()/emp.height)else 0f
 private val path=Path()
 override fun onDraw(c:Canvas){
  val prefs=context.getSharedPreferences("browser",0);val style=previewStyle?:form(prefs);if(style=="rock"){val artwork=hand(prefs);p.isAntiAlias=false;p.isFilterBitmap=false;val scale=minOf(width.toFloat()/emp.width,height.toFloat()/emp.height);if(previewStyle!=null){val rect=empBounds;val fit=minOf(width.toFloat()/rect.width(),height.toFloat()/rect.height());val w=rect.width()*fit;val h=rect.height()*fit;c.drawBitmap(artwork,rect,RectF((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2),p)}else c.drawBitmap(artwork,null,RectF(0f,0f,emp.width*scale,emp.height*scale),p);return};path.reset();path.moveTo(1f,1f)
  when(style){
   "macos"->{path.lineTo(8f,21f);path.lineTo(10f,12f);path.lineTo(17f,9f)}
   "pixel"->{path.lineTo(1f,17f);path.lineTo(4f,17f);path.lineTo(4f,14f);path.lineTo(6f,14f);path.lineTo(6f,18f);path.lineTo(8f,18f);path.lineTo(8f,21f);path.lineTo(12f,21f);path.lineTo(12f,17f);path.lineTo(10f,17f);path.lineTo(10f,13f);path.lineTo(16f,13f);path.lineTo(16f,10f);path.lineTo(13f,10f);path.lineTo(13f,7f);path.lineTo(10f,7f);path.lineTo(10f,5f);path.lineTo(7f,5f);path.lineTo(7f,3f);path.lineTo(4f,3f);path.lineTo(4f,1f)}
   "rock"->{path.lineTo(5f,1f);path.lineTo(5f,9f);path.lineTo(7f,9f);path.lineTo(7f,7f);path.lineTo(10f,7f);path.lineTo(10f,9f);path.lineTo(12f,9f);path.lineTo(12f,3f);path.lineTo(15f,3f);path.lineTo(15f,16f);path.lineTo(13f,16f);path.lineTo(13f,21f);path.lineTo(5f,21f);path.lineTo(5f,18f);path.lineTo(3f,18f);path.lineTo(3f,15f);path.lineTo(1f,15f);path.lineTo(1f,11f)}
   "diamond"->{path.lineTo(16f,8f);path.lineTo(11f,12f);path.lineTo(8f,21f);path.lineTo(5f,11f)}
   else->{path.lineTo(1f,15f);path.lineTo(5f,11f);path.lineTo(8f,18f);path.lineTo(11f,17f);path.lineTo(8f,10f);path.lineTo(13f,10f)}
  };path.close();val save=c.save();val scale=minOf(width/18f,height/23f);if(previewStyle!=null)c.translate((width-18f*scale)/2,(height-23f*scale)/2);c.scale(scale,scale);p.isAntiAlias=style !in setOf("pixel","rock");p.strokeJoin=if(style in setOf("pixel","rock"))Paint.Join.MITER else Paint.Join.ROUND
  val edge=outline(prefs);if(edge!="none"){p.style=Paint.Style.STROKE;p.strokeWidth=1.4f;p.color=if(edge=="white")Color.WHITE else Color.BLACK;c.drawPath(path,p)}
  p.style=Paint.Style.FILL;p.color=when(fill(prefs)){"black"->Color.BLACK;"accent"->BrowserTheme.resolve(prefs.getString("theme","OLED")!!,resources.configuration.uiMode and 48==32,prefs.getString("accent",null)).accent;else->Color.WHITE};c.drawPath(path,p)
  if(style=="rock"){p.style=Paint.Style.STROKE;p.strokeWidth=.5f;p.color=if(p.color==Color.BLACK)Color.WHITE else Color.BLACK;c.drawLine(6f,12f,7f,16f,p);c.drawLine(9f,12f,9f,16f,p)}
  c.restoreToCount(save)
 }
}
