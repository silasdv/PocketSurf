package org.pocketsurf.browser
import android.graphics.*
import android.graphics.drawable.Drawable
/** A saved setting has a checkmark; keyboard/controller focus uses the surrounding tile. */
class SelectedTick(private val color:Int,private val density:Float):Drawable(){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 override fun draw(c:Canvas){val r=5*density;val x=bounds.right-8*density;val y=bounds.top+8*density;p.style=Paint.Style.FILL;p.color=color;c.drawCircle(x,y,r,p);p.color=if(Color.luminance(color)>.45)Color.BLACK else Color.WHITE;p.style=Paint.Style.STROKE;p.strokeWidth=1.2f*density;p.strokeCap=Paint.Cap.ROUND;c.drawLine(x-2*density,y,x-.5f*density,y+1.5f*density,p);c.drawLine(x-.5f*density,y+1.5f*density,x+2.5f*density,y-2*density,p)}
 override fun setAlpha(a:Int){p.alpha=a}
 override fun setColorFilter(f:ColorFilter?){p.colorFilter=f}
 @Deprecated("Deprecated")override fun getOpacity()=PixelFormat.TRANSLUCENT
}
