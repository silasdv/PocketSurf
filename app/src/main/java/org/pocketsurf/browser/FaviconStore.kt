package org.pocketsurf.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlin.math.max

/** First-party favicons only; no icon proxies, analytics endpoint or certificate bypass. */
class FaviconStore(context:Context):AutoCloseable {
 private val folder=File(context.applicationContext.cacheDir,"favicons-v1").apply{mkdirs()}
 private val main=Handler(Looper.getMainLooper())
 private val worker=ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,ArrayBlockingQueue<Runnable>(64),{task->Thread(task,"PocketSurf favicons").apply{isDaemon=true}},ThreadPoolExecutor.AbortPolicy())
 private val memory=object:LruCache<String,Bitmap>(4*1024*1024){override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount}
 private val pending=mutableMapOf<String,MutableList<(Bitmap?)->Unit>>()
 private val failures=mutableMapOf<String,Long>()
 @Volatile private var closed=false
 @Volatile private var connection:HttpsURLConnection?=null

 /** Memory first, then a bounded local disk read. Use fetch() for asynchronous cold retrieval. */
 fun get(url:String):Bitmap? {
  if(closed)return null
  val key=originKey(url)?:return null
  memory.get(key)?.takeUnless{it.isRecycled}?.let{return it}
  val file=file(key)
  if(!file.exists()||file.length() !in 1..MAX_BYTES.toLong())return null
  return try {decode(file.readBytes())?.also{memory.put(key,it);file.setLastModified(System.currentTimeMillis())}}catch(_:Exception){null}
 }
 fun put(url:String,bitmap:Bitmap){
  if(closed||bitmap.isRecycled)return
  val key=originKey(url)?:return
  val value=bounded(bitmap)?:return
  memory.put(key,value)
  synchronized(failures){failures.remove(key)}
  submit {write(key,value)}
 }
 /** Callback runs on the main thread. Requests are coalesced and failures back off for 10 minutes. */
 fun fetch(url:String,onReady:(Bitmap?)->Unit){
  val key=originKey(url)
  if(closed)return
  if(key==null){deliver(onReady,null);return}
  memory.get(key)?.takeUnless{it.isRecycled}?.let{deliver(onReady,it);return}
  synchronized(pending){pending[key]?.let{it.add(onReady);return};if(pending.size>=64){deliver(onReady,null);return};pending[key]=mutableListOf(onReady)}
  val accepted=submit {
   var bitmap=get(url)
   val failedAt=synchronized(failures){failures[key]}
   if(bitmap==null&&(failedAt==null||SystemClock.elapsedRealtime()-failedAt>=BACKOFF_MS)){
    bitmap=download(url)
    if(bitmap!=null){memory.put(key,bitmap);write(key,bitmap)}else synchronized(failures){failures[key]=SystemClock.elapsedRealtime();while(failures.size>128)failures.remove(failures.keys.first())}
   }
   val callbacks=synchronized(pending){pending.remove(key).orEmpty()}
   callbacks.forEach{deliver(it,bitmap)}
  }
  if(!accepted)synchronized(pending){pending.remove(key).orEmpty()}.forEach{deliver(it,null)}
 }
 private fun deliver(callback:(Bitmap?)->Unit,bitmap:Bitmap?){main.post{if(!closed)callback(bitmap)}}
 private fun submit(task:()->Unit):Boolean {if(closed)return false;return try{worker.execute{if(!closed)task()};true}catch(_:RejectedExecutionException){false}}
 private fun file(key:String)=File(folder,MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}+".png")
 private fun java.io.InputStream.takeBounded(limit:Int):ByteArray {val out=ByteArrayOutputStream();val buffer=ByteArray(4096);while(out.size()<limit){val n=read(buffer,0,minOf(buffer.size,limit-out.size()));if(n<0)break;out.write(buffer,0,n)};return out.toByteArray()}
 private fun download(url:String):Bitmap? {
  val fallback=fetchUrl(url)?:return null
  // Read the site's own declared icon before trying the conventional favicon path.
  val declared=try {
   val page=URI(url);val target=URI("https",null,page.host,if(page.scheme=="https")page.port else -1,page.path.ifBlank{"/"},page.query,null)
   val c=target.toURL().openConnection() as HttpsURLConnection;connection=c
   c.connectTimeout=2500;c.readTimeout=2500;c.instanceFollowRedirects=false
   if(c.responseCode==200&&c.contentType.orEmpty().contains("text/html")){
    val html=c.inputStream.use{String(it.takeBounded(128*1024),Charsets.UTF_8)}
    Regex("<link\\b[^>]*>",RegexOption.IGNORE_CASE).findAll(html).mapNotNull{m->
     fun attr(name:String)=Regex(name+"\\s*=\\s*[\"']([^\"']+)[\"']",RegexOption.IGNORE_CASE).find(m.value)?.groupValues?.get(1)
     val rel=attr("rel").orEmpty();val href=attr("href")
     if(!rel.contains("icon",true)||href==null)null else runCatching{target.resolve(href)}.getOrNull()?.takeIf{it.scheme=="https"&&it.host==target.host}?.toString()
    }.toList()
   }else emptyList()
  }catch(_:Exception){emptyList<String>()}finally{connection?.disconnect();connection=null}
  for(target in (declared+fallback).distinct().take(5)){downloadImage(target)?.let{return it}}
  return null
 }
 private fun downloadImage(target:String):Bitmap? {
  return try {
   val c=URI(target).toURL().openConnection() as HttpsURLConnection;connection=c
   c.connectTimeout=3500;c.readTimeout=3500;c.instanceFollowRedirects=false;c.setRequestProperty("Accept","image/*")
   if(c.responseCode!=200||c.contentLengthLong>MAX_BYTES)return null
   val mime=c.contentType.orEmpty().substringBefore(';').lowercase()
   if(!mime.startsWith("image/")&&mime!="application/octet-stream")return null
   val bytes=c.inputStream.use {stream->val out=ByteArrayOutputStream();val buffer=ByteArray(4096);while(!closed){val n=stream.read(buffer);if(n<0)break;if(out.size()+n>MAX_BYTES)return null;out.write(buffer,0,n)};out.toByteArray()}
   if(closed)null else decode(bytes)
  }catch(_:Exception){null}finally{connection?.disconnect();connection=null}
 }
 private fun decode(bytes:ByteArray):Bitmap? {
  if(bytes.isEmpty()||bytes.size>MAX_BYTES)return null
  val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
  if(bounds.outWidth !in 1..16384||bounds.outHeight !in 1..16384)return null
  var sample=1;while(max(bounds.outWidth,bounds.outHeight)/sample>MAX_DIMENSION)sample*=2
  val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample})?:return null
  return bounded(decoded)
 }
 private fun bounded(bitmap:Bitmap):Bitmap? {
  if(bitmap.isRecycled||bitmap.width<1||bitmap.height<1)return null
  val side=max(bitmap.width,bitmap.height)
  return if(side<=MAX_DIMENSION)bitmap else Bitmap.createScaledBitmap(bitmap,max(1,bitmap.width*MAX_DIMENSION/side),max(1,bitmap.height*MAX_DIMENSION/side),true)
 }
 private fun write(key:String,bitmap:Bitmap){
  if(closed||bitmap.isRecycled)return
  var image=bitmap;var bytes:ByteArray
  while(true){val out=ByteArrayOutputStream();image.compress(Bitmap.CompressFormat.PNG,100,out);bytes=out.toByteArray();if(bytes.size<=MAX_BYTES)break;if(max(image.width,image.height)<=16)return;image=Bitmap.createScaledBitmap(image,max(1,image.width/2),max(1,image.height/2),true)}
  var temporary:File?=null
  try {folder.mkdirs();temporary=File.createTempFile("icon-",".tmp",folder);temporary.writeBytes(bytes)
   try{Files.move(temporary.toPath(),file(key).toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)}catch(_:java.nio.file.AtomicMoveNotSupportedException){Files.move(temporary.toPath(),file(key).toPath(),StandardCopyOption.REPLACE_EXISTING)}
   prune()
  }catch(_:Exception){}finally{temporary?.takeIf{it.exists()}?.delete()}
 }
 private fun prune(){
  val files=folder.listFiles{f->f.isFile&&f.name.endsWith(".png")}.orEmpty().sortedByDescending{it.lastModified()}
  var total=0L;files.forEachIndexed{i,f->total+=f.length();if(i>=MAX_FILES||total>MAX_DISK_BYTES)f.delete()}
 }
 override fun close(){closed=true;connection?.disconnect();worker.shutdownNow();synchronized(pending){pending.clear()};memory.evictAll()}
 companion object {
  const val MAX_BYTES=128*1024
  const val MAX_DIMENSION=256
  private const val MAX_FILES=32
  private const val MAX_DISK_BYTES=4L*1024*1024
  private const val BACKOFF_MS=10L*60*1000
  fun originKey(url:String):String?=StartPageStore.origin(url)
  /** Never follows a favicon redirect to a third-party host; HTTP origins use the same host over HTTPS. */
  fun fetchUrl(url:String):String? {val key=originKey(url)?:return null;return try {val u=URI(key);URI("https",null,u.host,if(u.scheme=="https")u.port else -1,"/favicon.ico",null,null).toASCIIString()}catch(_:Exception){null}}
 }
}
