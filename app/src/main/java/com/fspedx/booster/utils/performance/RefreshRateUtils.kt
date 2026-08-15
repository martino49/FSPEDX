package com.fspedx.booster.utils.performance

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.WindowManager

/**
 * Utilities for switching screen refresh rates (120Hz ↔ 60Hz).
 *
 * Apps cannot call Display.setMode() (it requires privileged system access), so
 * the working approach for Xiaomi/Redmi/MediaTek devices is to write the
 * refresh-rate setting keys the system actually reads, which requires the
 * WRITE_SETTINGS permission (already declared in the manifest). We also detect
 * and report whether the switch actually took effect so the UI is honest.
 */
object RefreshRateUtils {

    // Known settings keys used by Xiaomi/MIUI and MediaTek devices.
    // We try several and verify with the reported display refresh rate.
    private val REFRESH_RATE_KEYS = listOf(
        "peak_refresh_rate",
        "user_refresh_rate",
        "min_refresh_rate",
        "refresh_rate",
        "display_refresh_rate"
    )

    /**
     * Set the screen refresh rate (in Hz).
     * @return true only if the change was accepted AND the display reports the new rate.
     */
    fun setRefreshRate(context: Context, targetRate: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        if (targetRate != 60 && targetRate != 120) {
            return false
        }

        val canWrite = Settings.System.canWrite(context)
        if (!canWrite) {
            // No WRITE_SETTINGS permission — request it via the caller (Activity).
            // Returning false lets the UI surface the permission prompt.
            return false
        }

        return try {
            var anyWritten = false
            for (key in REFRESH_RATE_KEYS) {
                try {
                    if (Settings.System.putInt(context.contentResolver, key, targetRate)) {
                        anyWritten = true
                    }
                } catch (e: Exception) {
                    // Some keys are read-only on some devices — try the next one
                }
            }

            // Verify: give the display a moment, then read the actual rate.
            Thread.sleep(300)
            val actual = getRefreshRate(context)
            anyWritten && (actual in targetRate - 2..targetRate + 2 || actual == targetRate)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get the current refresh rate (Hz), reading the real display mode when
     * possible and falling back to the settings keys.
     */
    fun getRefreshRate(context: Context): Int {
        return try {
            // Prefer the actual display-reported rate (API 30+).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val rate = windowManager.defaultDisplay.refreshRate
                if (rate > 0f) {
                    return rate.toInt()
                }
            }

            // Fallback: the first settings key that has a real value.
            for (key in REFRESH_RATE_KEYS) {
                try {
                    val value = Settings.System.getInt(context.contentResolver, key, 0)
                    if (value > 0) return value
                } catch (e: Exception) {
                    // continue to next key
                }
            }
            60
        } catch (e: Exception) {
            60
        }
    }

    /**
     * Check if the device supports 120Hz refresh rate.
     */
    fun supports120Hz(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false
        }
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            display.supportedModes.any { it.refreshRate >= 120 }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Set refresh rate with a specific preference.
     * @param use60Hz true → 60Hz, false → 120Hz.
     */
    fun setPreferredRefreshRate(context: Context, use60Hz: Boolean) {
        val targetRate = if (use60Hz) 60 else 120
        setRefreshRate(context, targetRate)
    }

    /**
     * Check whether the app has WRITE_SETTINGS permission.
     */
    fun canWriteSettings(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }
}
