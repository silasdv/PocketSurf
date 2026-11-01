package org.pocketsurf.browser

import org.junit.Assert.*
import org.junit.Test

class BrowserThemeTest {
 @Test fun baseAndAccentOptionsAreIndependent(){assertEquals(listOf("Light","Dark","Auto","OLED"),BrowserTheme.names);assertTrue(BrowserTheme.accentNames.containsAll(listOf("Red","Teal","Violet","Amber","Green","Coral")))}
 @Test fun oledAccentChangeKeepsAllNeutralSurfacesAndText(){val blue=BrowserTheme.resolve("OLED",false,"Blue");val red=BrowserTheme.resolve("OLED",false,"Red");assertEquals(0xff000000.toInt(),red.background);assertNotEquals(blue.accent,red.accent);assertEquals(red.accent,red.focus);assertEquals(blue.copy(accent=red.accent,focus=red.focus),red)}
 @Test fun automaticThemeUsesSystemBaseAndIndependentAccent(){assertEquals(BrowserTheme.resolve("Light",false,"Green"),BrowserTheme.resolve("Auto",false,"Green"));assertEquals(BrowserTheme.resolve("Dark",true,"Green"),BrowserTheme.resolve("Auto",true,"Green"));assertNotEquals(BrowserTheme.resolve("Auto",true,"Green").accent,BrowserTheme.resolve("Auto",false,"Green").accent)}
 @Test fun existingCombinedThemesMigrateToExpectedBaseAndAccent(){val mapping=mapOf("Nord" to ("Dark" to "Teal"),"Dracula" to ("Dark" to "Violet"),"Gruvbox" to ("Dark" to "Amber"),"Forest" to ("Dark" to "Green"),"Sunset" to ("Dark" to "Coral"),"Solarized Light" to ("Light" to "Teal"));mapping.forEach{(legacy,target)->assertEquals(target.first,BrowserTheme.baseName(legacy));assertEquals(target.second,BrowserTheme.defaultAccentName(legacy));assertEquals(BrowserTheme.resolve(target.first,false,target.second),BrowserTheme.resolve(legacy,false))};assertEquals(BrowserTheme.resolve("Dark",true,"Red"),BrowserTheme.resolve("Nord",true,"Red"))}
 @Test fun accentHasReadableContrastOnBothSurfaces(){for(dark in listOf(false,true))for(accent in BrowserTheme.accentNames){val theme=BrowserTheme.resolve(if(dark)"Dark" else "Light",dark,accent);assertTrue("$accent dark=$dark",contrast(theme.accent,theme.surface)>=4.5);assertEquals(theme.accent,theme.focus)}}
 private fun contrast(a:Int,b:Int):Double{val first=luminance(a);val second=luminance(b);return (maxOf(first,second)+.05)/(minOf(first,second)+.05)}
 private fun luminance(color:Int):Double{fun channel(shift:Int):Double{val n=((color shr shift) and 255)/255.0;return if(n<=.04045)n/12.92 else Math.pow((n+.055)/1.055,2.4)};return .2126*channel(16)+.7152*channel(8)+.0722*channel(0)}
}
