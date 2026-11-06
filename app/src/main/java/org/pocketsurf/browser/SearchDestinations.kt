package org.pocketsurf.browser

import java.net.URI
import java.util.Locale
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Local visit statistics; typing never participates in destination ranking. */
object SearchDestinations {
 fun identity(url:String):String=try {
  val u=URI(url.trim()).normalize()
  val scheme=u.scheme.orEmpty().lowercase(Locale.ROOT)
  val port=if(u.port==443&&scheme=="https"||u.port==80&&scheme=="http")-1 else u.port
  URI(scheme,null,u.host.lowercase(Locale.ROOT).removePrefix("www."),port,null,null,null).toASCIIString()+u.rawPath.orEmpty().ifEmpty{"/"}+(u.rawQuery?.let{"?"+it}.orEmpty())
 }catch(_:Exception){url.trim()}
 fun websiteKey(url:String):String=try{val u=URI(url);u.host.lowercase(Locale.ROOT).removePrefix("www.")+if(u.port !in listOf(-1,80,443))":${u.port}" else ""}catch(_:Exception){identity(url)}
 /** PSL private suffixes keep independent hosted services distinct. */
 fun websiteIdentity(url:String):String {
  val parsed=url.toHttpUrlOrNull()?:return websiteKey(url)
  return parsed.topPrivateDomain()?:parsed.host
 }
 fun rank(history:List<Visit>):List<Visit> {
  val sites=history.filter{StartPageStore.origin(it.url)!=null}.groupBy{websiteIdentity(it.url)}
  return sites.entries.sortedWith(compareByDescending<Map.Entry<String,List<Visit>>>{it.value.size}
   .thenByDescending{it.value.maxOf{v->v.time}}.thenBy{it.key}).map{(_,visits)->
    visits.groupBy{identity(it.url)}.entries.sortedWith(compareByDescending<Map.Entry<String,List<Visit>>>{it.value.size}
     .thenByDescending{it.value.maxOf{v->v.time}}.thenBy{it.key}).first().value.maxBy{it.time}
   }
 }
 /** Bookmark candidates represent websites; saved pages from that website exclude the candidate. */
 fun bookmarkSuggestions(saved:List<Visit>,history:List<Visit>):List<Shortcut> {
  val savedSites=saved.map{websiteIdentity(it.url)}.toSet()
  val visits=history.filter{StartPageStore.origin(it.url)!=null}.groupBy{websiteIdentity(it.url)}
  val candidates=linkedMapOf<String,Shortcut>()
  StartPageStore.popular.forEach{candidates[websiteIdentity(it.url)]=it}
  visits.forEach{(key,items)->
   val representative=rank(items).first()
   val curated=candidates[key]
   candidates[key]=Shortcut(curated?.id?:"visited:"+key,curated?.title?:SiteNames.fallback(representative.url),representative.url)
  }
  val order=candidates.keys.withIndex().associate{it.value to it.index}
  return candidates.filterKeys{it !in savedSites}.entries.sortedWith(
   compareByDescending<Map.Entry<String,Shortcut>>{visits[it.key]?.size?:0}
    .thenByDescending{visits[it.key]?.maxOfOrNull{v->v.time}?:0}
    .thenBy{order[it.key]?:Int.MAX_VALUE}).take(8).map{it.value}
 }
 fun capacity(available:Int,item:Int,spacing:Int=0)=if(item<=0)0 else ((available.coerceAtLeast(0)+spacing)/(item+spacing)).coerceAtLeast(0)
}
