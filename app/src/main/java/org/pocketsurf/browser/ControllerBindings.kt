package org.pocketsurf.browser

import android.content.SharedPreferences
import android.view.KeyEvent

/** Stored action IDs remain stable so existing custom physical bindings survive the Surf Bar update. */
enum class Control(val title:String,val defaultKey:Int){
 CONFIRM("Confirm / open",96),BACK("Back / cancel",97),TABS("Close tab",99),CONTEXT("Surf Bar",100),
 HISTORY_BACK("Page back",102),HISTORY_FORWARD("Page forward",103),PREVIOUS_TAB("Previous tab",104),NEXT_TAB("Next tab",105),
 POINTER("Mouse / element navigation",106),ZOOM("Reading zoom",107),SEARCH("Menu",109),MENU("Settings",108),
 UP("Move up",19),DOWN("Move down",20),LEFT("Move left",21),RIGHT("Move right",22)
}
class ControllerBindings(private val prefs:SharedPreferences){
 init {if(!prefs.getBoolean("inline_controls_v1",false)){val legacy=Control.entries.associateWith{if(it==Control.SEARCH)108 else if(it==Control.MENU)109 else it.defaultKey};val custom=Control.entries.filter{prefs.contains("key_${it.name}")&&prefs.getInt("key_${it.name}",legacy.getValue(it))!=legacy.getValue(it)};val assigned=mutableMapOf<Control,Int>();custom.forEach{assigned[it]=prefs.getInt("key_${it.name}",legacy.getValue(it))};Control.entries.filter{it !in custom}.forEach{action->val key=listOf(action.defaultKey,legacy.getValue(action)).plus((96..110)+(188..203)+(19..23)).first{it !in assigned.values};assigned[action]=key};prefs.edit().apply{assigned.forEach{(a,k)->putInt("key_${a.name}",k)};putBoolean("inline_controls_v1",true);putInt("controls_layout_version",2)}.apply()}}

 fun key(action:Control)=prefs.getInt("key_${action.name}",action.defaultKey)
 fun action(key:Int)=Control.entries.firstOrNull {this.key(it)==key}?:if(key==KeyEvent.KEYCODE_DPAD_CENTER)Control.CONFIRM else null
 fun assign(action:Control,key:Int){val old=this.key(action);val occupied=Control.entries.firstOrNull {it!=action&&this.key(it)==key};prefs.edit().apply {putInt("key_${action.name}",key);if(occupied!=null)putInt("key_${occupied.name}",old)}.apply()}
 fun reset(){prefs.edit().apply {Control.entries.forEach {remove("key_${it.name}")}}.apply()}
 fun label(action:Control)=labelForKey(key(action))
 fun labelForKey(key:Int):String=when(key){
  96->if(playStation)"cross" else "A";97->if(playStation)"circle"else "B";99->if(playStation)"square"else"X";100->if(playStation)"triangle"else"Y"
  102->"L1";103->"R1";104->"L2";105->"R2";106->"L3";107->"R3";108->"Start";109->"Select";19->"↑";20->"↓";21->"←";22->"→";else->KeyEvent.keyCodeToString(key).removePrefix("KEYCODE_BUTTON_").removePrefix("KEYCODE_")
 }
 var playStation:Boolean
  get()=prefs.getBoolean("ps_labels",false)
  set(value){prefs.edit().putBoolean("ps_labels",value).apply()}
 companion object {
  fun color(label:String)=when(label){"A","circle"->0xffff6775.toInt();"B"->0xffffd266.toInt();"X","cross"->0xff6aaaff.toInt();"Y","triangle"->0xff60d6a0.toInt();"square"->0xffeda0da.toInt();else->0xffc3cfdf.toInt()}
 }
}
