package org.pocketsurf.browser

import android.net.Uri
import android.os.Bundle
import android.webkit.WebSettings
import androidx.browser.customtabs.CustomTabsService
import androidx.browser.customtabs.CustomTabsSessionToken

/** Basic Custom Tabs provider. Unsupported optional protocols fail explicitly. */
class SurfCustomTabsService:CustomTabsService(){
 override fun warmup(flags:Long):Boolean {android.os.Handler(mainLooper).post{runCatching{WebSettings.getDefaultUserAgent(applicationContext)}};return true}
 override fun newSession(sessionToken:CustomTabsSessionToken)=true
 override fun mayLaunchUrl(sessionToken:CustomTabsSessionToken,url:Uri?,extras:Bundle?,otherLikelyBundles:MutableList<Bundle>?)=false
 override fun extraCommand(commandName:String,args:Bundle?):Bundle?=null
 override fun updateVisuals(sessionToken:CustomTabsSessionToken,bundle:Bundle?)=false
 override fun requestPostMessageChannel(sessionToken:CustomTabsSessionToken,postMessageOrigin:Uri)=false
 override fun postMessage(sessionToken:CustomTabsSessionToken,message:String,extras:Bundle?)=RESULT_FAILURE_DISALLOWED
 override fun validateRelationship(sessionToken:CustomTabsSessionToken,relation:Int,origin:Uri,extras:Bundle?)=false
 override fun receiveFile(sessionToken:CustomTabsSessionToken,uri:Uri,purpose:Int,extras:Bundle?)=false
}
