package org.pocketsurf.browser

import kotlin.math.*

/** Dp geometry: labels shrink first, then icon-only tabs, then a +N overview entry. */
data class TabStripPlan(val indices:List<Int>,val width:Int,val titles:Boolean,val closeButtons:Boolean,val hidden:Int,val showNewTabButton:Boolean=true){
 companion object {
  fun create(available:Int,count:Int,active:Int,minimumIconWidth:Int=72):TabStripPlan {
   if(count<=0)return TabStripPlan(emptyList(),155,true,true,0)
   val iconWidth=minimumIconWidth.coerceAtLeast(72)
   val showPlus=count<=1||available>=iconWidth+94
   val room=(available-if(showPlus)46 else 0).coerceAtLeast(0)
   val each=room/count-4
   if(each>=max(110,iconWidth+32))return TabStripPlan((0 until count).toList(),min(max(180,iconWidth+70),each),true,true,0,showPlus)
   if(each>=iconWidth)return TabStripPlan((0 until count).toList(),each,false,true,0,showPlus)
   val slots=((room-44)/(iconWidth+4)).coerceAtLeast(1).coerceAtMost(count)
   val first=(active-slots/2).coerceIn(0,count-slots)
   return TabStripPlan((first until first+slots).toList(),iconWidth,false,true,count-slots,showPlus)
  }
 }
}
