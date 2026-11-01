package org.pocketsurf.browser

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class BrowserDataTest {
 private fun store()=BrowserData(RuntimeEnvironment.getApplication().getSharedPreferences("browser",0))
 @Test fun historySearchIncludesTitlesAndUrlsAndSortsByDate(){val d=store();d.record("https://example.com/new","Newest",time=200000);d.record("https://older.test","Old title",time=100000);assertEquals("Newest",d.search("history","").first().title);assertEquals("Old title",d.search("history","OLDER").single().title);assertEquals("Newest",d.search("history","newest").single().title)}
 @Test fun returnVisitsOnAnotherDayAreNotErased(){val d=store();d.record("https://example.com","Example",time=100000);d.record("https://example.com","Example",time=86400000);assertEquals(2,d.entries("history").size)}
 @Test fun dayGroupsUseCalendarDatesAcrossDaylightSaving(){val zone=ZoneId.of("Europe/Berlin");val now=ZonedDateTime.of(2026,10,26,0,30,0,0,zone).toInstant().toEpochMilli();val previous=ZonedDateTime.of(2026,10,25,0,15,0,0,zone).toInstant().toEpochMilli();assertEquals("Yesterday",BrowserData.day(previous,now,zone));assertEquals("Today",BrowserData.day(now,now,zone));assertEquals("Earlier",BrowserData.day(0,now,zone))}
 @Test fun remapCollisionsSwapAndReset(){val b=ControllerBindings(RuntimeEnvironment.getApplication().getSharedPreferences("browser",0));b.assign(Control.CONFIRM,100);assertEquals(100,b.key(Control.CONFIRM));assertEquals(96,b.key(Control.CONTEXT));assertEquals(Control.entries.size,Control.entries.map{b.key(it)}.toSet().size);b.reset();assertEquals(96,b.key(Control.CONFIRM));assertEquals(100,b.key(Control.CONTEXT))}
 @Test fun faceLabelsHaveRequestedColoursAndPersist(){val p=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0);val b=ControllerBindings(p);assertEquals(0xffff6775.toInt(),ControllerBindings.color("A"));assertEquals(0xffffd266.toInt(),ControllerBindings.color("B"));assertEquals(0xff6aaaff.toInt(),ControllerBindings.color("X"));assertEquals(0xff60d6a0.toInt(),ControllerBindings.color("Y"));b.playStation=true;assertEquals("cross",ControllerBindings(p).label(Control.CONFIRM));assertEquals(ControllerBindings.color("X"),ControllerBindings.color("cross"));assertEquals(0xffeda0da.toInt(),ControllerBindings.color("square"))}
}
