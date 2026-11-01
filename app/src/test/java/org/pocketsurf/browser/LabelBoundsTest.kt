package org.pocketsurf.browser

import android.graphics.*
import android.view.*
import android.widget.TextView
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LabelBoundsTest {
 @org.junit.Before fun skipFirstRunForBrowserFixture(){org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().putBoolean("setup_complete",true).commit()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun click(a:BrowserActivity,s:String){if(s=="Settings"){if(all(a.window.decorView).any{it.contentDescription=="Browser menu popup"})a.dispatchKeyEvent(KeyEvent(0,97));all(a.window.decorView).first{it.contentDescription=="Settings"}.performClick();return};var v:View=all(a.window.decorView).filterIsInstance<TextView>().first{it.text.toString()==s};while(!v.hasOnClickListeners())v=v.parent as View;v.performClick()}
 @Test fun allPlaystationSymbolsFitInMappingAndLegendLabels(){
  val c=Robolectric.buildActivity(BrowserActivity::class.java).setup().visible();val a=c.get();a.dispatchKeyEvent(KeyEvent(0,4));click(a,"Settings");click(a,"Controller");click(a,"SNES labels")
  for(symbol in listOf("cross","circle","square","triangle"))for((w,h) in listOf(42 to 28,22 to 22,16 to 16)){
   val view=all(a.window.decorView).first{it.contentDescription?.toString()==symbol};view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap));val pixels=mutableListOf<Pair<Int,Int>>();for(y in 0 until h)for(x in 0 until w)if(Color.alpha(bitmap.getPixel(x,y))>0)pixels.add(x to y)
   assertTrue("$symbol drawn",pixels.size>12);assertTrue("$symbol left",pixels.minOf{it.first}>0);assertTrue("$symbol right",pixels.maxOf{it.first}<w-1);assertTrue("$symbol top",pixels.minOf{it.second}>0);assertTrue("$symbol bottom",pixels.maxOf{it.second}<h-1)
  };c.pause().stop().destroy()
 }
}
