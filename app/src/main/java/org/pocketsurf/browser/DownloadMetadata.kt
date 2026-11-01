package org.pocketsurf.browser

import android.net.Uri
import android.webkit.URLUtil
import android.webkit.MimeTypeMap
import java.net.URLDecoder
import java.util.Locale

/** Preserve the server filename; generic response types must not rewrite its suffix. */
object DownloadMetadata {
 private val known=mapOf("apk" to "application/vnd.android.package-archive","zip" to "application/zip","7z" to "application/x-7z-compressed","pdf" to "application/pdf")
 fun name(url:String,disposition:String?,type:String?):String {
  val extended=Regex("""(?:^|;)\s*filename\*\s*=\s*(?:"([^"]*)"|([^;]*))""",RegexOption.IGNORE_CASE).find(disposition.orEmpty())?.let{it.groupValues[1].ifBlank{it.groupValues[2]}.trim()}
  val decoded=extended?.let{try{val p=it.split("'",limit=3);if(p.size==3)URLDecoder.decode(p[2].replace("+","%2B"),p[0].ifBlank{"UTF-8"})else null}catch(_:Exception){null}}
  var result=decoded?:URLUtil.guessFileName(url,disposition,null)
  val plain=Regex("""(?:^|;)\s*filename\s*=\s*(?:"([^"]*)"|([^;]*))""",RegexOption.IGNORE_CASE).find(disposition.orEmpty())?.let{it.groupValues[1].ifBlank{it.groupValues[2]}.trim()}
  val original=decoded?:plain?:try{Uri.parse(url).lastPathSegment.orEmpty()}catch(_:Exception){""}
  if(!original.substringAfterLast('/').contains('.')){
   val normalized=type.orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
   val suffix=known.entries.firstOrNull{it.value==normalized}?.key?:MimeTypeMap.getSingleton().getExtensionFromMimeType(normalized)?:"bin"
   result=if(original.isNotBlank())"$original.$suffix" else URLUtil.guessFileName(url,disposition,type)
  }
  return result.replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""),"_").take(180).ifBlank{"download.bin"}
 }
 fun mime(name:String,reported:String?):String {
  val ext=name.substringAfterLast('.',"").lowercase(Locale.ROOT)
  if(ext=="apk")return "application/vnd.android.package-archive"
  val type=reported.orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
  return if(type.isBlank()||type in listOf("application/octet-stream","binary/octet-stream","application/download","application/x-download"))known[ext]?:MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?:"application/octet-stream" else type
 }
}
