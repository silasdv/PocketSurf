package org.pocketsurf.browser

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import kotlin.math.roundToInt

/** Consistent leading actions and trailing navigation clusters; narrow overflow can scroll. */
class SpacedLegendLayout(context:Context):LinearLayout(context){
 init{orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
 override fun onLayout(changed:Boolean,l:Int,t:Int,r:Int,b:Int){
  val children=(0 until childCount).map{getChildAt(it)}.filter{it.visibility!=View.GONE};if(children.isEmpty())return
  val leading=children.filter{it.tag!="legend-trailing"};val trailing=children.filter{it.tag=="legend-trailing"};val gap=(16*resources.displayMetrics.density).roundToInt();val edge=gap
  fun used(group:List<View>)=group.sumOf{val p=it.layoutParams as LayoutParams;it.measuredWidth+p.leftMargin+p.rightMargin}+gap*(group.size-1).coerceAtLeast(0)
  if(used(leading)+used(trailing)+2*edge+(if(leading.isNotEmpty()&&trailing.isNotEmpty())gap else 0)>width-paddingLeft-paddingRight){super.onLayout(changed,l,t,r,b);return}
  fun place(group:List<View>,start:Int){var x=start;for(child in group){val p=child.layoutParams as LayoutParams;x+=p.leftMargin;val y=paddingTop+(height-paddingTop-paddingBottom-child.measuredHeight)/2;child.layout(x,y,x+child.measuredWidth,y+child.measuredHeight);x+=child.measuredWidth+p.rightMargin+gap}}
  place(leading,paddingLeft+edge);place(trailing,width-paddingRight-edge-used(trailing))
 }
}
