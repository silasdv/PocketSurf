package org.pocketsurf.browser

import android.graphics.*
import android.graphics.drawable.Drawable

/** Flush drawer with an accent-colored right edge. */
class DrawerBackground(private val fill: Int, private val border: Int, private val radius: Float, private val stroke: Float) : Drawable() {
 private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
 override fun draw(canvas: Canvas) {
  val rect = RectF(bounds)
  paint.style=Paint.Style.FILL;paint.color=fill;canvas.drawRect(rect,paint)
  paint.color=border;canvas.drawRect(rect.right-stroke*3,rect.top,rect.right,rect.bottom,paint)
 }
 override fun setAlpha(alpha: Int) { paint.alpha=alpha;invalidateSelf() }
 override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter=filter;invalidateSelf() }
 @Deprecated("Deprecated by Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
