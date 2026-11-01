package org.pocketsurf.browser

import android.content.SharedPreferences

/** Process-memory settings for isolated private sessions. */
class MemoryPreferences(initial:Map<String,*> = emptyMap<String,Any>()):SharedPreferences {
 private val values=initial.toMutableMap();private val listeners=mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
 override fun getAll():MutableMap<String,*> = synchronized(values){values.toMutableMap()}
 override fun contains(key:String?)=synchronized(values){values.containsKey(key)}
 override fun getString(key:String?,defValue:String?)=synchronized(values){values[key] as? String?:defValue}
 override fun getStringSet(key:String?,defValues:MutableSet<String>?):MutableSet<String>?=synchronized(values){(values[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet()?:defValues?.toMutableSet()}
 override fun getInt(key:String?,defValue:Int)=synchronized(values){values[key] as? Int?:defValue}
 override fun getLong(key:String?,defValue:Long)=synchronized(values){values[key] as? Long?:defValue}
 override fun getFloat(key:String?,defValue:Float)=synchronized(values){values[key] as? Float?:defValue}
 override fun getBoolean(key:String?,defValue:Boolean)=synchronized(values){values[key] as? Boolean?:defValue}
 override fun registerOnSharedPreferenceChangeListener(listener:SharedPreferences.OnSharedPreferenceChangeListener?){if(listener!=null)listeners.add(listener)}
 override fun unregisterOnSharedPreferenceChangeListener(listener:SharedPreferences.OnSharedPreferenceChangeListener?){listeners.remove(listener)}
 override fun edit():SharedPreferences.Editor=object:SharedPreferences.Editor {
  val changes=mutableMapOf<String,Any?>();var erase=false
  fun put(k:String?,v:Any?):SharedPreferences.Editor {if(k!=null)changes[k]=v;return this}
  override fun putString(k:String?,v:String?)=put(k,v)
  override fun putStringSet(k:String?,v:MutableSet<String>?)=put(k,v?.toSet())
  override fun putInt(k:String?,v:Int)=put(k,v)
  override fun putLong(k:String?,v:Long)=put(k,v)
  override fun putFloat(k:String?,v:Float)=put(k,v)
  override fun putBoolean(k:String?,v:Boolean)=put(k,v)
  override fun remove(k:String?)=put(k,null)
  override fun clear():SharedPreferences.Editor {erase=true;return this}
  override fun commit():Boolean {synchronized(values){if(erase)values.clear();for((k,v) in changes){if(v==null)values.remove(k)else values[k]=v}};for(k in changes.keys)listeners.toList().forEach{it.onSharedPreferenceChanged(this@MemoryPreferences,k)};return true}
  override fun apply(){commit()}
 }
}
