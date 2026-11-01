package org.pocketsurf.browser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Native onboarding renders, with full-height controls and a separate provenance caption. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="w960dp-h540dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SetupSnapshotTest {
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun shot(a:SetupActivity,name:String){
  val w=960;val h=540;val view=a.window.decorView;view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h)
  TestFrameClock.advance(java.time.Duration.ofMillis(16))
  val bitmap=Bitmap.createBitmap(w,h+18,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);view.draw(canvas);val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK};canvas.drawRect(0f,h.toFloat(),w.toFloat(),(h+18).toFloat(),paint);paint.color=Color.WHITE;paint.textSize=10f;canvas.drawText("Robolectric native setup fixture - not a physical-device screenshot",8f,(h+12).toFloat(),paint)
  for(v in all(view).filter{it is android.widget.Button||it.contentDescription?.startsWith("Search engine: ")==true||it.contentDescription?.startsWith("Pin website: ")==true||it.contentDescription?.startsWith("Setup controls:")==true}){val r=android.graphics.Rect();assertTrue("${v.contentDescription} hidden",v.getGlobalVisibleRect(r));assertEquals("${v.contentDescription} clipped",v.height,r.height())}
  val dir=File(requireNotNull(System.getProperty("pocketsurf.snapshots")));dir.mkdirs();File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
 }
 @Test fun setupPages(){RuntimeEnvironment.getApplication().getSharedPreferences("browser",0).edit().clear().commit();val c=Robolectric.buildActivity(SetupActivity::class.java).setup().visible();val a=c.get();shot(a,"16-setup-engine");all(a.window.decorView).filterIsInstance<TextView>().first{it.text=="Continue"}.performClick();shot(a,"17-setup-pins");c.pause().stop().destroy()}
}
