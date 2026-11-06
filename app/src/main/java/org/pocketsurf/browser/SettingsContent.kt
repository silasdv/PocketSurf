package org.pocketsurf.browser
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlin.math.roundToInt
/** Measure natural content first, then use spare vertical space without changing widths. */
class SettingsContent(context:Context):LinearLayout(context){
 var expandRows=true
 private data class Size(val height:Int,val top:Int,val bottom:Int)
 private val sizes=java.util.WeakHashMap<View,Size>()
 private fun visit(group:ViewGroup,scale:Float){for(i in 0 until group.childCount){val v=group.getChildAt(i);val lp=v.layoutParams
 val m=lp as? ViewGroup.MarginLayoutParams
 val original=sizes.getOrPut(v){Size(lp.height,m?.topMargin?:0,m?.bottomMargin?:0)}
 if(original.height>0)lp.height=if(v is MousePointer||v.tag=="settings-heading")original.height else (original.height*scale).roundToInt()
 m?.topMargin=original.top;m?.bottomMargin=original.bottom
 if(v is ViewGroup)visit(v,scale)
 if(original.height>0&&v is LinearLayout&&v.orientation==VERTICAL){
 val children=(0 until v.childCount).map{v.getChildAt(it).layoutParams}
 if(children.all{it.height>0})lp.height=maxOf(lp.height,v.paddingTop+v.paddingBottom+children.sumOf{it.height+((it as? ViewGroup.MarginLayoutParams)?.let{m->m.topMargin+m.bottomMargin}?:0)})
 }
 }}
 override fun onMeasure(w:Int,h:Int){visit(this,1f);super.onMeasure(w,MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));val natural=measuredHeight
 val available=MeasureSpec.getSize(h)
 if(expandRows&&natural>0&&available>natural){val factor=(available*.96f/natural).coerceIn(1f,1.8f);visit(this,factor)}
 super.onMeasure(w,h)
 }
}
