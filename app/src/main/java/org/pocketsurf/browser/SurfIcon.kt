package org.pocketsurf.browser

import android.graphics.*
import android.graphics.drawable.Drawable

/** Small original vector icon set, consistent 24-unit grid and rounded strokes. */
class SurfIcon(private val name:String,private val tint:Int):Drawable(){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=tint;style=Paint.Style.STROKE;strokeWidth=1.8f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
 override fun draw(c:Canvas){val save=c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f)
  fun line(vararg points:Float){val path=Path();path.moveTo(points[0],points[1]);for(i in 2 until points.size step 2)path.lineTo(points[i],points[i+1]);c.drawPath(path,p)}
  when(name){
   "back"->line(14f,5f,7f,12f,14f,19f)
   "send"->{line(3f,4f,22f,12f,3f,20f,6f,12f,3f,4f);line(6f,12f,22f,12f)}
   "forward"->line(10f,5f,17f,12f,10f,19f)
   "search"->{c.drawCircle(10.5f,10.5f,6.5f,p);line(15.5f,15.5f,21f,21f)}
   "zoom-out"->{c.drawCircle(10f,10f,6.5f,p);line(15f,15f,21f,21f);line(7f,10f,13f,10f)}
   "zoom-in"->{c.drawCircle(10f,10f,6.5f,p);line(15f,15f,21f,21f);line(7f,10f,13f,10f);line(10f,7f,10f,13f)}
   "github"->{
    val cat=Path().apply{moveTo(6f,7f);lineTo(6f,3f);lineTo(10f,5f);quadTo(12f,4.5f,14f,5f);lineTo(18f,3f);lineTo(18f,7f);cubicTo(23f,12f,19f,17f,14.5f,17.5f);quadTo(16f,19f,16f,22f);lineTo(8f,22f);quadTo(8f,20f,8.5f,19f);cubicTo(4f,20f,3.5f,16f,1.5f,15f);cubicTo(4.5f,14f,5f,18.5f,9f,17f);cubicTo(4f,16f,2f,11f,6f,7f);close()}
    p.style=Paint.Style.FILL;c.drawPath(cat,p);p.style=Paint.Style.STROKE
   }
   "kofi"->{
    c.drawRoundRect(3f,5f,17f,18f,2f,2f,p);c.drawArc(14f,6f,22f,14f,-90f,180f,false,p);line(2f,21f,19f,21f)
    val heart=Path().apply{moveTo(10f,14.5f);cubicTo(3.5f,10f,8f,6.5f,10f,9f);cubicTo(12f,6.5f,16.5f,10f,10f,14.5f);close()}
    p.style=Paint.Style.FILL;c.drawPath(heart,p);p.style=Paint.Style.STROKE
   }
   "tabs"->{c.drawRoundRect(3f,3f,21f,21f,3f,3f,p);line(3f,8f,21f,8f);line(9f,3f,9f,8f)}
   "menu"->{line(4f,6f,20f,6f);line(4f,12f,20f,12f);line(4f,18f,20f,18f)}
   "context"->{for(y in listOf(5f,12f,19f))c.drawCircle(12f,y,1f,p)}
   "plus"->{line(12f,5f,12f,19f);line(5f,12f,19f,12f)}
   "close","stop"->{line(6f,6f,18f,18f);line(18f,6f,6f,18f)}
   "undo"->{c.drawArc(4f,4f,20f,20f,135f,290f,false,p);line(4f,4f,4f,10f,10f,10f)}
   "reload"->{c.drawArc(4f,4f,20f,20f,45f,290f,false,p);line(20f,4f,20f,10f,14f,10f)}
   "history"->{c.drawArc(4f,4f,21f,21f,210f,320f,false,p);line(3f,4f,3f,10f,9f,10f);line(12f,7f,12f,13f,16f,15f)}
   "download"->{line(12f,3f,12f,16f);line(7f,11f,12f,16f,17f,11f);line(4f,17f,4f,21f,20f,21f,20f,17f)}
   "bookmark"->line(6f,3f,18f,3f,18f,21f,12f,17f,6f,21f,6f,3f)
   "settings"->{val gear=Path();for(i in 0 until 32){val a=(i*2*Math.PI/32)-Math.PI/2;val r=if(i%4==1||i%4==2)10f else 7.5f;val x=12f+r*kotlin.math.cos(a).toFloat();val y=12f+r*kotlin.math.sin(a).toFloat();if(i==0)gear.moveTo(x,y)else gear.lineTo(x,y)};gear.close();c.drawPath(gear,p);c.drawCircle(12f,12f,3f,p)}
   "home"->{line(3f,11f,12f,3f,21f,11f);line(5f,10f,5f,21f,10f,21f,10f,15f,14f,15f,14f,21f,19f,21f,19f,10f)}
   "shield"->{val path=Path().apply{moveTo(12f,2f);lineTo(21f,6f);lineTo(20f,13f);cubicTo(19f,18f,15f,21f,12f,22f);cubicTo(9f,21f,5f,18f,4f,13f);lineTo(3f,6f);close()};c.drawPath(path,p);line(8f,12f,11f,15f,16f,9f)}
   "cookie"->{val path=Path().apply{moveTo(13f,3f);cubicTo(7f,1f,2f,6f,2f,12f);cubicTo(2f,18f,7f,22f,13f,21f);cubicTo(18f,21f,22f,17f,22f,12f);cubicTo(18f,13f,17f,10f,18f,8f);cubicTo(14f,9f,11f,6f,13f,3f);close()};c.drawPath(path,p);for(point in listOf(7f to 9f,11f to 15f,6f to 16f,16f to 17f))c.drawCircle(point.first,point.second,.8f,p)}
   "pin"->{val path=Path().apply{moveTo(8f,3f);lineTo(16f,3f);lineTo(15f,8f);lineTo(19f,12f);lineTo(19f,14f);lineTo(5f,14f);lineTo(5f,12f);lineTo(9f,8f);close()};c.drawPath(path,p);line(12f,14f,12f,22f)}
   "private"->{val mask=Path().apply{moveTo(2f,8f);quadTo(7f,5f,12f,8f);quadTo(17f,5f,22f,8f);lineTo(20f,16f);quadTo(15f,21f,12f,15f);quadTo(9f,21f,4f,16f);close()};c.drawPath(mask,p);c.drawOval(5f,10f,10f,13f,p);c.drawOval(14f,10f,19f,13f,p)}
   "privacy"->{val path=Path().apply{moveTo(12f,2f);lineTo(21f,6f);lineTo(20f,14f);quadTo(18f,20f,12f,22f);quadTo(6f,20f,4f,14f);lineTo(3f,6f);close()};c.drawPath(path,p);line(8f,8f,16f,8f);line(8f,12f,16f,12f);line(9f,16f,15f,16f)}
   "help"->{c.drawCircle(12f,12f,9f,p);val path=Path().apply{moveTo(9f,9f);cubicTo(9f,5f,16f,6f,15f,10f);cubicTo(14.5f,12f,12f,11.5f,12f,14f)};c.drawPath(path,p);c.drawCircle(12f,17f,.6f,p)}
   "setup"->{for(y in listOf(5f,12f,19f)){c.drawRoundRect(3f,y-2,7f,y+2,1f,1f,p);line(11f,y,21f,y)}}
   "globe"->{c.drawCircle(12f,12f,9f,p);c.drawOval(8f,3f,16f,21f,p);line(3f,12f,21f,12f)}
   "share"->{c.drawCircle(6f,12f,2.5f,p);c.drawCircle(18f,5f,2.5f,p);c.drawCircle(18f,19f,2.5f,p);line(8.5f,10.5f,15.5f,6.5f);line(8.5f,13.5f,15.5f,17.5f)}
   "theme"->{c.drawCircle(12f,12f,9f,p);val path=Path().apply{addArc(3f,3f,21f,21f,90f,180f);close()};p.style=Paint.Style.FILL;c.drawPath(path,p);p.style=Paint.Style.STROKE}
   "gamepad"->{c.drawRoundRect(2f,6f,22f,19f,5f,5f,p);line(6f,12f,12f,12f);line(9f,9f,9f,15f);c.drawCircle(17f,10f,1f,p);c.drawCircle(19f,14f,1f,p)}
   "controller"->line(9f,3f,15f,3f,15f,9f,21f,9f,21f,15f,15f,15f,15f,21f,9f,21f,9f,15f,3f,15f,3f,9f,9f,9f,9f,3f)
   "mouse"->{c.drawRoundRect(6f,2f,18f,22f,6f,6f,p);line(12f,2f,12f,10f);line(6f,10f,18f,10f)}
   "check"->line(5f,12f,10f,17f,20f,7f)
   "trash","close_all"->{line(4f,6f,20f,6f);line(9f,3f,15f,3f);line(6f,6f,7f,21f,17f,21f,18f,6f);line(10f,10f,10f,17f);line(14f,10f,14f,17f)}
   "lock"->{c.drawRoundRect(5f,10f,19f,21f,2f,2f,p);c.drawArc(8f,2f,16f,16f,180f,180f,false,p)}
   else->{c.drawCircle(12f,12f,9f,p);line(12f,10f,12f,17f);c.drawCircle(12f,6.5f,.5f,p)}
  };c.restoreToCount(save)
 }
 override fun setAlpha(a:Int){p.alpha=a};override fun setColorFilter(f:ColorFilter?){p.colorFilter=f};@Deprecated("Deprecated in Java") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
