package org.pocketsurf.browser
import android.content.Context
import android.graphics.*
import android.view.View
/** A thumbstick cap and short neck distinguish stick inputs from face-button circles. */
class StickBadge(context:Context,private val label:String):View(context){
 private val stroke=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;style=Paint.Style.STROKE;strokeWidth=1.3f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
 private val ink=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=8f;typeface=Typeface.create("sans-serif",Typeface.BOLD);textAlign=Paint.Align.CENTER}
 init{contentDescription=label;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
 override fun onDraw(canvas:Canvas){super.onDraw(canvas);val z=minOf(width/22f,height/18f);val save=canvas.save();canvas.translate((width-22*z)/2,(height-18*z)/2);canvas.scale(z,z);canvas.drawRoundRect(2f,1f,20f,14f,6f,6f,stroke);canvas.drawLine(8f,14f,8f,17f,stroke);canvas.drawLine(14f,14f,14f,17f,stroke);canvas.drawLine(7f,17f,15f,17f,stroke);canvas.drawText(label,11f,10.5f,ink);canvas.restoreToCount(save)}
}
