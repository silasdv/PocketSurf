package org.pocketsurf.browser

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.max

/** Consumes controller motion at dispatch, before WebView/Android can turn it into scroll. */
class ControllerInput {
 data class Axes(val leftX:Float=0f,val leftY:Float=0f,val rightX:Float=0f,val rightY:Float=0f,val hatX:Float=0f,val hatY:Float=0f,val leftTrigger:Float=0f,val rightTrigger:Float=0f)
 var axes=Axes();private set
 private var heldDirection:String?=null
 private var nextRepeat=0L
 private var leftDown=false;private var rightDown=false
 private var lastLeft=-1000L;private var lastRight=-1000L
 fun reset(){axes=Axes();heldDirection=null;nextRepeat=0;leftDown=false;rightDown=false}
 fun read(e:MotionEvent):Axes {
  val ranges=e.device?.motionRanges.orEmpty().map {it.axis}.toSet()
  val zr=ranges.contains(MotionEvent.AXIS_Z)&&ranges.contains(MotionEvent.AXIS_RZ)
  val rxry=ranges.contains(MotionEvent.AXIS_RX)&&ranges.contains(MotionEvent.AXIS_RY)
  // Never use left X/Y as a right-stick fallback. Metadata decides; unregistered test/virtual devices may expose either pair.
  val rx=if(!zr&&rxry)e.getAxisValue(MotionEvent.AXIS_RX) else e.getAxisValue(MotionEvent.AXIS_Z)
  val ry=if(!zr&&rxry)e.getAxisValue(MotionEvent.AXIS_RY) else e.getAxisValue(MotionEvent.AXIS_RZ)
  val fallback=ranges.isEmpty()&&max(abs(rx),abs(ry))==0f
  return Axes(e.getAxisValue(MotionEvent.AXIS_X),e.getAxisValue(MotionEvent.AXIS_Y),if(fallback)e.getAxisValue(MotionEvent.AXIS_RX)else rx,if(fallback)e.getAxisValue(MotionEvent.AXIS_RY)else ry,e.getAxisValue(MotionEvent.AXIS_HAT_X),e.getAxisValue(MotionEvent.AXIS_HAT_Y),max(e.getAxisValue(MotionEvent.AXIS_LTRIGGER),e.getAxisValue(MotionEvent.AXIS_BRAKE)),max(e.getAxisValue(MotionEvent.AXIS_RTRIGGER),e.getAxisValue(MotionEvent.AXIS_GAS)))
 }
 fun update(value:Axes,now:Long,mouse:Boolean,direction:(String)->Unit,tab:(Int)->Unit){
  axes=value
  val hats=max(abs(value.hatX),abs(value.hatY))>.5f
  val x=if(hats)value.hatX else if(mouse)0f else value.leftX
  val y=if(hats)value.hatY else if(mouse)0f else value.leftY
  val d=if(max(abs(x),abs(y))<.55f)null else if(abs(x)>abs(y)){if(x>0)"right"else"left"}else if(y>0)"down"else"up"
  if(d!=heldDirection){heldDirection=d;nextRepeat=now+320;if(d!=null)direction(d)}
  if(value.leftTrigger>.5f&&!leftDown)trigger(-1,now,tab)
  if(value.rightTrigger>.5f&&!rightDown)trigger(1,now,tab)
  leftDown=value.leftTrigger>.5f;rightDown=value.rightTrigger>.5f
 }
 fun repeat(now:Long,direction:(String)->Unit){heldDirection?.let {if(now>=nextRepeat){direction(it);nextRepeat=now+150}}}
 fun trigger(delta:Int,now:Long,tab:(Int)->Unit){val last=if(delta<0)lastLeft else lastRight;if(now-last<180)return;if(delta<0)lastLeft=now else lastRight=now;tab(delta)}
 fun scroll(speed:Float,deadzone:Float=.22f)=BrowserLogic.velocity(axes.rightX,axes.rightY,deadzone,speed)
 companion object {
  fun isController(e:MotionEvent)=e.actionMasked==MotionEvent.ACTION_MOVE&&((e.source and InputDevice.SOURCE_CLASS_JOYSTICK)!=0||(e.source and InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD)
  fun direction(key:Int)=when(key){KeyEvent.KEYCODE_DPAD_UP->"up";KeyEvent.KEYCODE_DPAD_DOWN->"down";KeyEvent.KEYCODE_DPAD_LEFT->"left";KeyEvent.KEYCODE_DPAD_RIGHT->"right";else->null}
 }
}
