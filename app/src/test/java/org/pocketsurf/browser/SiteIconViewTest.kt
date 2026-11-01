package org.pocketsurf.browser

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class SiteIconViewTest {
 private fun icon(color:Int)=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888).apply{eraseColor(color)}
 @Test fun darkArtworkGetsLightBackingInBothThemes(){val black=icon(Color.BLACK);assertEquals(0xFFF1F3F5.toInt(),SiteIconView.backingColor(black,true));assertEquals(SiteIconView.backingColor(black,true),SiteIconView.backingColor(black,false))}
 @Test fun lightArtworkGetsDarkBackingInBothThemes(){val white=icon(Color.WHITE);assertEquals(0xFF303743.toInt(),SiteIconView.backingColor(white,false));assertEquals(SiteIconView.backingColor(white,true),SiteIconView.backingColor(white,false))}
 @Test fun transparentPixelsDoNotHideDarkArtwork(){val image=icon(Color.TRANSPARENT);image.setPixel(12,12,Color.BLACK);assertEquals(0xFFF1F3F5.toInt(),SiteIconView.backingColor(image,true));assertNotEquals(SiteIconView.backingColor(icon(Color.TRANSPARENT),true),SiteIconView.backingColor(icon(Color.TRANSPARENT),false))}
 @Test fun refreshPreservesArtworkAndCallerTag(){val view=SiteIconView.create(RuntimeEnvironment.getApplication(),"https://github.com",32,true);val bitmap=icon(Color.BLACK);view.tag="caller";view.refresh(bitmap);assertEquals("caller",view.tag);assertSame(bitmap,((view.getChildAt(1) as ImageView).drawable as BitmapDrawable).bitmap);assertEquals(Color.BLACK,bitmap.getPixel(0,0));view.refresh(null);assertEquals("caller",view.tag)}
 @Test fun fallbackUsesHostIdentityWithoutNetwork(){assertEquals("W",SiteIconView.identity("https://en.wikipedia.org/wiki/Main_Page").first);assertEquals("G",SiteIconView.identity("https://www.google.com/search?q=test").first);assertEquals(SiteIconView.identity("https://new.example/a"),SiteIconView.identity("https://new.example/b"));assertNotEquals(SiteIconView.identity("https://google.com").second,SiteIconView.identity("https://bing.com").second)}
}
