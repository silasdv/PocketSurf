package org.pocketsurf.browser

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** A miniature browser, using the same palette as the selected theme. */
class ThemePreview(context: Context, private val theme: String, private val accent: String) : View(context) {
 private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
 override fun onDraw(canvas: Canvas) {
  val save = canvas.save()
  canvas.scale(width / 100f, height / 36f)
  fun draw(name: String) {
   val colors = BrowserTheme.resolve(name, true, accent)
   paint.color = colors.background
   canvas.drawRoundRect(0f, 0f, 100f, 36f, 4f, 4f, paint)
   paint.color = colors.surface
   canvas.drawRoundRect(5f, 4f, 35f, 11f, 2f, 2f, paint)
   canvas.drawRoundRect(5f, 14f, 95f, 22f, 3f, 3f, paint)
   paint.color = colors.accent
   canvas.drawRoundRect(9f, 7f, 25f, 8.5f, 1f, 1f, paint)
   paint.color = colors.secondary
   canvas.drawRoundRect(8f, 27f, 75f, 29f, 1f, 1f, paint)
  }
  if (theme == "Auto") {
   draw("Light")
   canvas.save()
   canvas.clipRect(50f, 0f, 100f, 36f)
   draw("Dark")
   canvas.restore()
  } else draw(theme)
  canvas.restoreToCount(save)
 }
}
