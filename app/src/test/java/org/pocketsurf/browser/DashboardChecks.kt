package org.pocketsurf.browser
import android.view.*
import android.widget.*
import org.junit.Assert.*
import java.time.Duration

/** Shared acceptance checks for the single-surface settings contract. */
internal object DashboardChecks {
 fun all(a:BrowserActivity)=BrowserTestActions.views(a.window.decorView)
 fun field(a:BrowserActivity,n:String)=BrowserTestActions.field(a,n)
 fun call(a:BrowserActivity,n:String,vararg args:Any?)=BrowserTestActions.call(a,n,*args)
 fun settle(a:BrowserActivity){repeat(3){BrowserTestActions.settle(a);TestFrameClock.advance(Duration.ofMillis(150))}}
 fun node(a:BrowserActivity,id:String)=all(a).first{it.isShown&&it.contentDescription==id}
 fun click(a:BrowserActivity,id:String){node(a,id).performClick();settle(a)}
 fun open(a:BrowserActivity,category:String){if(field(a,"screen")!="settings")call(a,"showSettingsRoot");settle(a);click(a,"Category: $category")}
 fun key(a:BrowserActivity,key:Int){a.dispatchKeyEvent(KeyEvent(0,key));a.dispatchKeyEvent(KeyEvent(1,key));settle(a)}
 fun close(a:BrowserActivity){repeat(3){if(field(a,"screen")=="settings")key(a,97)};assertEquals("page",field(a,"screen"))}
 fun fits(a:BrowserActivity){settle(a);assertEquals("settings",field(a,"screen"));assertNull(field(a,"popupLayer"));assertTrue(all(a).none{it is ScrollView});val pane=node(a,"Settings detail pane");val p=IntArray(2);pane.getLocationOnScreen(p);for(v in all(a).filter{it.isShown&&it.isClickable&&it.contentDescription!="Back"}){val xy=IntArray(2);v.getLocationOnScreen(xy);assertTrue("Clipped ${v.contentDescription}",xy[1]+v.height<=p[1]+pane.height+1)}}
 fun slider(a:BrowserActivity,title:String)=node(a,"$title slider") as SeekBar
 fun mapping(a:BrowserActivity,action:Control,key:Int){open(a,"controller");click(a,action.title);click(a,"Record a button");key(a,key);if(all(a).any{it.isShown&&it.contentDescription=="Confirm setting action"})click(a,"Confirm setting action")}
 fun appearance(a:BrowserActivity){open(a,"appearance");click(a,"Theme: OLED");click(a,"Accent: Red");val p=a.getSharedPreferences("browser",0);assertEquals("OLED",p.getString("theme",null));assertEquals("Red",p.getString("accent",null));fits(a)}
 fun headings(a:BrowserActivity){val positions=mutableSetOf<Pair<Int,Int>>();for(category in SettingsDashboard.categories.keys){open(a,category);val h=node(a,"Settings section heading");val xy=IntArray(2);h.getLocationOnScreen(xy);positions.add(xy[0] to xy[1]);fits(a)};assertEquals(1,positions.size)}
}
