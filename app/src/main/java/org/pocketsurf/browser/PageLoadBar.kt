package org.pocketsurf.browser
import android.content.Context
import android.graphics.*
import android.view.View
import android.animation.ValueAnimator

class PageLoadBar(context:Context):View(context){
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 private var amount=0f
 private var animation:ValueAnimator?=null
 private var owner:String?=null
 private var target=-1
 fun update(id:String?,progress:Int,color:Int,loading:Boolean){
  paint.color=color
  if(owner!=id){animation?.cancel();owner=id;amount=0f;target=-1}
  val next=if(loading)progress.coerceIn(2,99)else 100
  if(id==null){animation?.cancel();visibility=INVISIBLE;target=-1;amount=0f;return}
  if(next==target){invalidate();return};animation?.cancel();target=next
  if(!loading&&visibility!=VISIBLE){amount=0f;return};visibility=VISIBLE
  animation=ValueAnimator.ofFloat(amount,next/100f).apply{duration=160;addUpdateListener{amount=it.animatedValue as Float;invalidate();if(amount>=1f&&target==100)visibility=INVISIBLE};start()}
 }
 override fun onDraw(canvas:Canvas){canvas.drawRect(0f,0f,width*amount,height.toFloat(),paint)}
 override fun onDetachedFromWindow(){animation?.cancel();super.onDetachedFromWindow()}
}
