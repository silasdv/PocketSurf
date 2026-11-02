package org.pocketsurf.browser
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.*

/** One browsing policy for normal, restored, private, popup and Custom Tab views. */
object DesktopBrowsing {
 fun userAgent(original:String):String {
  val version=Regex("Chrome/[0-9.]+").find(original)?.value?:"Chrome/131.0.0.0"
  return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) $version Safari/537.36"
 }
 val viewportScript="""(()=>{if(window.top!==window||window.__pocketDesktop)return;let width=1280,deviceWidth=0;const apply=()=>{if(!document.head)return;let m=document.querySelector('meta[name="viewport"]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}const value='width='+width+(deviceWidth>0?', initial-scale='+(deviceWidth/width):'');if(m.content!==value)m.content=value;};window.__pocketDesktop={setZoom(percent,available){width=Math.round(1280*100/Math.max(50,Math.min(250,percent)));deviceWidth=Math.max(1,available);apply();},get width(){return width;}};new MutationObserver(apply).observe(document,{childList:true,subtree:true,attributes:true,attributeFilter:['content']});apply();})();"""

 fun script(zoom:Int,available:Float)=viewportScript+";window.__pocketDesktop.setZoom(${zoom.coerceIn(50,250)},${available.coerceAtLeast(1f)});"
 fun applyZoom(view:WebView,zoom:Int){val available=if(view.width>0)view.width/view.resources.displayMetrics.density else view.resources.configuration.screenWidthDp.toFloat();view.evaluateJavascript(script(zoom,available),null)}
 fun install(view:WebView,zoom:Int):ScriptHandler?=if(WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))WebViewCompat.addDocumentStartJavaScript(view,script(zoom,view.resources.configuration.screenWidthDp.toFloat()),setOf("*"))else null
 fun apply(view:WebView,zoom:Int=100):ScriptHandler?{
  val s=view.settings;s.userAgentString=userAgent(WebSettings.getDefaultUserAgent(view.context));s.useWideViewPort=true;s.loadWithOverviewMode=true
  if(WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA))WebSettingsCompat.setUserAgentMetadata(s,UserAgentMetadata.Builder().setMobile(false).setPlatform("Linux").setPlatformVersion("").setModel("").setArchitecture("x86").setBitness(64).build())
  return install(view,zoom)
 }
}
