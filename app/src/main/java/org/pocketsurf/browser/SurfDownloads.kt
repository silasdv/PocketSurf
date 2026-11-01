package org.pocketsurf.browser

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import org.json.JSONArray
import org.json.JSONObject

data class SurfDownload(val id:Long,val url:String,val name:String,val mime:String,val time:Long,val status:Int,val received:Long=0,val total:Long=0,val reason:Int=0,val localUri:String="",val referer:String="")
class SurfDownloads(private val context:Context){
 private val prefs=context.getSharedPreferences("downloads",Context.MODE_PRIVATE)
 private val manager=context.getSystemService(DownloadManager::class.java)
 private fun stored():List<SurfDownload> = try {val a=JSONArray(prefs.getString("items","[]"));(0 until a.length()).map {val o=a.getJSONObject(it);SurfDownload(o.getLong("id"),o.getString("url"),o.getString("name"),o.optString("mime"),o.getLong("time"),o.optInt("status",DownloadManager.STATUS_PENDING),o.optLong("received"),o.optLong("total"),o.optInt("reason"),o.optString("localUri"),o.optString("referer"))}}catch(_:Exception){emptyList()}
 private fun save(items:List<SurfDownload>){val a=JSONArray();items.forEach {a.put(JSONObject().put("id",it.id).put("url",it.url).put("name",it.name).put("mime",it.mime).put("time",it.time).put("status",it.status).put("received",it.received).put("total",it.total).put("reason",it.reason).put("localUri",it.localUri).put("referer",it.referer))};prefs.edit().putString("items",a.toString()).apply()}
 fun start(url:String,userAgent:String,disposition:String?=null,mime:String?=null,referer:String?=null):SurfDownload {
  require(Uri.parse(url).scheme in listOf("http","https")){"This download type is not supported"}
  val name=URLUtil.guessFileName(url,disposition,mime).replace(Regex("[\\\\/:*?\"<>|]"),"_")
  val stamp=System.currentTimeMillis();val dot=name.lastIndexOf('.');val unique=if(dot>0)name.substring(0,dot)+"-$stamp"+name.substring(dot)else "$name-$stamp"
  val request=DownloadManager.Request(Uri.parse(url)).setTitle(name).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).addRequestHeader("User-Agent",userAgent)
  if(!mime.isNullOrBlank())request.setMimeType(mime)
  if(referer!=null&&!(referer.startsWith("https://")&&url.startsWith("http://")))request.addRequestHeader("Referer",referer)
  CookieManager.getInstance().getCookie(url)?.let {request.addRequestHeader("Cookie",it)}
  if(Build.VERSION.SDK_INT>=29)request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,unique)else request.setDestinationInExternalFilesDir(context,Environment.DIRECTORY_DOWNLOADS,unique)
  val item=SurfDownload(manager.enqueue(request),url,name,mime.orEmpty(),stamp,DownloadManager.STATUS_PENDING,referer=referer.orEmpty());save(listOf(item)+stored());return item
 }
 fun items():List<SurfDownload> = stored().map {item->
  if(item.status==CANCELLED||item.localUri.isNotBlank())return@map item
  try {manager.query(DownloadManager.Query().setFilterById(item.id)).use {c->
   if(!c.moveToFirst())item.copy(status=MISSING)else item.copy(status=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),received=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),total=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),reason=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)))
  }}catch(_:Exception){item.copy(status=DownloadManager.STATUS_FAILED)}
 }
 fun cancel(id:Long):Boolean {
  // A stale UI action must never remove an already completed file.
  val state=items().firstOrNull{it.id==id}?.status?:return false
  if(state !in listOf(DownloadManager.STATUS_RUNNING,DownloadManager.STATUS_PENDING,DownloadManager.STATUS_PAUSED))return false
  if(items().firstOrNull{it.id==id}?.localUri.isNullOrBlank())manager.remove(id);save(stored().map {if(it.id==id)it.copy(status=CANCELLED)else it});return true
 }
 fun retry(item:SurfDownload,ua:String){start(item.url,ua,mime=item.mime,referer=item.referer.ifBlank{null});save(stored().filter{it.id!=item.id});manager.remove(item.id)}
 fun open(item:SurfDownload){if(item.localUri.isNotBlank()){context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(item.localUri),item.mime.ifBlank{"application/octet-stream"}).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));return};val uri=manager.getUriForDownloadedFile(item.id)?:throw IllegalStateException("Downloaded file is no longer available");val mime=manager.getMimeTypeForDownloadedFile(item.id)?:"application/octet-stream";context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}
 fun generated(url:String,name:String,mime:String,uri:Uri):SurfDownload {val stamp=System.currentTimeMillis();val item=SurfDownload(-android.os.SystemClock.elapsedRealtimeNanos().coerceAtLeast(1000L),if(url.startsWith("data:"))"data:"else url,name,mime,stamp,DownloadManager.STATUS_RUNNING,localUri=uri.toString());save(listOf(item)+stored());return item}
 fun cancelled(id:Long)=stored().any{it.id==id&&it.status==CANCELLED}
 fun generatedProgress(id:Long,bytes:Long,complete:Boolean=false,failed:Boolean=false){save(stored().map{if(it.id==id&&it.status!=CANCELLED)it.copy(received=bytes,total=if(complete)bytes else 0,status=if(failed)DownloadManager.STATUS_FAILED else if(complete)DownloadManager.STATUS_SUCCESSFUL else DownloadManager.STATUS_RUNNING)else it})}
 companion object {
  const val CANCELLED=-1;const val MISSING=-2
  fun status(item:SurfDownload)=when(item.status){DownloadManager.STATUS_PENDING->"Queued";DownloadManager.STATUS_RUNNING->if(item.total>0)"${item.received*100/item.total}% · ${bytes(item.received)} of ${bytes(item.total)}"else "Downloading · ${bytes(item.received)}";DownloadManager.STATUS_PAUSED->"Waiting for connection";DownloadManager.STATUS_SUCCESSFUL->"Completed · ${bytes(item.total)}";DownloadManager.STATUS_FAILED->"Failed · ${when(item.reason){DownloadManager.ERROR_INSUFFICIENT_SPACE->"Not enough storage";DownloadManager.ERROR_FILE_ALREADY_EXISTS->"File already exists";DownloadManager.ERROR_HTTP_DATA_ERROR->"Network error";else->"Download interrupted"}}";CANCELLED->"Cancelled";else->"File unavailable"}
  fun bytes(value:Long)=when {value<0->"Unknown size";value<1024->"$value B";value<1024*1024->String.format(java.util.Locale.ENGLISH,"%.1f KB",value/1024.0);else->String.format(java.util.Locale.ENGLISH,"%.1f MB",value/(1024.0*1024))}
 }
}
