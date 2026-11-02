package org.pocketsurf.browser

import android.view.View
import android.view.ViewGroup
import java.time.Duration

/** Existing feature fixtures enter through the visible actions; SurfBarRegressionTest tests physical keys. */
internal object BrowserTestActions {
 fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
 fun field(a:BrowserActivity,n:String)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==args.size}.apply{isAccessible=true}.invoke(a,*args)
 fun settle(a:BrowserActivity){repeat(2){a.window.decorView.apply{val d=a.resources.displayMetrics.density;val c=a.resources.configuration;measure(View.MeasureSpec.makeMeasureSpec((c.screenWidthDp*d).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((c.screenHeightDp*d).toInt(),View.MeasureSpec.EXACTLY));layout(0,0,measuredWidth,measuredHeight)};TestFrameClock.advance(Duration.ofMillis(32))}}
 private fun click(a:BrowserActivity,label:String){views(a.window.decorView).first{it.isShown&&it.contentDescription==label&&it.hasOnClickListeners()}.performClick();settle(a)}
 fun openTabs(a:BrowserActivity)=click(a,"Open tabs")
 fun newTab(a:BrowserActivity)=click(a,"New tab")
 fun focusAddress(a:BrowserActivity){BrowserActivity::class.java.getDeclaredMethod("focusChrome").apply{isAccessible=true}.invoke(a);settle(a)}
}
