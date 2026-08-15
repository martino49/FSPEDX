package com.fspedx.booster.utils.performance

import android.content.Context
import android.os.PowerManager

/**
 * Keeps the screen on while PES is in the foreground.
 *
 * Uses a partial-scope wake lock? No — for keeping the *screen* on we need
 * SCREEN_DIM_WAKE_LOCK semantics, but on modern Android the reliable way from
 * an app is a PowerManager full-screen wakelock OR (better) FLAG_KEEP_SCREEN_ON
 * on an overlay window. For a background service the cleanest supported approach
 * is a PowerManager wake lock with SCREEN_BRIGHT_WAKE_LOCK (deprecated but still
 * functional on most devices) — however the fully-supported way is to hold the
 * wake lock with PARTIAL_WAKE_LOCK only keeps the CPU on, not the screen.
 *
 * The correct, supported mechanism for "keep screen on while an overlay is
 * active" is to set FLAG_KEEP_SCREEN_ON on the overlay window (see
 * OverlayService). This utility provides the PowerManager fallback for the
 * short gaps between overlay setup.
 */
object ScreenKeeper {

    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Keep the screen on. Requires WAKE_LOCK permission (declared in manifest).
     * @return true if the screen-wake was acquired.
     */
    fun keepScreenOn(context: Context): Boolean {
        release()
        return try {
            val pm = context.applicationContext
                .getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "FSPEDX:KeepScreenOn"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Stop keeping the screen on.
     */
    fun release() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        wakeLock = null
    }

    fun isHeld(): Boolean = wakeLock?.isHeld == true
}
