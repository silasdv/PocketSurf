package org.pocketsurf.browser

import android.net.Uri
import java.net.IDN
import java.util.Locale

object PageIdentity {
 fun host(url:String):String=try{val u=Uri.parse(url);val host=(u.host?:throw IllegalArgumentException()).removeSurrounding("[","]");val ascii=if(host.contains(':'))"[$host]"else IDN.toASCII(host.trimEnd('.'),IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT);ascii+if(u.port>0&&!((u.scheme.equals("https",true)&&u.port==443)||(u.scheme.equals("http",true)&&u.port==80)))":${u.port}"else ""}catch(_:Exception){"Unknown website"}
 fun origin(url:String):String?=try{val u=Uri.parse(url);if(u.scheme?.lowercase(Locale.ROOT) !in listOf("http","https")||u.host.isNullOrBlank())null else host(url).takeUnless{it=="Unknown website"}?.let{"${u.scheme?.lowercase(Locale.ROOT)}://$it"}}catch(_:Exception){null}
 fun referer(source:String,target:String):String? {val from=origin(source)?:return null;val to=origin(target)?:return null;if(from.startsWith("https:")&&to.startsWith("http:"))return null;if(from!=to)return "$from/";val u=Uri.parse(source);return Uri.Builder().scheme(u.scheme?.lowercase(Locale.ROOT)).encodedAuthority(host(source)).encodedPath(u.encodedPath?.ifEmpty{"/"}?:"/").encodedQuery(u.encodedQuery).build().toString()}
 fun label(url:String):String=(if(Uri.parse(url).scheme.equals("http",true))"Not secure · "else "")+host(url)
}
