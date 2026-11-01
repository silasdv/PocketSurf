package org.pocketsurf.browser
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.pow
class ThemeTest {
 private fun luminance(c:Int):Double {val v=listOf((c shr 16)and 255,(c shr 8)and 255,c and 255).map {val x=it/255.0;if(x<=.04045)x/12.92 else ((x+.055)/1.055).pow(2.4)};return v[0]*.2126+v[1]*.7152+v[2]*.0722}
 private fun ratio(a:Int,b:Int):Double {val x=luminance(a);val y=luminance(b);return (maxOf(x,y)+.05)/(minOf(x,y)+.05)}
 @Test fun allThemesReadable(){for(name in BrowserTheme.names){val t=BrowserTheme.resolve(name,true);for(bg in listOf(t.background,t.surface)){assertTrue("$name text",ratio(t.text,bg)>=4.5);assertTrue("$name secondary",ratio(t.secondary,bg)>=4.5);assertTrue("$name focus",ratio(t.focus,bg)>=3.0)}}}
 @Test fun autoFollowsSystem(){assertEquals(BrowserTheme.resolve("Light",false),BrowserTheme.resolve("Auto",false));assertEquals(BrowserTheme.resolve("Dark",true),BrowserTheme.resolve("Auto",true))}
}
