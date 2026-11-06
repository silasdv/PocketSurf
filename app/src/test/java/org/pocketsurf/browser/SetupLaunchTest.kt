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
 @Test fun firstLaunchGoesDirectlyToBrowser(){val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();assertNull(Shadows.shadowOf(c.get()).nextStartedActivityForResult);assertEquals("google",SearchEngines.resolve(prefs).id);DashboardChecks.open(c.get(),"about");assertTrue(all(c.get().window.decorView).none{it.contentDescription=="Run setup again"});c.pause().stop().destroy()}

 @Test fun completedSetupDoesNotLaunchAgain(){prefs.edit().putBoolean("setup_complete",true).commit();val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();assertNull("Completed setup must not interrupt browser launch",Shadows.shadowOf(c.get()).nextStartedActivityForResult);c.pause().stop().destroy()}
 @Test fun emptyBookmarksOfferSitesAndCustomEntry(){val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();val a=c.get();BrowserTestActions.call(a,"showCollection","bookmarks");BrowserTestActions.settle(a);assertTrue(all(a.window.decorView).any{it.contentDescription=="Enter a website"});assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any{it.text=="Google"});assertTrue(BrowserData(prefs).entries("bookmarks").isEmpty());c.pause().stop().destroy()}
 @Test fun freshDefaultsAreOledGoogleAndBookmarksBar(){val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();val a=c.get();
  assertEquals("google",SearchEngines.resolve(prefs).id)
  assertEquals(android.graphics.Color.BLACK,((BrowserTestActions.field(a,"root") as View).background as android.graphics.drawable.ColorDrawable).color)
  DashboardChecks.open(a,"browsing");assertTrue(all(a.window.decorView).filterIsInstance<android.widget.Switch>().first{it.contentDescription=="Bookmarks bar switch"}.isChecked);c.pause().stop().destroy()}
 @Test fun bookmarksBarCheckboxPersistsAndSettingsReflectIt(){val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();val a=c.get();BrowserTestActions.call(a,"showCollection","bookmarks");BrowserTestActions.settle(a);val checkbox=all(a.window.decorView).filterIsInstance<android.widget.CheckBox>().first{it.contentDescription=="Show bookmarks bar"};assertTrue(checkbox.isChecked);checkbox.performClick();assertFalse(prefs.getBoolean("favorites_bar",true));DashboardChecks.open(a,"browsing");assertFalse(all(a.window.decorView).filterIsInstance<android.widget.Switch>().first{it.contentDescription=="Bookmarks bar switch"}.isChecked);c.pause().stop().destroy()}
}
