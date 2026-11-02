package org.pocketsurf.browser
import android.app.DownloadManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.SystemClock

data class DownloadIndicator(val active:Int,val fraction:Float?,val completed:Boolean,val failed:Boolean){
 companion object {
  fun from(items:List<SurfDownload>,seen:Set<String>):DownloadIndicator{
   val running=items.filter{it.status in listOf(DownloadManager.STATUS_PENDING,DownloadManager.STATUS_RUNNING,DownloadManager.STATUS_PAUSED)}
   val fraction=if(running.isNotEmpty()&&running.all{it.total>0})(running.sumOf{it.received.coerceAtLeast(0).toDouble()}/running.sumOf{it.total.toDouble()}).toFloat().coerceIn(0f,1f)else null
   return DownloadIndicator(running.size,fraction,items.any{it.status==DownloadManager.STATUS_SUCCESSFUL&&it.id.toString() !in seen},items.any{it.status==DownloadManager.STATUS_FAILED&&it.id.toString() !in seen})
  }
  fun color(accent:Int):Int{val hsv=FloatArray(3);Color.colorToHSV(accent,hsv);return if(hsv[0] in 185f..265f&&hsv[1]>.2f)0xff39c47c.toInt()else 0xff438cff.toInt()}
 }
}
class DownloadStatusIcon(private val state:DownloadIndicator,private val color:Int):Drawable(){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=2f;strokeCap=Paint.Cap.ROUND}
 private val glyph=SurfIcon(if(state.failed&&!state.completed)"download-error"else"download",if(state.failed&&!state.completed)0xffef7777.toInt()else color)
 private val tick=Runnable{invalidateSelf()}
 override fun draw(c:Canvas){
  if(state.active==0){unscheduleSelf(tick);glyph.bounds=bounds;glyph.draw(c);return}
  val save=c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f);p.color=(color and 0xffffff) or 0x33000000;c.drawCircle(12f,12f,9f,p);p.color=color
  val phase=(SystemClock.uptimeMillis()%1600)/1600f
  c.drawArc(3f,3f,21f,21f,if(state.fraction==null)phase*360f-90f else -90f,if(state.fraction==null)95f else maxOf(5f,state.fraction*360f),false,p)
  p.alpha=(155+100*kotlin.math.sin(phase*6.283185f)).toInt().coerceIn(55,255);c.drawCircle(12f,12f,2f,p);p.alpha=255;c.restoreToCount(save);scheduleSelf(tick,SystemClock.uptimeMillis()+16)
 }
 override fun setAlpha(alpha:Int){p.alpha=alpha;glyph.alpha=alpha}
 override fun setColorFilter(filter:ColorFilter?){}
 @Deprecated("Deprecated")override fun getOpacity()=PixelFormat.TRANSLUCENT
}
