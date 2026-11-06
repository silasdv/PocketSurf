package org.pocketsurf.browser

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView

/** Shared monochrome controller hints for the browser, setup and settings. */
object ControllerBadge {
 fun create(context: Context, label: String, color: Int = Color.WHITE, fillColor:Int = Color.TRANSPARENT): View {
  if (label in listOf("L3", "R3", "RS", "LS")) return StickBadge(context, label, color, fillColor)
  if (label in listOf("cross", "circle", "square", "triangle")) return object : View(context) {
   private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    style = Paint.Style.STROKE
    strokeWidth = 2f
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
   }
   override fun onDraw(canvas: Canvas) {
    val scale = minOf(width, height) / 24f
    val save = canvas.save()
    canvas.translate((width - 24 * scale) / 2, (height - 24 * scale) / 2)
    canvas.scale(scale, scale)
    when (label) {
     "cross" -> { canvas.drawLine(7f, 7f, 17f, 17f, paint); canvas.drawLine(17f, 7f, 7f, 17f, paint) }
     "circle" -> canvas.drawCircle(12f, 12f, 7f, paint)
     "square" -> canvas.drawRoundRect(5f, 5f, 19f, 19f, 1f, 1f, paint)
     else -> canvas.drawPath(Path().apply { moveTo(12f, 4f); lineTo(21f, 20f); lineTo(3f, 20f); close() }, paint)
    }
    canvas.restoreToCount(save)
   }
  }.apply { contentDescription = label }
  return TextView(context).apply {
   text = when (label) { "L1" -> "L"; "R1" -> "R"; else -> label }
   textSize = if (label in listOf("Start", "Select") || label.length == 1) 10f else 8f
   setTextColor(color)
   typeface = Typeface.DEFAULT_BOLD
   includeFontPadding = false
   gravity = Gravity.CENTER
   contentDescription = label
   background = GradientDrawable().apply {
    setColor(fillColor)
    cornerRadius = (if (label.length == 1) 20 else 5) * resources.displayMetrics.density
    setStroke(resources.displayMetrics.density.toInt().coerceAtLeast(1), color)
   }
  }
 }
}
