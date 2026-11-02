package org.pocketsurf.browser

import android.content.*
import android.net.Uri
import android.os.*
import android.webkit.WebView
import androidx.browser.customtabs.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CustomTabsProviderTest {
 private val app get()=RuntimeEnvironment.getApplication()
 private val prefs get()=app.getSharedPreferences("browser",0)
 private val url="https://files.test/attachment.apk"
 @Before fun reset(){prefs.edit().clear().putBoolean(SetupActivity.COMPLETE,true).commit()}
 private fun field(a:BrowserActivity,n:String)=BrowserActivity::class.java.getDeclaredField(n).apply{isAccessible=true}.get(a)
 private fun call(a:BrowserActivity,n:String)=BrowserActivity::class.java.declaredMethods.first{it.name==n&&it.parameterCount==0}.apply{isAccessible=true}.invoke(a)
 private fun intent()=CustomTabsIntent.Builder().build().intent.setData(Uri.parse(url))
 @Test fun providerIsDiscoverableByOtherApps(){val services=app.packageManager.queryIntentServices(Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION).setPackage(app.packageName),0);assertTrue(services.any{it.serviceInfo.name==SurfCustomTabsService::class.java.name&&it.serviceInfo.exported});assertEquals(app.packageName,CustomTabsClient.getPackageName(app,listOf(app.packageName),true))}
 @Test fun basicProtocolSupportsWarmupAndSessionsWithoutPretendingOriginTrust(){val s=Robolectric.buildService(SurfCustomTabsService::class.java).create();var client:CustomTabsClient?=null;val connection=object:CustomTabsServiceConnection(){override fun onCustomTabsServiceConnected(name:ComponentName,c:CustomTabsClient){client=c};override fun onServiceDisconnected(name:ComponentName){}};Shadows.shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app,SurfCustomTabsService::class.java),s.get().onBind(Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)));CustomTabsClient.bindCustomTabsService(app,app.packageName,connection);Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(client!!.warmup(0));val session=client!!.newSession(CustomTabsCallback());assertNotNull(session);assertFalse(session!!.mayLaunchUrl(Uri.parse(url),null,null));assertFalse(session.validateRelationship(CustomTabsService.RELATION_USE_AS_ORIGIN,Uri.parse("https://files.test"),null));assertEquals(CustomTabsService.RESULT_FAILURE_DISALLOWED,session.postMessage("test",null));s.destroy()}
 @Test fun customTabsIntentRoutesToOurOwnWindow(){val c=Robolectric.buildActivity(BrowserEntryActivity::class.java,intent()).create();val next=Shadows.shadowOf(c.get()).nextStartedActivity;assertEquals(SurfCustomTabActivity::class.java.name,next.component!!.className);assertEquals(Uri.parse(url),next.data);assertTrue(c.get().isFinishing);c.destroy()}
 @Test fun ordinaryLinksRouteToTheNormalBrowser(){val c=Robolectric.buildActivity(BrowserEntryActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse(url))).create();assertEquals(BrowserActivity::class.java.name,Shadows.shadowOf(c.get()).nextStartedActivity.component!!.className);c.destroy()}
 @Test fun invalidSchemesCannotUseTheExportedEntry(){val c=Robolectric.buildActivity(BrowserEntryActivity::class.java,Intent(Intent.ACTION_VIEW,Uri.parse("file:///secret"))).create();assertNull(Shadows.shadowOf(c.get()).nextStartedActivity);assertTrue(c.get().isFinishing);c.destroy()}
 @Test fun customWindowDoesNotOverwriteTheNormalTabSessionAndBackClosesIt(){val original="[{\"url\":\"https://normal.test/\",\"title\":\"Normal\",\"home\":false}]";prefs.edit().putString("sessions_v2",original).putInt("active",0).commit();val c=Robolectric.buildActivity(SurfCustomTabActivity::class.java,intent()).setup().visible();val a=c.get();assertEquals(1,(field(a,"tabs") as List<*>).size);assertEquals(url,(call(a,"getWeb") as WebView).url);call(a,"systemBack");assertTrue(a.isFinishing);c.pause().stop().destroy();assertEquals(original,prefs.getString("sessions_v2",null))}
 @Test fun customWindowRestoresItsOwnTabsWithoutLoadingTheNormalSession(){prefs.edit().putString("sessions_v2","[{\"url\":\"https://normal.test/\",\"home\":false}]").commit();val c=Robolectric.buildActivity(SurfCustomTabActivity::class.java,intent()).setup().visible();val state=Bundle();c.saveInstanceState(state).pause().stop().destroy();val next=Robolectric.buildActivity(SurfCustomTabActivity::class.java,intent()).create(state).start().restoreInstanceState(state).resume().visible();assertEquals(1,(field(next.get(),"tabs") as List<*>).size);assertEquals(url,(call(next.get(),"getWeb") as WebView).url);next.pause().stop().destroy()}
 @Test fun navigationCallbacksAreDeliveredToTheOpeningClient(){val s=Robolectric.buildService(SurfCustomTabsService::class.java).create();var client:CustomTabsClient?=null;val connection=object:CustomTabsServiceConnection(){override fun onCustomTabsServiceConnected(name:ComponentName,c:CustomTabsClient){client=c};override fun onServiceDisconnected(name:ComponentName){}};Shadows.shadowOf(app).setComponentNameAndServiceForBindService(ComponentName(app,SurfCustomTabsService::class.java),s.get().onBind(Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)));CustomTabsClient.bindCustomTabsService(app,app.packageName,connection);Shadows.shadowOf(Looper.getMainLooper()).idle();val events=mutableListOf<Int>();val session=client!!.newSession(object:CustomTabsCallback(){override fun onNavigationEvent(event:Int,extras:Bundle?){events.add(event)}});assertNotNull("client session",session);val i=CustomTabsIntent.Builder(session).build().intent.setData(Uri.parse(url));val c=Robolectric.buildActivity(SurfCustomTabActivity::class.java,i).setup().visible();assertNotNull("activity session",field(c.get(),"customSession"));assertNotNull("activity owner",field(c.get(),"customOwner"));val active=call(c.get(),"getTab") as BrowserActivity.Tab;assertEquals(active.id,field(c.get(),"customOwner"));val v=call(c.get(),"getWeb") as WebView;v.webViewClient!!.onPageStarted(v,url,null);v.webViewClient!!.onPageFinished(v,url);TestFrameClock.advance(java.time.Duration.ofMillis(100));assertTrue("started events=$events",events.contains(CustomTabsCallback.NAVIGATION_STARTED));assertTrue("finished events=$events",events.contains(CustomTabsCallback.NAVIGATION_FINISHED));c.pause().stop().destroy();s.destroy()}
}
