package org.pocketsurf.browser
import android.graphics.*
import android.graphics.drawable.Drawable
/** Saved value badge overlays the tile; it never changes content measurement. */
class SelectedTick(private val color:Int,private val density:Float):Drawable(){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 override fun draw(c:Canvas){drawCheck(c,bounds.right-9*density,bounds.top+9*density,5.5f*density,color,p)}
 override fun setAlpha(a:Int){p.alpha=a}
 override fun setColorFilter(f:ColorFilter?){p.colorFilter=f}
 @Deprecated("Deprecated")override fun getOpacity()=PixelFormat.TRANSLUCENT
}
internal fun drawCheck(c:Canvas,x:Float,y:Float,r:Float,color:Int,p:Paint){
 p.style=Paint.Style.FILL;p.color=if(Color.luminance(color)>.5)Color.WHITE else Color.BLACK;c.drawCircle(x,y,r,p)
 p.style=Paint.Style.STROKE;p.strokeWidth=r*.25f;p.strokeCap=Paint.Cap.ROUND;p.strokeJoin=Paint.Join.ROUND;p.color=if(Color.luminance(color)>.5)Color.BLACK else Color.WHITE
 val path=Path();path.moveTo(x-r*.48f,y);path.lineTo(x-r*.12f,y+r*.32f);path.lineTo(x+r*.5f,y-r*.34f);c.drawPath(path,p)
}
class AccentSelection(private val color:Int,private val accent:Int,private val focused:Boolean,private val selected:Boolean,private val density:Float):Drawable(){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 override fun draw(c:Canvas){val x=bounds.exactCenterX();val y=bounds.exactCenterY();val r=minOf(bounds.width(),bounds.height())*.36f
 if(focused){p.style=Paint.Style.STROKE;p.strokeWidth=2*density;p.color=color;c.drawCircle(x,y,r+3*density,p)}
 if(selected)drawCheck(c,x,y,6*density,color,p)
 }
 override fun setAlpha(a:Int){p.alpha=a}
 override fun setColorFilter(f:ColorFilter?){p.colorFilter=f}
 @Deprecated("Deprecated")override fun getOpacity()=PixelFormat.TRANSLUCENT
}
