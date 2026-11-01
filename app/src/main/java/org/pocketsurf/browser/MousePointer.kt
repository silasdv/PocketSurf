package org.pocketsurf.browser
import android.content.Context
import android.graphics.*
import android.view.View
/** Compact 10x14dp arrow. Hotspot is the top-left tip. */
class MousePointer(context:Context):View(context){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 private val path=Path().apply{moveTo(1f,1f);lineTo(1f,15f);lineTo(5f,11f);lineTo(8f,18f);lineTo(11f,17f);lineTo(8f,10f);lineTo(13f,10f);close()}
 override fun onDraw(c:Canvas){val save=c.save();c.scale(resources.displayMetrics.density*.7f,resources.displayMetrics.density*.7f);p.style=Paint.Style.STROKE;p.strokeWidth=1.5f;p.strokeJoin=Paint.Join.ROUND;p.color=Color.BLACK;c.drawPath(path,p);p.style=Paint.Style.FILL;p.color=Color.WHITE;c.drawPath(path,p);c.restoreToCount(save)}
}
