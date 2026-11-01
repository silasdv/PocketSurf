package org.pocketsurf.browser

import kotlin.math.*

/** Allocate the complete available row before deciding whether titles still fit. */
data class TabStripPlan(val indices:List<Int>,val width:Int,val titles:Boolean,val closeButtons:Boolean,val hidden:Int,val showNewTabButton:Boolean=true,val remainder:Int=0){
 fun widthFor(position:Int)=width+if(position<remainder)1 else 0
 companion object {
  fun create(available:Int,count:Int,active:Int,minimumIconWidth:Int=72,minimumTitleWidth:Int=minimumIconWidth+20,newTabWidth:Int=46):TabStripPlan {
   if(count<=0)return TabStripPlan(emptyList(),155,true,true,0)
   val iconWidth=minimumIconWidth.coerceAtLeast(72)
   val plusWidth=newTabWidth.coerceAtLeast(0)
   val showPlus=count<=1||available>=iconWidth+4+plusWidth+44
   val room=(available-if(showPlus)plusWidth else 0).coerceAtLeast(0)
   val overflow=room/count-4<iconWidth
   val slots=if(overflow)((room-44)/(iconWidth+4)).coerceAtLeast(1).coerceAtMost(count)else count
   val budget=(room-if(overflow)44 else 0).coerceAtLeast(0)
   val each=(budget/slots-4).coerceAtLeast(iconWidth)
   val first=(active-slots/2).coerceIn(0,count-slots)
   val titled=each>=max(iconWidth+20,minimumTitleWidth)
   val capped=titled&&count<4&&each>max(180,iconWidth+70)
   val width=if(capped)max(180,iconWidth+70)else each
   return TabStripPlan((first until first+slots).toList(),width,titled,true,count-slots,showPlus,if(capped||budget<slots*(iconWidth+4))0 else budget%slots)
  }
 }
}
