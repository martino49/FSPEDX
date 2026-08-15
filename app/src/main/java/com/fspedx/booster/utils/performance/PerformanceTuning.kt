package com.fspedx.booster.utils.performance

import android.content.Context
import android.provider.Settings
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.utils.constants.Constants

/**
 * GPU / rendering tuning (Feature #28).
 *
 * Writes the same Settings.Global keys the Developer Options toggles use:
 *   - "force_gpu_rendering" → forces GPU to do all drawing (hardware layers),
 *     offloading the CPU.
 *   - "disable_hw_overlays" → forces GPU composition instead of letting the
 *     SurfaceFlinger use hardware overlays (reduces CPU composition work).
 *
 * These require WRITE_SETTINGS. Some keys are read-only on some devices /
 * Android versions, so every write is verified and failures are reported
 * honestly rather than pretending success.
 */
object PerformanceTuning {

    private const val KEY_FORCE_GPU = "force_gpu_rendering"
    private const val KEY_DISABLE_OVERLAYS = "disable_hw_overlays"

    /**
     * Apply the current GPU-tuning preferences. Call after the settings change
     * or at app start. Returns the number of settings successfully written.
     */
    fun applyPreferences(context: Context): Int {
        val prefs = (context.applicationContext as FSPEDXApplication).prefs
        val forceGpu = prefs.getBoolean(Constants.KEY_GPU_FORCE_RENDERING, false)
        val disableOverlays = prefs.getBoolean(Constants.KEY_GPU_DISABLE_OVERLAYS, false)

        var written = 0
        if (setForceGpuRendering(context, forceGpu)) written++
        if (setDisableHwOverlays(context, disableOverlays)) written++
        return written
    }

    /**
     * Force GPU rendering (hardware-accelerated drawing for all apps).
     * @return true if the setting was written successfully.
     */
    fun setForceGpuRendering(context: Context, enabled: Boolean): Boolean {
        if (!Settings.System.canWrite(context)) return false
        return try {
            Settings.Global.putInt(
                context.contentResolver,
                KEY_FORCE_GPU,
                if (enabled) 1 else 0
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Disable hardware overlays (force GPU composition).
     * @return true if the setting was written successfully.
     */
    fun setDisableHwOverlays(context: Context, enabled: Boolean): Boolean {
        if (!Settings.System.canWrite(context)) return false
        return try {
            Settings.Global.putInt(
                context.contentResolver,
                KEY_DISABLE_OVERLAYS,
                if (enabled) 1 else 0
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Read the current state of the two GPU settings.
     * @return Pair(forceGpu, disableOverlays)
     */
    fun getCurrentState(context: Context): Pair<Boolean, Boolean> {
        val forceGpu = try {
            Settings.Global.getInt(context.contentResolver, KEY_FORCE_GPU, 0) == 1
        } catch (e: Exception) {
            false
        }
        val disableOverlays = try {
            Settings.Global.getInt(context.contentResolver, KEY_DISABLE_OVERLAYS, 0) == 1
        } catch (e: Exception) {
            false
        }
        return Pair(forceGpu, disableOverlays)
    }
}
