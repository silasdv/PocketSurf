package org.pocketsurf.browser

data class BrowserTheme(val background:Int,val surface:Int,val text:Int,val secondary:Int,val accent:Int,
 val focus:Int=accent,val selected:Int=surface,val pressed:Int=surface,val disabled:Int=secondary,
 val divider:Int=secondary,val error:Int=0xffff8a80.toInt(),val warning:Int=0xffffd180.toInt(),val success:Int=0xff9cdda5.toInt()) {
 /** Match framework widgets and their inherited tint to the same neutral base as custom chrome. */
 val platformStyle:Int get()=if(background==0xfff4f7fb.toInt())android.R.style.Theme_Material_Light_NoActionBar else android.R.style.Theme_Material_NoActionBar
 /** Include selection handles, switches and platform dialogs in the chosen accent. */
 fun applyPlatform(activity:android.app.Activity){
  activity.setTheme(platformStyle)
  val overlay=when(accent){
   0xff0759b3.toInt()->R.style.SurfAccentBlueLight
   0xff69baff.toInt()->R.style.SurfAccentBlueDark
   0xff006778.toInt()->R.style.SurfAccentTealLight
   0xff79d6df.toInt()->R.style.SurfAccentTealDark
   0xff7040b5.toInt()->R.style.SurfAccentVioletLight
   0xffc5a2ff.toInt()->R.style.SurfAccentVioletDark
   0xff805400.toInt()->R.style.SurfAccentAmberLight
   0xffffcf70.toInt()->R.style.SurfAccentAmberDark
   0xff226a39.toInt()->R.style.SurfAccentGreenLight
   0xff8ada9e.toInt()->R.style.SurfAccentGreenDark
   0xffa53b25.toInt()->R.style.SurfAccentCoralLight
   0xffffad91.toInt()->R.style.SurfAccentCoralDark
   0xffb02535.toInt()->R.style.SurfAccentRedLight
   0xffff96a0.toInt()->R.style.SurfAccentRedDark
   0xffa41427.toInt()->R.style.SurfAccentCrimsonLight
   0xffff5360.toInt()->R.style.SurfAccentCrimsonDark
   0xff1845b7.toInt()->R.style.SurfAccentCobaltLight
   0xff638aff.toInt()->R.style.SurfAccentCobaltDark
   0xff006c42.toInt()->R.style.SurfAccentEmeraldLight
   0xff24ca78.toInt()->R.style.SurfAccentEmeraldDark
   0xff974300.toInt()->R.style.SurfAccentOrangeLight
   0xffff903d.toInt()->R.style.SurfAccentOrangeDark
   0xffa31472.toInt()->R.style.SurfAccentMagentaLight
   0xfff058bc.toInt()->R.style.SurfAccentMagentaDark
   0xff006575.toInt()->R.style.SurfAccentCyanLight
   0xff31d2ef.toInt()->R.style.SurfAccentCyanDark
   0xff6420a8.toInt()->R.style.SurfAccentPurpleLight
   0xffb881ff.toInt()->R.style.SurfAccentPurpleDark
   0xffba3162.toInt()->R.style.SurfAccentRoseLight
   0xffef6697.toInt()->R.style.SurfAccentRoseDark
   0xffc81e3c.toInt()->R.style.SurfAccentRubyLight
   0xffff5573.toInt()->R.style.SurfAccentRubyDark
   0xff487200.toInt()->R.style.SurfAccentLimeLight
   0xffa4ce3a.toInt()->R.style.SurfAccentLimeDark
   0xff606d13.toInt()->R.style.SurfAccentOliveLight
   0xffa7b45a.toInt()->R.style.SurfAccentOliveDark
   0xff007649.toInt()->R.style.SurfAccentMintLight
   0xff58d6a9.toInt()->R.style.SurfAccentMintDark
   0xff007462.toInt()->R.style.SurfAccentAquaLight
   0xff42cdbb.toInt()->R.style.SurfAccentAquaDark
   0xff0f69b5.toInt()->R.style.SurfAccentAzureLight
   0xff429ce8.toInt()->R.style.SurfAccentAzureDark
   0xff6059c1.toInt()->R.style.SurfAccentIndigoLight
   0xff8d86ee.toInt()->R.style.SurfAccentIndigoDark
   0xff7956a6.toInt()->R.style.SurfAccentLavenderLight
   0xffba97e7.toInt()->R.style.SurfAccentLavenderDark
   0xff99459e.toInt()->R.style.SurfAccentPlumLight
   0xffc975ce.toInt()->R.style.SurfAccentPlumDark
   0xff995621.toInt()->R.style.SurfAccentPeachLight
   0xffedaa75.toInt()->R.style.SurfAccentPeachDark
   0xff836200.toInt()->R.style.SurfAccentGoldLight
   0xffddbc4a.toInt()->R.style.SurfAccentGoldDark
   else->if(platformStyle==android.R.style.Theme_Material_Light_NoActionBar)R.style.SurfAccentBlueLight else R.style.SurfAccentBlueDark
  }
  activity.theme.applyStyle(overlay,true)
 }
 companion object {
  val names=listOf("Light","Dark","Auto","OLED")
  val accentNames=listOf("Rose","Mint","Lavender","Blue","Teal","Violet","Amber","Green","Coral","Red","Peach","Gold","Olive","Ruby","Lime","Aqua","Crimson","Cobalt","Emerald","Orange","Magenta","Cyan","Purple","Azure","Indigo","Plum")
  /** Old combined themes become a base plus an accent without losing the user's choice. */
  fun baseName(name:String):String=when(name){
   "Nord","Dracula","Gruvbox","Forest","Sunset"->"Dark"
   "Solarized Light"->"Light"
   else->names.firstOrNull{it.equals(name,true)}?:"Dark"
  }
  fun defaultAccentName(name:String):String=when(name){
   "Nord","Solarized Light"->"Teal"
   "Dracula"->"Violet"
   "Gruvbox"->"Amber"
   "Forest"->"Green"
   "Sunset"->"Coral"
   else->"Blue"
  }
  /** Accent colors maintain readable contrast against their respective neutral base. */
  fun accentColor(accentName:String,dark:Boolean):Int {
   val pair=when(accentNames.firstOrNull{it.equals(accentName,true)}?:"Blue"){
    "Rose"->0xffba3162L to 0xffef6697L
    "Ruby"->0xffc81e3cL to 0xffff5573L
    "Lime"->0xff487200L to 0xffa4ce3aL
    "Olive"->0xff606d13L to 0xffa7b45aL
    "Mint"->0xff007649L to 0xff58d6a9L
    "Aqua"->0xff007462L to 0xff42cdbbL
    "Azure"->0xff0f69b5L to 0xff429ce8L
    "Indigo"->0xff6059c1L to 0xff8d86eeL
    "Lavender"->0xff7956a6L to 0xffba97e7L
    "Plum"->0xff99459eL to 0xffc975ceL
    "Peach"->0xff995621L to 0xffedaa75L
    "Gold"->0xff836200L to 0xffddbc4aL
    "Teal"->0xff006778L to 0xff79d6dfL
    "Violet"->0xff7040b5L to 0xffc5a2ffL
    "Amber"->0xff805400L to 0xffffcf70L
    "Green"->0xff226a39L to 0xff8ada9eL
    "Coral"->0xffa53b25L to 0xffffad91L
    "Red"->0xffb02535L to 0xffff96a0L
    "Crimson"->0xffa41427L to 0xffff5360L
    "Cobalt"->0xff1845b7L to 0xff638affL
    "Emerald"->0xff006c42L to 0xff24ca78L
    "Orange"->0xff974300L to 0xffff903dL
    "Magenta"->0xffa31472L to 0xfff058bcL
    "Cyan"->0xff006575L to 0xff31d2efL
    "Purple"->0xff6420a8L to 0xffb881ffL
    else->0xff0759b3L to 0xff69baffL
   }
   return (if(dark)pair.second else pair.first).toInt()
  }
  fun resolve(name:String,dark:Boolean,accentName:String?=null):BrowserTheme {
   val base=baseName(name)
   val light=base=="Light"||(base=="Auto"&&!dark)
   val oled=base=="OLED"
   val background=if(light)0xfff4f7fb.toInt() else if(oled)0xff000000.toInt() else 0xff0f141d.toInt()
   val surface=if(light)0xffe2eaf4.toInt() else if(oled)0xff121820.toInt() else 0xff1e2a3a.toInt()
   val text=if(light)0xff121c2b.toInt() else 0xfff7faff.toInt()
   val secondary=if(light)0xff40536b.toInt() else 0xffadbed2.toInt()
   val accent=accentColor(accentName?:defaultAccentName(name),!light)
   return BrowserTheme(background,surface,text,secondary,accent,
    error=if(light)0xffa71919.toInt() else 0xffff8a80.toInt(),
    warning=if(light)0xff765000.toInt() else 0xffffd180.toInt(),
    success=if(light)0xff186733.toInt() else 0xff9cdda5.toInt())
  }
 }
}
