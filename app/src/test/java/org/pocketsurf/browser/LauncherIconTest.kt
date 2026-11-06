package org.pocketsurf.browser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the compiled launcher resources, not a mock SVG or hand-drawn test replacement. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherIconTest {
 @Test fun compiledAdaptiveLauncherHasForegroundMonochromeAndVisibleArtwork(){
  val app=RuntimeEnvironment.getApplication()
  val drawable=app.getDrawable(R.mipmap.ic_launcher)
  assertTrue("Launcher must use Android adaptive icon resources",drawable is AdaptiveIconDrawable)
  val icon=drawable as AdaptiveIconDrawable
  assertNotNull(icon.foreground);assertNotNull(icon.background);assertNotNull("Android 13 themed icon missing",icon.monochrome)
  val mark=Bitmap.createBitmap(108,108,Bitmap.Config.ARGB_8888)
  icon.foreground.setBounds(0,0,108,108);icon.foreground.draw(Canvas(mark))
  val opaque=mutableListOf<Pair<Int,Int>>()
  for(y in 0 until mark.height)for(x in 0 until mark.width)if(Color.alpha(mark.getPixel(x,y))>128)opaque.add(x to y)
  assertTrue("Compiled PS foreground is empty",opaque.size>200)
  assertTrue("Foreground artwork clips left",opaque.minOf{it.first}>0);assertTrue("Foreground artwork clips right",opaque.maxOf{it.first}<107)
  assertTrue("Foreground artwork clips top",opaque.minOf{it.second}>0);assertTrue("Foreground artwork clips bottom",opaque.maxOf{it.second}<107)
  val mono=Bitmap.createBitmap(108,108,Bitmap.Config.ARGB_8888)
  icon.monochrome!!.setBounds(0,0,108,108);icon.monochrome!!.draw(Canvas(mono))
  assertTrue("Compiled monochrome artwork is empty",(0 until 108).sumOf{y->(0 until 108).count{x->Color.alpha(mono.getPixel(x,y))>128}}>200)
  for(y in 0 until 108)for(x in 0 until 108)assertEquals("Blue and solid silhouettes differ",Color.alpha(mark.getPixel(x,y)),Color.alpha(mono.getPixel(x,y)))
  val hues=opaque.map{mark.getPixel(it.first,it.second)}.toSet()
  assertTrue("The supplied blue radial gradient is missing",hues.size>20)
  val bitmap=Bitmap.createBitmap(512,540,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);canvas.drawColor(0xff10151e.toInt())
  icon.setBounds(32,24,480,472);icon.draw(canvas)
  var blue=0;var white=0
  for(y in 24 until 472)for(x in 32 until 480){val c=bitmap.getPixel(x,y);if(Color.blue(c)>150&&Color.red(c)<80)blue++;if(Color.red(c)>230&&Color.green(c)>230&&Color.blue(c)>230)white++}
  assertTrue("Blue logo missing",blue>1000);assertTrue("Neutral adaptive background missing",white>10000)
  val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=13f};canvas.drawText("Compiled Android launcher · API 33 native render",32f,512f,paint)
  val dir=File(requireNotNull(System.getProperty("pocketsurf.snapshots")));dir.mkdirs();File(dir,"19-launcher.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
 }
}
