package org.pocketsurf.browser
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
/** Square colour targets with matching horizontal and vertical rhythm. */
class AccentRow(context:Context):LinearLayout(context){
 init{orientation=HORIZONTAL;gravity=Gravity.CENTER;isBaselineAligned=false}
 override fun onMeasure(w:Int,h:Int){val gap=(6*resources.displayMetrics.density).toInt();val size=minOf(MeasureSpec.getSize(h),(MeasureSpec.getSize(w)-gap*(childCount-1))/childCount.coerceAtLeast(1))
 for(i in 0 until childCount){(getChildAt(i).layoutParams as LayoutParams).apply{width=size;weight=0f;leftMargin=if(i==0)0 else gap}}
 super.onMeasure(w,h)
 }
}
