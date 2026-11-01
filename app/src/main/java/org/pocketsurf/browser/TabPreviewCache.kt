package org.pocketsurf.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors

/** Normal-mode thumbnails only; private callers keep previews in memory. */
class TabPreviewCache(context:Context):AutoCloseable {
 private val directory=File(context.cacheDir,"tab-previews").apply{mkdirs()}
 private val writer=Executors.newSingleThreadExecutor{r->Thread(r,"PocketSurf previews").apply{isDaemon=true}}
 private fun file(id:String)=File(directory,MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).joinToString(""){"%02x".format(it)}+".webp")
 fun load(id:String):Bitmap?=runCatching{val f=file(id);BitmapFactory.decodeFile(f.path,BitmapFactory.Options().apply{inPreferredConfig=Bitmap.Config.RGB_565})?.also{f.setLastModified(System.currentTimeMillis())}}.getOrNull()
 fun save(id:String,bitmap:Bitmap):java.util.concurrent.Future<*>?{if(writer.isShutdown)return null;return writer.submit{runCatching{val target=file(id);val temp=File(directory,target.name+"."+java.util.UUID.randomUUID()+".tmp");temp.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.WEBP,70,it))};if(!temp.renameTo(target))temp.delete();directory.listFiles()?.filter{it.extension=="webp"}?.sortedByDescending{it.lastModified()}?.drop(128)?.forEach{it.delete()}}}}
 override fun close(){writer.shutdown()}
}
