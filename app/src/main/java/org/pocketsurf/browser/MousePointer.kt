package org.pocketsurf.browser
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.view.View

/** Original vector shapes. Every style shares the top-left hotspot. */
class MousePointer(context:Context,var previewStyle:String?=null):View(context){
 companion object {
  val styles=linkedMapOf("windows" to "Classic","macos" to "Compact","pixel" to "Pixel","rock" to "Rock hand","diamond" to "Diamond")
  val legacy=setOf("linux","black","white_plain","black_plain")
  fun form(p:SharedPreferences)=p.getString("pointer_style","windows").let{if(it in styles)it!! else "windows"}
  fun fill(p:SharedPreferences)=p.getString("pointer_fill",null)?:if(p.getString("pointer_style","") in setOf("macos","black","black_plain"))"black"else"white"
  fun outline(p:SharedPreferences)=p.getString("pointer_outline",null)?:if(p.getString("pointer_style","")?.endsWith("plain")==true)"none"else if(fill(p)=="black")"white"else"black"
 }
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 private val path=Path()
 override fun onDraw(c:Canvas){
  val prefs=context.getSharedPreferences("browser",0);val style=previewStyle?:form(prefs);path.reset();path.moveTo(1f,1f)
  when(style){
   "macos"->{path.lineTo(1f,17f);path.lineTo(5f,12f);path.lineTo(8f,20f);path.lineTo(11f,19f);path.lineTo(8f,11f);path.lineTo(15f,11f)}
   "pixel"->{path.lineTo(1f,17f);path.lineTo(4f,17f);path.lineTo(4f,14f);path.lineTo(6f,14f);path.lineTo(6f,18f);path.lineTo(8f,18f);path.lineTo(8f,21f);path.lineTo(12f,21f);path.lineTo(12f,17f);path.lineTo(10f,17f);path.lineTo(10f,13f);path.lineTo(16f,13f);path.lineTo(16f,10f);path.lineTo(13f,10f);path.lineTo(13f,7f);path.lineTo(10f,7f);path.lineTo(10f,5f);path.lineTo(7f,5f);path.lineTo(7f,3f);path.lineTo(4f,3f);path.lineTo(4f,1f)}
   "rock"->{path.lineTo(1f,12f);path.lineTo(3f,17f);path.lineTo(5f,21f);path.lineTo(12f,21f);path.lineTo(15f,16f);path.lineTo(16f,3f);path.quadTo(15f,1f,14f,3f);path.lineTo(13f,11f);path.lineTo(11f,10f);path.lineTo(10f,7f);path.quadTo(9f,6f,8f,8f);path.lineTo(8f,10f);path.lineTo(7f,7f);path.quadTo(5f,5f,5f,9f);path.lineTo(4f,11f);path.lineTo(3f,1f)}
   "diamond"->{path.lineTo(16f,8f);path.lineTo(11f,12f);path.lineTo(8f,21f);path.lineTo(5f,11f)}
   else->{path.lineTo(1f,15f);path.lineTo(5f,11f);path.lineTo(8f,18f);path.lineTo(11f,17f);path.lineTo(8f,10f);path.lineTo(13f,10f)}
  };path.close();val save=c.save();c.scale(width/18f,height/23f);p.strokeJoin=if(style=="pixel")Paint.Join.MITER else Paint.Join.ROUND
  val edge=outline(prefs);if(edge!="none"){p.style=Paint.Style.STROKE;p.strokeWidth=1.4f;p.color=if(edge=="white")Color.WHITE else Color.BLACK;c.drawPath(path,p)}
  p.style=Paint.Style.FILL;p.color=when(fill(prefs)){"black"->Color.BLACK;"accent"->BrowserTheme.resolve(prefs.getString("theme","Dark")!!,resources.configuration.uiMode and 48==32,prefs.getString("accent",null)).accent;else->Color.WHITE};c.drawPath(path,p)
  if(style=="rock"){p.style=Paint.Style.STROKE;p.strokeWidth=.5f;p.color=if(p.color==Color.BLACK)Color.WHITE else Color.BLACK;c.drawLine(6f,12f,7f,16f,p);c.drawLine(9f,12f,9f,16f,p)}
  c.restoreToCount(save)
 }
}
