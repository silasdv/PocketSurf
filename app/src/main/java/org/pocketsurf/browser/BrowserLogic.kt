package org.pocketsurf.browser
import java.net.URI
import java.net.URLEncoder
import kotlin.math.*
object BrowserLogic {
 fun address(text:String,search:String="https://www.google.com/search?q="):String {
  val s=text.trim()
  if(s.startsWith("https://",true)||s.startsWith("http://",true)) return s
  if(!s.contains(' ') && (s.contains('.')||s.startsWith("localhost"))) {
   try { val u=URI("https://$s"); if(!u.host.isNullOrBlank()) return u.toString() } catch(_:Exception) {}
  }
  return search+URLEncoder.encode(s,"UTF-8")
 }
 fun velocity(x:Float,y:Float,deadzone:Float,speed:Float):Pair<Float,Float> {
  val m=sqrt(x*x+y*y);return if(m<=deadzone) 0f to 0f else x/m*speed to y/m*speed
 }
 /** Radial deadzone with continuous analog speed; full deflection retains the speed ceiling. */
 fun pointerVelocity(x:Float,y:Float,deadzone:Float,speed:Float):Pair<Float,Float> {
  val magnitude=hypot(x,y)
  if(magnitude<=deadzone)return 0f to 0f
  val intensity=((magnitude.coerceAtMost(1f)-deadzone)/(1f-deadzone)).coerceIn(0f,1f)
  return x/magnitude*speed*intensity to y/magnitude*speed*intensity
 }
 fun adjacent(index:Int,delta:Int,size:Int)=if(size<=0) 0 else (index+delta).coerceIn(0,size-1)
}
