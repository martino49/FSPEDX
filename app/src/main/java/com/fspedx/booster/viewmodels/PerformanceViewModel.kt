package com.fspedx.booster.viewmodels

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.utils.constants.Constants
import com.fspedx.booster.utils.performance.RefreshRateUtils
import com.fspedx.booster.utils.performance.ThermalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PerformanceViewModel : ViewModel() {

    // ============ STATE FLOWS ============

    // FPS (frames per second)
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    // Battery Temperature (°C)
    private val _temperature = MutableStateFlow(0.0)
    val temperature: StateFlow<Double> = _temperature.asStateFlow()

    // CPU Temperature (°C) - may not be available on all devices
    private val _cpuTemperature = MutableStateFlow(0.0)
    val cpuTemperature: StateFlow<Double> = _cpuTemperature.asStateFlow()

    // Battery Level (%)
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    // Thermal Throttle Status (0=normal, 1=warning, 2=critical)
    private val _thermalStatus = MutableStateFlow(0)
    val thermalStatus: StateFlow<Int> = _thermalStatus.asStateFlow()

    // Current Refresh Rate (Hz)
    private val _refreshRate = MutableStateFlow(60)
    val refreshRate: StateFlow<Int> = _refreshRate.asStateFlow()

    // Is the refresh rate switch enabled (120Hz → 60Hz)
    private val _refreshRateSwitchEnabled = MutableStateFlow(false)
    val refreshRateSwitchEnabled: StateFlow<Boolean> = _refreshRateSwitchEnabled.asStateFlow()

    // Is the thermal sentinel enabled
    private val _thermalSentinelEnabled = MutableStateFlow(false)
    val thermalSentinelEnabled: StateFlow<Boolean> = _thermalSentinelEnabled.asStateFlow()

    // ============ LIVE UPDATE LOOP ============

    private var updateJob: kotlinx.coroutines.Job? = null

    init {
        // Load preferences
        val prefs = FSPEDXApplication.instance.prefs
        _refreshRateSwitchEnabled.value = prefs.getBoolean(Constants.KEY_REFRESH_RATE_SWITCH, true)
        _thermalSentinelEnabled.value = prefs.getBoolean(Constants.KEY_THERMAL_SENTINEL, true)

        // Set initial refresh rate
        if (_refreshRateSwitchEnabled.value) {
            RefreshRateUtils.setRefreshRate(FSPEDXApplication.instance, 60)
            _refreshRate.value = 60
        } else {
            _refreshRate.value = 120
        }

        // Start update loop
        startUpdateLoop()
    }

    private fun startUpdateLoop() {
        updateJob = viewModelScope.launch {
            while (true) {
                updateStats()
                delay(Constants.OVERLAY_UPDATE_INTERVAL_MS)
            }
        }
    }

    private suspend fun updateStats() {
        withContext(Dispatchers.IO) {
            val context = FSPEDXApplication.instance

            // Update temperature
            val temp = ThermalUtils.getBatteryTemperature(context)
            _temperature.value = temp

            // Update CPU temp (if available)
            val cpuTemp = ThermalUtils.getCpuTemperature()
            if (cpuTemp > 0) {
                _cpuTemperature.value = cpuTemp
            }

            // Update battery level
            val level = ThermalUtils.getBatteryLevel(context)
            _batteryLevel.value = level

            // Update thermal status
            _thermalStatus.value = when {
                temp >= Constants.TEMP_CRITICAL_THRESHOLD -> 2 // Critical
                temp >= Constants.TEMP_WARNING_THRESHOLD -> 1 // Warning
                else -> 0 // Normal
            }

            // Thermal Sentinel: at critical temp, force 60Hz (halves GPU work,
            // cutting heat) and notify the UI so it can warn the user.
            if (_thermalSentinelEnabled.value) {
                when {
                    _thermalStatus.value >= 2 -> {
                        if (_refreshRate.value != 60) {
                            RefreshRateUtils.setRefreshRate(context, 60)
                            _refreshRate.value = 60
                        }
                    }
                    _thermalStatus.value == 1 && _refreshRateSwitchEnabled.value -> {
                        // Warning: if the user wants 60Hz anyway, keep it there.
                        if (_refreshRate.value != 60) {
                            RefreshRateUtils.setRefreshRate(context, 60)
                            _refreshRate.value = 60
                        }
                    }
                }
            }

            // Update refresh rate if switch is enabled
            if (_refreshRateSwitchEnabled.value) {
                val currentRate = RefreshRateUtils.getRefreshRate(context)
                if (currentRate != 60) {
                    RefreshRateUtils.setRefreshRate(context, 60)
                    _refreshRate.value = 60
                }
            } else {
                val currentRate = RefreshRateUtils.getRefreshRate(context)
                if (currentRate != 120) {
                    RefreshRateUtils.setRefreshRate(context, 120)
                    _refreshRate.value = 120
                }
            }
        }
    }

    // ============ PUBLIC METHODS ============

    /**
     * Update FPS from the OverlayService callback.
     * Called by the service via a broadcast or direct method.
     */
    fun updateFps(newFps: Int) {
        _fps.value = newFps
    }

    /**
     * Toggle the refresh rate switch (120Hz ↔ 60Hz).
     */
    fun toggleRefreshRateSwitch(enabled: Boolean) {
        _refreshRateSwitchEnabled.value = enabled
        val prefs = FSPEDXApplication.instance.prefs
        prefs.edit().putBoolean(Constants.KEY_REFRESH_RATE_SWITCH, enabled).apply()

        val context = FSPEDXApplication.instance
        if (enabled) {
            RefreshRateUtils.setRefreshRate(context, 60)
            _refreshRate.value = 60
        } else {
            RefreshRateUtils.setRefreshRate(context, 120)
            _refreshRate.value = 120
        }
    }

    /**
     * Toggle the thermal sentinel.
     */
    fun toggleThermalSentinel(enabled: Boolean) {
        _thermalSentinelEnabled.value = enabled
        val prefs = FSPEDXApplication.instance.prefs
        prefs.edit().putBoolean(Constants.KEY_THERMAL_SENTINEL, enabled).apply()
    }

    /**
     * Get a human-readable thermal status string.
     */
    fun getThermalStatusText(): String {
        return when (_thermalStatus.value) {
            0 -> "Normal"
            1 -> "Warming"
            2 -> "Critical"
            else -> "Unknown"
        }
    }

    /**
     * Check if the device needs cooling (temperature > warning).
     */
    fun needsCooling(): Boolean {
        return _thermalStatus.value >= 1
    }

    // ============ CLEANUP ============

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
        updateJob = null
    }
}
