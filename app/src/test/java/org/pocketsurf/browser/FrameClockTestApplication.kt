package org.pocketsurf.browser

import android.app.Application
import org.robolectric.shadows.ShadowChoreographer
import java.time.Duration

/** Real devices receive VSYNC asynchronously. Do not use Robolectric's synchronous auto-VSYNC
 * for a continuously scheduled production FrameCallback; advance its virtual clock explicitly. */
class FrameClockTestApplication:Application(){
 override fun onCreate(){super.onCreate();ShadowChoreographer.setPaused(true);ShadowChoreographer.setFrameDelay(Duration.ofNanos(16_666_667))}
}

/** Advance virtual wall time in display-sized steps, so continuous VSYNC and animations see
 * intermediate frames instead of Robolectric delivering a single late frame after a large jump. */
internal object TestFrameClock {
 fun advance(duration:Duration){
  var remaining=duration.toNanos()
  val looper=org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
  while(remaining>0){val step=minOf(remaining,16_666_667L);looper.idleFor(Duration.ofNanos(step));remaining-=step}
 }
}
