package org.pocketsurf.browser
import android.content.Context
import android.graphics.*
import android.view.View
/** Compact 10x14dp arrow. Hotspot is the top-left tip. */
class MousePointer(context:Context,var previewStyle:String?=null):View(context){
 companion object {val styles=linkedMapOf("windows" to "Windows","macos" to "macOS","linux" to "Linux","black" to "Black · outline","white_plain" to "White · plain","black_plain" to "Black · plain")}
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 private val path=Path().apply{moveTo(1f,1f);lineTo(1f,15f);lineTo(5f,11f);lineTo(8f,18f);lineTo(11f,17f);lineTo(8f,10f);lineTo(13f,10f);close()}
 override fun onDraw(c:Canvas){
  val style=previewStyle?:context.getSharedPreferences("browser",0).getString("pointer_style","windows")!!
  path.reset();path.moveTo(1f,1f)
  if(style=="macos"){path.lineTo(1f,16f);path.lineTo(5f,12f);path.lineTo(8f,19f);path.lineTo(11f,18f);path.lineTo(8f,11f);path.lineTo(14f,11f)}
  else if(style=="linux"){path.lineTo(2f,16f);path.lineTo(6f,12f);path.lineTo(9f,19f);path.lineTo(12f,17f);path.lineTo(9f,11f);path.lineTo(15f,10f)}
  else{path.lineTo(1f,15f);path.lineTo(5f,11f);path.lineTo(8f,18f);path.lineTo(11f,17f);path.lineTo(8f,10f);path.lineTo(13f,10f)}
  path.close();val save=c.save();c.scale(width/16f,height/21f);val dark=style=="macos"||style.startsWith("black");p.strokeJoin=Paint.Join.ROUND
  if(!style.endsWith("plain")){p.style=Paint.Style.STROKE;p.strokeWidth=1.5f;p.color=if(dark)Color.WHITE else Color.BLACK;c.drawPath(path,p)}
  p.style=Paint.Style.FILL;p.color=if(dark)Color.BLACK else Color.WHITE;c.drawPath(path,p);c.restoreToCount(save)
 }
}
