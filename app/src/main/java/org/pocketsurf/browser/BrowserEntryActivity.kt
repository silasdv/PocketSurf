package org.pocketsurf.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent

/** One exported HTTP entry; no duplicate browser choices or forwarding to other packages. */
class BrowserEntryActivity:Activity(){
 override fun onCreate(state:Bundle?){super.onCreate(state)
  val source=intent;val uri=source?.data
  if(source?.action==Intent.ACTION_VIEW&&uri?.scheme in listOf("http","https")){
   val custom=runCatching{source.hasExtra(CustomTabsIntent.EXTRA_SESSION)}.getOrDefault(false)
   val target=if(custom)SurfCustomTabActivity::class.java else BrowserActivity::class.java
   startActivity(Intent(source).apply{setClass(this@BrowserEntryActivity,target);selector=null;setPackage(packageName);flags=0})
  }
  finish()
 }
}
class SurfCustomTabActivity:BrowserActivity(){override val isCustomTab=true}
