package org.pocketsurf.browser

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** Tests BrowserActivity's wizard entry integration independently of wizard choice behavior. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi",shadows=[RecordingWebViewShadow::class])
class SetupLaunchTest {
 private val prefs get()=RuntimeEnvironment.getApplication().getSharedPreferences("browser",0)
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 @Before fun clear(){prefs.edit().clear().commit()}
 @Test fun firstBrowserLaunchRequestsSetup(){val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();val a=c.get();val launch=Shadows.shadowOf(a).nextStartedActivityForResult;assertNotNull("First launch must offer setup",launch);assertEquals(SetupActivity::class.java.name,launch.intent.component!!.className);assertFalse("Opening the wizard is not completion",prefs.getBoolean(SetupActivity.COMPLETE,false));c.pause().stop().destroy()}
 @Test fun completedSetupDoesNotLaunchAgain(){prefs.edit().putBoolean(SetupActivity.COMPLETE,true).commit();val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();assertNull("Completed setup must not interrupt browser launch",Shadows.shadowOf(c.get()).nextStartedActivityForResult);c.pause().stop().destroy()}
 @Test fun completedSetupCanBeReopenedFromAbout(){prefs.edit().putBoolean(SetupActivity.COMPLETE,true).commit();val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();val a=c.get();all(a.window.decorView).first{it.contentDescription=="Settings"}.performClick();assertTrue(all(a.window.decorView).none{it.contentDescription=="Run setup again"});all(a.window.decorView).first{it.contentDescription=="About PocketSurf"}.performClick();var action:View=all(a.window.decorView).filterIsInstance<TextView>().first{it.text=="Run setup again"};while(!action.hasOnClickListeners())action=action.parent as View;action.performClick();val launch=Shadows.shadowOf(a).nextStartedActivityForResult;assertNotNull(launch);assertEquals(SetupActivity::class.java.name,launch.intent.component!!.className);c.pause().stop().destroy()}
}
