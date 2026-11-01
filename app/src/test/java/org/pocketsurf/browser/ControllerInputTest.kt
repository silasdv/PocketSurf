package org.pocketsurf.browser
import org.junit.Test
import org.junit.Assert.*
class ControllerInputTest {
 @Test fun leftStickDoesNotScroll(){val c=ControllerInput();val directions=mutableListOf<String>();c.update(ControllerInput.Axes(leftY=1f),0,false,{directions.add(it)},{ });assertEquals(listOf("down"),directions);assertEquals(0f to 0f,c.scroll(600f))}
 @Test fun rightStickDoesNotNavigate(){val c=ControllerInput();c.update(ControllerInput.Axes(rightY=1f),0,false,{fail("Right stick navigated")},{});assertEquals(0f to 600f,c.scroll(600f))}
 @Test fun hatsNavigateInPointerMode(){val c=ControllerInput();val out=mutableListOf<String>();c.update(ControllerInput.Axes(hatX=-1f),0,true,{out.add(it)},{});assertEquals(listOf("left"),out)}
 @Test fun controlledHoldRepeatAndRelease(){val c=ControllerInput();val out=mutableListOf<String>();c.update(ControllerInput.Axes(leftY=1f),0,false,{out.add(it)},{});c.repeat(100){out.add(it)};assertEquals(1,out.size);c.repeat(320){out.add(it)};assertEquals(2,out.size);c.update(ControllerInput.Axes(),400,false,{out.add(it)},{});c.repeat(1000){out.add(it)};assertEquals(2,out.size)}
 @Test fun analogAndDigitalTriggersDeduplicate(){val c=ControllerInput();val out=mutableListOf<Int>();c.update(ControllerInput.Axes(rightTrigger=1f),1000,false,{}, {out.add(it)});c.trigger(1,1020){out.add(it)};assertEquals(listOf(1),out);c.update(ControllerInput.Axes(),1300,false,{},{});c.update(ControllerInput.Axes(rightTrigger=1f),1500,false,{}, {out.add(it)});assertEquals(listOf(1,1),out)}
 @Test fun scrollConstantAtEveryMagnitudeAndDiagonal(){for(v in listOf(.3f,.6f,1f)){val c=ControllerInput();c.update(ControllerInput.Axes(rightY=v),0,false,{},{});assertEquals(600f,c.scroll(600f).second,.01f)};val c=ControllerInput();c.update(ControllerInput.Axes(rightX=1f,rightY=1f),0,false,{},{});val p=c.scroll(600f);assertEquals(600f,kotlin.math.hypot(p.first,p.second),.01f)}
}
