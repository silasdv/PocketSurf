package org.pocketsurf.browser
import android.graphics.*
import android.graphics.drawable.Drawable
/** Checkbox drawn at the same intrinsic size as an action-chip icon. */
class CheckboxIcon(private val color:Int,private val size:Int):Drawable(){
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 override fun getIntrinsicWidth()=size
 override fun getIntrinsicHeight()=size
 override fun isStateful()=true
 override fun onStateChange(state:IntArray):Boolean{invalidateSelf();return true}
 override fun draw(c:Canvas){val save=c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/18f,bounds.height()/18f);paint.color=color;paint.style=Paint.Style.STROKE;paint.strokeWidth=1.5f;paint.strokeCap=Paint.Cap.ROUND;paint.strokeJoin=Paint.Join.ROUND;c.drawRoundRect(1.5f,1.5f,16.5f,16.5f,3f,3f,paint);if(state.contains(android.R.attr.state_checked)){val path=Path();path.moveTo(5f,9f);path.lineTo(8f,12f);path.lineTo(13f,6f);c.drawPath(path,paint)};c.restoreToCount(save)}
 override fun setAlpha(a:Int){paint.alpha=a}
 override fun setColorFilter(f:ColorFilter?){paint.colorFilter=f}
 @Deprecated("Deprecated")override fun getOpacity()=PixelFormat.TRANSLUCENT
}
