package org.pocketsurf.browser

import android.webkit.ValueCallback
import android.webkit.WebView
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowWebView

/** Records native host/JS commands; it does not execute Chromium or fabricate DOM results. */
@Implements(WebView::class)
class RecordingWebViewShadow:ShadowWebView(){
 @RealObject private lateinit var view:WebView
 @Implementation override fun evaluateJavascript(script:String,callback:ValueCallback<String>?){scripts.add(script);super.evaluateJavascript(script,callback)}
 @Implementation override fun scrollBy(x:Int,y:Int){nativeScroll.add(x to y);view.scrollTo(view.scrollX+x,view.scrollY+y)}
 @Implementation fun findAllAsync(query:String){findQueries.add(query)}
 @Implementation fun findNext(forward:Boolean){findDirections.add(forward)}
 @Implementation fun clearMatches(){clearFindCalls++}
 @Implementation fun setFindListener(listener:WebView.FindListener?){findListener=listener}
 companion object {
  val scripts=mutableListOf<String>()
  val nativeScroll=mutableListOf<Pair<Int,Int>>()
  val findQueries=mutableListOf<String>()
  val findDirections=mutableListOf<Boolean>()
  var clearFindCalls=0
  var findListener:WebView.FindListener?=null
  fun clearEvidence(){scripts.clear();nativeScroll.clear();findQueries.clear();findDirections.clear();clearFindCalls=0;findListener=null}
 }
}

