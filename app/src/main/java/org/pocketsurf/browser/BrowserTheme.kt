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
   else->if(platformStyle==android.R.style.Theme_Material_Light_NoActionBar)R.style.SurfAccentBlueLight else R.style.SurfAccentBlueDark
  }
  activity.theme.applyStyle(overlay,true)
 }
 companion object {
  val names=listOf("Light","Dark","Auto","OLED")
  val accentNames=listOf("Blue","Teal","Violet","Amber","Green","Coral","Red")
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
    "Teal"->0xff006778L to 0xff79d6dfL
    "Violet"->0xff7040b5L to 0xffc5a2ffL
    "Amber"->0xff805400L to 0xffffcf70L
    "Green"->0xff226a39L to 0xff8ada9eL
    "Coral"->0xffa53b25L to 0xffffad91L
    "Red"->0xffb02535L to 0xffff96a0L
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
