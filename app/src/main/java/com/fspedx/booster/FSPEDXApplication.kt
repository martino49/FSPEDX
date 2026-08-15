package com.fspedx.booster

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.preference.PreferenceManager
import com.fspedx.booster.di.AppContainer
import com.fspedx.booster.utils.constants.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FSPEDXApplication : Application() {

    // Singleton instance
    companion object {
        lateinit var instance: FSPEDXApplication
            private set
        lateinit var appContainer: AppContainer
            private set
    }

    // SharedPreferences (lazy init)
    val prefs: SharedPreferences by lazy { 
        PreferenceManager.getDefaultSharedPreferences(this) 
    }

    // StateFlow to track if PES is currently in the foreground
    private val _isPesForeground = MutableStateFlow(false)
    val isPesForeground: StateFlow<Boolean> = _isPesForeground

    // Coroutine scope for background tasks
    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Foreground monitor job (cancellable)
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContainer = AppContainer(this)

        // Start monitoring foreground app if Auto-Launch is enabled
        if (prefs.getBoolean(Constants.KEY_AUTO_LAUNCH, true)) {
            startForegroundMonitoring()
        }
    }

    /**
     * (Re)starts the foreground monitor. Idempotent — cancels any existing
     * job first so we never leak a second infinite loop.
     */
    fun startForegroundMonitoring() {
        stopForegroundMonitoring()
            monitorJob = applicationScope.launch {
            while (isActive) {
                val isPesRunning = isPesInForeground()
                if (isPesRunning != _isPesForeground.value) {
                    _isPesForeground.value = isPesRunning
                    // Keep Background Data Starvation (#30) in sync with the
                    // actual PES foreground state.
                    com.fspedx.booster.utils.network.DataGovernor.evaluate(this@FSPEDXApplication, isPesRunning)
                    // Keep screen on while PES is foreground (if enabled)
                    updateScreenState(isPesRunning)
                    if (isPesRunning) {
                        // PES launched! Trigger auto-boost
                        onPesLaunched()
                    }
                }
                delay(2000) // Check every 2 seconds
            }
        }
    }

    /**
     * Cancels the foreground monitor job. Safe to call multiple times.
     */
    fun stopForegroundMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * Re-evaluates whether to run the monitor, honouring the current pref.
     * Called when the Auto-Launch toggle changes.
     */
    fun applyAutoLaunchPreference(enabled: Boolean) {
        if (enabled) {
            startForegroundMonitoring()
        } else {
            stopForegroundMonitoring()
            _isPesForeground.value = false
            updateScreenState(false)
        }
    }

    /**
     * Acquire/release the keep-screen-on wake lock based on PES state + pref.
     */
    private fun updateScreenState(pesForeground: Boolean) {
        val enabled = prefs.getBoolean(Constants.KEY_KEEP_SCREEN_ON, false)
        if (pesForeground && enabled) {
            com.fspedx.booster.utils.performance.ScreenKeeper.keepScreenOn(this)
        } else {
            com.fspedx.booster.utils.performance.ScreenKeeper.release()
        }
    }

    /**
     * Checks if PES/eFootball is currently the foreground app.
     * Uses UsageStatsManager (requires PACKAGE_USAGE_STATS permission).
     */
    private fun isPesInForeground(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
        
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return false

        val currentTime = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - 1000 * 60 * 5, // Last 5 minutes
            currentTime
        )

        if (stats.isNullOrEmpty()) return false

        // Sort by last time used
        val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
        val topPackage = sortedStats.firstOrNull()?.packageName ?: return false

        return topPackage in Constants.PES_PACKAGE_NAMES
    }

    /**
     * Called automatically when PES is detected.
     * Triggers RAM boost and enables all selected boosters.
     */
    private fun onPesLaunched() {
        applicationScope.launch {
            // If Auto-Boost is enabled, trigger RAM cleanup with the whitelist
            if (prefs.getBoolean(Constants.KEY_AUTO_BOOST, true)) {
                val whitelist = prefs.getStringSet(Constants.KEY_WHITELIST_APPS, emptySet())
                    ?: emptySet()
                com.fspedx.booster.utils.performance.RamCleaner.forceClean(this@FSPEDXApplication, whitelist)
                android.util.Log.d("FSPEDX", "PES launched! Auto-boost applied.")
            }
        }
    }

    // Clean up on app termination
    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }
}
