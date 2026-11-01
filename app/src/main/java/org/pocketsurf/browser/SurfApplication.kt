package org.pocketsurf.browser

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import android.webkit.WebView
import java.io.File

/** Private WebView storage lives in a separate process/data-directory; browser settings are memory-only. */
class SurfApplication:Application(){
 var privateProcess=false;private set
 var privateReady=false;private set
 private val memory=mutableMapOf<String,SharedPreferences>()
 override fun onCreate(){super.onCreate();privateProcess=Build.VERSION.SDK_INT>=28&&getProcessName().endsWith(":private")
  if(privateProcess){
   File(super.getCacheDir(),"private-process-id").writeText(android.os.Process.myPid().toString())
   val base=File(applicationInfo.dataDir).canonicalFile;val old=File(base,"app_webview_pocketsurf_private").canonicalFile
   if(old.parentFile==base)old.deleteRecursively()
   val temp=File(super.getCacheDir(),"private-session").canonicalFile;if(temp.parentFile==super.getCacheDir().canonicalFile)temp.deleteRecursively()
   try{if(Build.VERSION.SDK_INT<28)return;WebView.setDataDirectorySuffix("pocketsurf_private");privateReady=true}catch(_:RuntimeException){privateReady=false}
  }else cleanupAbandonedPrivate()
 }
 fun cleanupAbandonedPrivate(){if(privateProcess)return;val marker=File(super.getCacheDir(),"private-process-id");val pid=runCatching{marker.readText().trim().toInt()}.getOrNull()?:return;val processes=(getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).runningAppProcesses?:return;if(processes.any{it.pid==pid&&it.processName==packageName+":private"})return;val base=File(applicationInfo.dataDir).canonicalFile;val profile=File(base,"app_webview_pocketsurf_private").canonicalFile;if(profile.parentFile==base)profile.deleteRecursively();val temp=File(super.getCacheDir(),"private-session").canonicalFile;if(temp.parentFile==super.getCacheDir().canonicalFile)temp.deleteRecursively();marker.delete()}
 fun normalPreferences(name:String):SharedPreferences=super.getSharedPreferences(name,MODE_PRIVATE)
 override fun getSharedPreferences(name:String,mode:Int):SharedPreferences {
  if(!privateProcess)return super.getSharedPreferences(name,mode)
  return memory.getOrPut(name){val initial=if(name=="browser")super.getSharedPreferences(name,mode).all.filterKeys{BackupCodec.allowed(it)}.toMutableMap().apply{put("setup_complete",true)}else emptyMap();MemoryPreferences(initial)}
 }
 override fun getFilesDir():File=if(privateProcess)File(super.getCacheDir(),"private-session/files").apply{mkdirs()}else super.getFilesDir()
 override fun getCacheDir():File=if(privateProcess)File(super.getCacheDir(),"private-session/cache").apply{mkdirs()}else super.getCacheDir()
}
class PrivateBrowserActivity:BrowserActivity(){override val isPrivateSession=true}
