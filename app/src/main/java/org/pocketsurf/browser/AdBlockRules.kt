package org.pocketsurf.browser

import java.net.URI
import java.util.Locale

/** Conservative ABP subset. Unsupported options/rule languages are skipped, never broadened. */
class AdBlockRules private constructor(
 private val hosts:Map<String,List<NetworkRule>>,
 private val generic:List<NetworkRule>,
 private val cosmetics:List<CosmeticRule>,
 val supportedRules:Int,
 val skippedRules:Int
) {
 private data class Scope(val include:Set<String>,val exclude:Set<String>){
  fun matches(host:String)=exclude.none{domainMatches(host,it)}&&(include.isEmpty()||include.any{domainMatches(host,it)})
 }
 private data class NetworkRule(val allow:Boolean,val scope:Scope,val regex:Regex?,val literal:String?=null,val token:String="",val matchCase:Boolean=false){
  fun matches(url:String):Boolean {
   if(literal!=null)return url.contains(literal,ignoreCase=!matchCase)
   if(token.length>=3&&!url.contains(token,ignoreCase=!matchCase))return false
   return regex==null||regex.containsMatchIn(url)
  }
 }
 private data class CosmeticRule(val allow:Boolean,val scope:Scope,val selector:String)
 fun blocks(resourceUrl:String,pageUrl:String):Boolean {
  val resourceHost=host(resourceUrl)?:return false
  if(!http(resourceUrl))return false
  val pageHost=host(pageUrl)?:return false
  val matches=mutableListOf<NetworkRule>()
  var candidate=resourceHost
  while(candidate.isNotEmpty()){
   hosts[candidate]?.let{rules->for(rule in rules)if(rule.scope.matches(pageHost)&&rule.matches(resourceUrl))matches.add(rule)}
   val dot=candidate.indexOf('.');if(dot<0)break;candidate=candidate.substring(dot+1)
  }
  for(rule in generic)if(rule.scope.matches(pageHost)&&rule.matches(resourceUrl))matches.add(rule)
  return matches.any{!it.allow}&&matches.none{it.allow}
 }
 fun selectors(pageUrl:String):List<String>{
  val pageHost=host(pageUrl)?:return emptyList()
  val applicable=cosmetics.filter{it.scope.matches(pageHost)}
  val excluded=applicable.filter{it.allow}.map{it.selector}.toSet()
  return applicable.asSequence().filter{!it.allow&&it.selector !in excluded}.map{it.selector}.distinct().take(MAX_COSMETICS).toList()
 }
 companion object {
  private const val MAX_LINES=200_000
  private const val MAX_GENERIC=6_000
  private const val MAX_COSMETICS=2_000
  fun host(url:String):String?=try{URI(url).host?.lowercase(Locale.ROOT)?.trimEnd('.')?.takeIf{it.isNotBlank()}}catch(_:Exception){null}
  private fun http(url:String)=url.startsWith("https://",true)||url.startsWith("http://",true)
  private fun domainMatches(host:String,rule:String)=host==rule||host.endsWith(".$rule")
  private fun scope(value:String,separator:Char):Scope {
   val tokens=value.split(separator).map{it.trim().lowercase(Locale.ROOT)}.filter{it.isNotEmpty()}
   return Scope(tokens.filterNot{it.startsWith('~')}.toSet(),tokens.filter{it.startsWith('~')}.map{it.drop(1)}.toSet())
  }
  /** Literal tokens + '*' and '^'; no arbitrary regex execution or scriptlets. */
  private fun pattern(value:String,matchCase:Boolean):Regex? {
   if(value.length>1500||value.startsWith('/')&&value.endsWith('/'))return null
   val out=StringBuilder();var i=0
   if(value.startsWith("||")){out.append("^[a-zA-Z][a-zA-Z0-9+.-]*://(?:[^/?#]+\\.)?");i=2}
   else if(value.startsWith('|')){out.append('^');i=1}
   while(i<value.length){val c=value[i];when(c){'*'->out.append(".*");'^'->out.append("(?:[^A-Za-z0-9_.%-]|$)");'|'->if(i==value.lastIndex)out.append('$')else out.append("\\|");else->out.append(Regex.escape(c.toString()))};i++}
   return try{Regex(out.toString(),if(matchCase)emptySet()else setOf(RegexOption.IGNORE_CASE))}catch(_:Exception){null}
  }
  fun parse(text:String):AdBlockRules {
   val hostRules=linkedMapOf<String,MutableList<NetworkRule>>();val generic=mutableListOf<NetworkRule>();val cosmetics=mutableListOf<CosmeticRule>()
   var accepted=0;var skipped=0;var genericBlocks=0
   for(raw in text.lineSequence().take(MAX_LINES)){
    var line=raw.trim();if(line.isEmpty()||line.startsWith('!')||line.startsWith('['))continue
    if(line.length>2048||line.contains('\u0000')||line.contains("#?#")||line.contains("#$#")||line.contains("#%#")){skipped++;continue}
    val cosmeticException=line.indexOf("#@#");val cosmetic=line.indexOf("##")
    if(cosmeticException>=0||cosmetic>=0){
     val allow=cosmeticException>=0;val index=if(allow)cosmeticException else cosmetic
     val domains=line.substring(0,index);val selector=line.substring(index+if(allow)3 else 2).trim()
     if(selector.isEmpty()||selector.length>600||selector.contains(":has-text(")||selector.contains(":matches-")||selector.contains("+js(")||selector.contains(":style(")||selector.contains('{')||selector.contains('}')){skipped++;continue}
     cosmetics.add(CosmeticRule(allow,scope(domains,','),selector));accepted++;continue
    }
    val allow=line.startsWith("@@");if(allow)line=line.drop(2)
    val dollar=line.indexOf('$');val options=if(dollar>=0)line.substring(dollar+1).split(',')else emptyList();if(dollar>=0)line=line.substring(0,dollar)
    var filterScope=Scope(emptySet(),emptySet());var matchCase=false;var supported=true
    for(option in options)when{option.startsWith("domain=")->filterScope=scope(option.removePrefix("domain="),'|');option=="match-case"->matchCase=true;else->supported=false}
    if(!supported||line.isEmpty()||line.contains("##")){skipped++;continue}
    var domain:String?=null
    if(line.startsWith("||")){
     val possible=line.drop(2).takeWhile{it.isLetterOrDigit()||it=='.'||it=='-'}
     if(possible.contains('.')&&possible.all{it.code<128}&&line.drop(2).getOrNull(possible.length) in listOf(null,'^','/','|'))domain=possible.lowercase(Locale.ROOT)
    }
    val hostOnly=domain!=null&&(line=="||$domain^"||line=="||$domain")
    val literal=if(!hostOnly&&line.none{it in "*^|"}&&!(line.startsWith('/')&&line.endsWith('/')))line else null
    val regex=if(hostOnly||literal!=null)null else pattern(line,matchCase)
    if(!hostOnly&&literal==null&&regex==null){skipped++;continue}
    val token=if(hostOnly)""else line.split('*','^','|').maxByOrNull{it.length}.orEmpty()
    val rule=NetworkRule(allow,filterScope,regex,literal,token,matchCase)
    if(domain!=null)hostRules.getOrPut(domain){mutableListOf()}.add(rule)
    else if((allow||genericBlocks<MAX_GENERIC)&&line.length>=4){generic.add(rule);if(!allow)genericBlocks++}else{skipped++;continue}
    accepted++
   }
   return AdBlockRules(hostRules.mapValues{it.value.toList()},generic.toList(),cosmetics.toList(),accepted,skipped)
  }
 }
}
