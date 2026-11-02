package org.pocketsurf.browser
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot
class BrowserLogicTest {
 @Test fun scrollConstant() { for(v in listOf(.3f,.6f,1f)) assertEquals(600f,BrowserLogic.velocity(v,0f,.2f,600f).first,0.01f) }
 @Test fun diagonalNormalized() {val p=BrowserLogic.velocity(1f,1f,.2f,600f);assertEquals(600f,hypot(p.first,p.second),.01f)}
 @Test fun deadzone() {assertEquals(0f to 0f,BrowserLogic.velocity(.1f,.1f,.2f,600f))}
 @Test fun analogPointerSpeed(){
  assertEquals(0f to 0f,BrowserLogic.pointerVelocity(.1f,.1f,.22f,600f))
  val speeds=listOf(.23f,.3f,.6f,1f).map{BrowserLogic.pointerVelocity(it,0f,.22f,600f).first}
  assertTrue(speeds.zipWithNext().all{(a,b)->a<b})
  assertTrue(speeds.first()<10f)
  assertEquals(600f,speeds.last(),.01f)
  val diagonal=BrowserLogic.pointerVelocity(1f,1f,.22f,600f)
  assertEquals(600f,hypot(diagonal.first,diagonal.second),.01f)
  assertEquals(-600f,BrowserLogic.pointerVelocity(-1f,0f,.22f,600f).first,.01f)
 }
 @Test fun addresses() {assertEquals("https://example.com",BrowserLogic.address("example.com"));assertEquals("http://example.com",BrowserLogic.address("http://example.com"));assertTrue(BrowserLogic.address("a b").endsWith("a+b"));assertTrue(BrowserLogic.address("javascript:alert(1)").startsWith("https://www.google.com"))}
 @Test fun tabEdges() {assertEquals(0,BrowserLogic.adjacent(0,-1,3));assertEquals(2,BrowserLogic.adjacent(2,1,3))}
}
