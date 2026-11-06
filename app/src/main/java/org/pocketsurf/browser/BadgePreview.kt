package org.pocketsurf.browser
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
/** Render a legend badge at its native size and scale the whole glyph uniformly. */
class BadgePreview(context:Context,badge:View):FrameLayout(context){
 private val unit=(16*resources.displayMetrics.density).toInt()
 init{tag="neutral-controller-badge";val wide=badge.contentDescription in listOf("Start","Select");addView(badge,LayoutParams(if(wide)unit*2 else unit,unit))}
 override fun onMeasure(w:Int,h:Int){val child=getChildAt(0);child.measure(MeasureSpec.makeMeasureSpec(child.layoutParams.width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(unit,MeasureSpec.EXACTLY));setMeasuredDimension(MeasureSpec.getSize(w),MeasureSpec.getSize(h))}
 override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int){getChildAt(0).let{it.layout(0,0,it.measuredWidth,it.measuredHeight)}}
 override fun dispatchDraw(c:Canvas){val child=getChildAt(0);val scale=minOf(width.toFloat()/child.width,height.toFloat()/child.height);val save=c.save();c.translate((width-child.width*scale)/2,(height-child.height*scale)/2);c.scale(scale,scale);super.dispatchDraw(c);c.restoreToCount(save)}
}
