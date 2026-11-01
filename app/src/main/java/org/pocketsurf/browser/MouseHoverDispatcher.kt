package org.pocketsurf.browser

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.webkit.WebView

/** Real mouse input, including CSS :hover; never fabricate DOM mouseover events. */
class MouseHoverDispatcher {
 private var current:WebView?=null;private var px=Float.NaN;private var py=Float.NaN
 private fun event(v:WebView,action:Int,x:Float,y:Float,pressed:Boolean=false,downTime:Long=SystemClock.uptimeMillis(),touch:Boolean=false){val now=SystemClock.uptimeMillis();val p=MotionEvent.PointerProperties().apply{id=0;toolType=if(touch)MotionEvent.TOOL_TYPE_FINGER else MotionEvent.TOOL_TYPE_MOUSE};val c=MotionEvent.PointerCoords().apply{this.x=x;this.y=y;pressure=if(pressed)1f else 0f};val e=MotionEvent.obtain(downTime,now,action,1,arrayOf(p),arrayOf(c),0,if(pressed&&!touch)MotionEvent.BUTTON_PRIMARY else 0,1f,1f,0,0,if(touch)InputDevice.SOURCE_TOUCHSCREEN else InputDevice.SOURCE_MOUSE,0);try{if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_UP)v.dispatchTouchEvent(e)else v.dispatchGenericMotionEvent(e)}finally{e.recycle()}}
 fun move(v:WebView,x:Float,y:Float,force:Boolean=false){var entered=false;if(current!==v){leave();entered=true;current=v;px=x;py=y;event(v,MotionEvent.ACTION_HOVER_ENTER,x,y)};if(entered||force||px!=x||py!=y){px=x;py=y;event(v,MotionEvent.ACTION_HOVER_MOVE,x,y)}}
 fun leave(){val v=current?:return;current=null;event(v,MotionEvent.ACTION_HOVER_EXIT,px,py);px=Float.NaN;py=Float.NaN}
 // WebView consumes mouse clicks through button events whose actionButton cannot be
 // populated through the public MotionEvent API. Use a trusted native tap for activation.
 fun click(v:WebView,x:Float,y:Float){move(v,x,y);val down=SystemClock.uptimeMillis();event(v,MotionEvent.ACTION_DOWN,x,y,true,down,true);event(v,MotionEvent.ACTION_UP,x,y,false,down,true);move(v,x,y,true)}
}
