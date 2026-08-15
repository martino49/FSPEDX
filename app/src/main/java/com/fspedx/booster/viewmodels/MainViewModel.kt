package com.fspedx.booster.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.data.repository.StatsRepository
import com.fspedx.booster.utils.constants.Constants
import com.fspedx.booster.utils.network.NetworkUtils
import com.fspedx.booster.utils.performance.RamCleaner
import com.fspedx.booster.utils.performance.ThermalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val repository: StatsRepository = StatsRepository()
) : ViewModel() {

    // ============ LIVE DATA ============

    // Ping
    private val _lastPing = MutableLiveData<Int>()
    val lastPing: LiveData<Int> = _lastPing

    // RAM Usage (Pair: Used MB, Total MB)
    private val _ramUsage = MutableLiveData<Pair<Int, Int>>()
    val ramUsage: LiveData<Pair<Int, Int>> = _ramUsage

    // Boost Status (0 = idle, 1 = boosting, 2 = complete)
    private val _boostStatus = MutableLiveData(0)
    val boostStatus: LiveData<Int> = _boostStatus

    // Last boost result (MB freed)
    private val _lastBoostResult = MutableLiveData(0)
    val lastBoostResult: LiveData<Int> = _lastBoostResult

    // Battery and Temperature (combined)
    private val _batteryInfo = MutableLiveData<Pair<Int, Double>>() // (Level %, Temp °C)
    val batteryInfo: LiveData<Pair<Int, Double>> = _batteryInfo

    // ============ INIT ============

    init {
        // Load initial data
        loadPingFromPrefs()
        updateRamUsage()
        updateBatteryInfo()
    }

    // ============ PUBLIC METHODS ============

    /**
     * Perform a RAM boost (80% priority lock).
     * Runs in background, updates UI via LiveData.
     */
    fun performBoost(whitelist: Set<String>) {
        viewModelScope.launch {
            _boostStatus.value = 1 // Boosting

            val freedMB = withContext(Dispatchers.IO) {
                RamCleaner.forceClean(
                    FSPEDXApplication.instance,
                    whitelist
                )
            }

            _lastBoostResult.value = freedMB
            _boostStatus.value = 2 // Complete

            // Update RAM usage after boost
            updateRamUsage()

            // Reset status after 3 seconds
            delay(3000)
            _boostStatus.value = 0 // Idle
        }
    }

    /**
     * Run a manual ping test to the default host.
     */
    fun runPingTest() {
        viewModelScope.launch(Dispatchers.IO) {
            val ping = NetworkUtils.ping(Constants.PING_DEFAULT_HOST)
            withContext(Dispatchers.Main) {
                if (ping > 0) {
                    _lastPing.value = ping
                    // Save to SharedPreferences
                    val prefs = FSPEDXApplication.instance.prefs
                    prefs.edit().apply {
                        putInt(Constants.KEY_LAST_PING, ping)
                        putLong(Constants.KEY_LAST_PING_TIMESTAMP, System.currentTimeMillis())
                        apply()
                    }
                } else {
                    // If ping fails, keep the last known value
                }
            }
        }
    }

    /**
     * Update RAM usage from the system.
     */
    fun updateRamUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val usage = RamCleaner.getRamUsage(FSPEDXApplication.instance)
            withContext(Dispatchers.Main) {
                _ramUsage.value = usage
            }
        }
    }

    /**
     * Update battery level and temperature.
     */
    fun updateBatteryInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val level = ThermalUtils.getBatteryLevel(FSPEDXApplication.instance)
            val temp = ThermalUtils.getBatteryTemperature(FSPEDXApplication.instance)
            withContext(Dispatchers.Main) {
                _batteryInfo.value = Pair(level, temp)
            }
        }
    }

    // ============ PRIVATE HELPERS ============

    private fun loadPingFromPrefs() {
        val prefs = FSPEDXApplication.instance.prefs
        val ping = prefs.getInt(Constants.KEY_LAST_PING, 0)
        if (ping > 0) {
            _lastPing.value = ping
        }
    }

    // ============ CLEANUP ============

    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing tasks if needed
    }
}
