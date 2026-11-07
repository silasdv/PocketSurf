package org.pocketsurf.browser

import kotlin.math.roundToInt

/** Pixel-budget layout: scale constants once, then distribute the exact remaining pixels. */
data class SurfTabLayout(val indices:List<Int>,val widths:List<Int>,val titles:Boolean,val hidden:Int){
 companion object {
  fun create(available:Int,count:Int,active:Int,unit:Float=1f):SurfTabLayout {
   if(count<=0)return SurfTabLayout(emptyList(),emptyList(),true,0)
   fun d(value:Int)=(value*unit).roundToInt()
   val budget=available.coerceAtLeast(0);val extra=d(28);val gap=d(4);val base=d(34)
   val overflow=count*(base+gap)+extra>budget
   val slots=if(overflow)((budget-d(40)-extra)/(base+gap).coerceAtLeast(1)).coerceIn(1,count)else count
   val hidden=count-slots;val room=(budget-if(hidden>0)d(40) else 0).coerceAtLeast(0)
   val first=(active-slots/2).coerceIn(0,count-slots);val indices=(first until first+slots).toList()
   val each=((room-extra)/slots-gap).coerceAtLeast(0);val titles=each>=d(82)
   val capped=titles&&count<4;val width=if(capped)each.coerceAtMost(d(200))else each
   val remainder=if(capped)0 else (room-extra-slots*(width+gap)).coerceAtLeast(0)
   val widths=indices.mapIndexed{i,index->width+(if(index==active)extra else 0)+(if(i<remainder)1 else 0)}
   return SurfTabLayout(indices,widths,titles,hidden)
  }
 }
}
