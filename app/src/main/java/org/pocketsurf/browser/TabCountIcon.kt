package org.pocketsurf.browser
import android.graphics.*
import android.graphics.drawable.Drawable
/** One tab outline with the live count, never overlapping copy rectangles. */
class TabCountIcon(private val count:Int,color:Int):Drawable(){
 private val outline=Paint(Paint.ANTI_ALIAS_FLAG).apply{this.color=color;style=Paint.Style.STROKE;strokeWidth=1.8f}
 private val label=Paint(Paint.ANTI_ALIAS_FLAG).apply{this.color=color;typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);textAlign=Paint.Align.CENTER;textSize=if(count>99)10f else 12f}
 override fun draw(c:Canvas){val save=c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f);c.drawRoundRect(3f,2f,21f,22f,4f,4f,outline);c.drawText(if(count>99)"99+"else count.toString(),12f,12f-(label.ascent()+label.descent())/2,label);c.restoreToCount(save)}
 override fun setAlpha(a:Int){outline.alpha=a;label.alpha=a};override fun setColorFilter(f:ColorFilter?){outline.colorFilter=f;label.colorFilter=f};@Deprecated("Deprecated")override fun getOpacity()=PixelFormat.TRANSLUCENT
}
