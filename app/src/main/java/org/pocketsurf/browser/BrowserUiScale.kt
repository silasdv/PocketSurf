package org.pocketsurf.browser

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.WindowInsets
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * One design unit system for native browser UI only. WebView and system windows retain the
 * Activity's real resources. The IME reduces layout space, never the global design scale.
 */
class BrowserUiScale(private val activity:Activity) {
 companion object {
  const val REFERENCE_WIDTH_DP=832f
  const val REFERENCE_HEIGHT_DP=468f
  fun factor(widthPx:Int,heightPx:Int,density:Float):Float {
   require(widthPx>0&&heightPx>0&&density>0&&density.isFinite())
   return min(widthPx/density/REFERENCE_WIDTH_DP,heightPx/density/REFERENCE_HEIGHT_DP)
  }
 }
 data class Viewport(val width:Int,val height:Int,val density:Float,val fontScale:Float,val uiMode:Int){
  val factor get()=factor(width,height,density)
  val uiDensity get()=density*factor
 }
 private var systemInsets=Rect()
 var viewport=measure();private set
 private val nativeContext=NativeContext(activity,viewport)
 val context:Context get()=nativeContext
 val resources get()=nativeContext.resources
 init{logViewport()}
 private fun logViewport(){if(BuildConfig.DEBUG)android.util.Log.d("PocketSurfScale","window=${viewport.width}x${viewport.height} baseDensity=${viewport.density} fontScale=${viewport.fontScale} scale=${viewport.factor} uiDensity=${viewport.uiDensity}")}
 fun refreshTheme(){nativeContext.resetTheme()}
 /** A real window/density/font change, not an IME/content-height animation. */
 fun refresh(keepSystemInsets:Boolean=false):Boolean {
  val next=measure(keepSystemInsets)
  if(next==viewport)return false
  viewport=next;nativeContext.update(next);logViewport();return true
 }
 private fun measure(keepSystemInsets:Boolean=false):Viewport {
  val base=activity.resources
  val width:Int;val height:Int
  if(Build.VERSION.SDK_INT>=30){
   val metrics=activity.windowManager.currentWindowMetrics
   val wi=metrics.windowInsets
   if(!keepSystemInsets&&!wi.isVisible(WindowInsets.Type.ime())){
    val bars=wi.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
    systemInsets=Rect(bars.left,bars.top,bars.right,bars.bottom)
   }
   width=metrics.bounds.width()-systemInsets.left-systemInsets.right
   height=metrics.bounds.height()-systemInsets.top-systemInsets.bottom
  }else{
   val dm=DisplayMetrics();@Suppress("DEPRECATION") activity.windowManager.defaultDisplay.getMetrics(dm)
   width=dm.widthPixels;height=dm.heightPixels
  }
  return Viewport(width.coerceAtLeast(1),height.coerceAtLeast(1),base.displayMetrics.density,base.configuration.fontScale,base.configuration.uiMode)
 }
 /** Private resources: never mutate Resources.getSystem(), Activity or application density. */
 private class NativeContext(base:Context,viewport:Viewport):ContextWrapper(base){
  private var nativeResources=makeResources(viewport)
  private var nativeTheme:Resources.Theme?=null
  private fun makeResources(v:Viewport):Resources {
   val original=baseContext.resources
   val metrics=DisplayMetrics().apply{
    setTo(original.displayMetrics)
    density=v.uiDensity;densityDpi=(density*160).roundToInt()
    scaledDensity=original.displayMetrics.scaledDensity*v.factor
    widthPixels=v.width;heightPixels=v.height
   }
   val config=Configuration(original.configuration).apply{
    densityDpi=metrics.densityDpi
    screenWidthDp=(v.width/v.uiDensity).roundToInt()
    screenHeightDp=(v.height/v.uiDensity).roundToInt()
    smallestScreenWidthDp=min(screenWidthDp,screenHeightDp)
   }
   @Suppress("DEPRECATION") val result=Resources(original.assets,metrics,config)
   // Resources normalizes density to integer DPI. Keep the private unit conversion continuous;
   // densityDpi still selects the nearest Android resource bucket. Never touch base resources.
   result.displayMetrics.density=v.uiDensity
   result.displayMetrics.scaledDensity=original.displayMetrics.scaledDensity*v.factor
   return result
  }
  fun resetTheme(){nativeTheme=null}
  fun update(v:Viewport){nativeResources=makeResources(v);resetTheme()}
  override fun getResources()=nativeResources
  override fun getAssets()=nativeResources.assets
  override fun getTheme():Resources.Theme=nativeTheme?:nativeResources.newTheme().apply{setTo(baseContext.theme);nativeTheme=this}
  override fun setTheme(resid:Int){theme.applyStyle(resid,true)}
  override fun getSystemService(name:String):Any?=if(name==Context.LAYOUT_INFLATER_SERVICE)LayoutInflater.from(baseContext).cloneInContext(this)else super.getSystemService(name)
 }
}
