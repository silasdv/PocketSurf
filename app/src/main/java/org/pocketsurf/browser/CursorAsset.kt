package org.pocketsurf.browser
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
/** Decode the supplied monochrome CUR including its AND transparency mask. */
object CursorAsset {
 fun load(context:Context):Bitmap {
 val bytes=context.resources.openRawResource(R.raw.metalhand).use{it.readBytes()}
 val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
 val start=b.getInt(18);val width=b.getInt(start+4);val height=b.getInt(start+8)/2
 require(b.getShort(start+14).toInt()==1)
 val palette=start+b.getInt(start);val pixels=palette+8;val stride=((width+31)/32)*4;val mask=pixels+stride*height
 fun bit(offset:Int,x:Int,y:Int)=(bytes[offset+(height-1-y)*stride+x/8].toInt() ushr (7-x%8)) and 1
 val colors=IntArray(width*height){i->val x=i%width;val y=i/width;if(bit(mask,x,y)==1)Color.TRANSPARENT else {val at=palette+bit(pixels,x,y)*4;Color.rgb(bytes[at+2].toInt() and 255,bytes[at+1].toInt() and 255,bytes[at].toInt() and 255)}}
 return Bitmap.createBitmap(colors,width,height,Bitmap.Config.ARGB_8888)
 }
}
