package com.fspedx.booster.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.services.GameVpnService
import com.fspedx.booster.utils.constants.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkViewModel : ViewModel() {

    // ============ STATE FLOWS (Modern) ============

    // VPN status
    private val _vpnStatus = MutableStateFlow(false)
    val vpnStatus: StateFlow<Boolean> = _vpnStatus.asStateFlow()

    // DNS status (true = using Smart DNS)
    private val _dnsStatus = MutableStateFlow(false)
    val dnsStatus: StateFlow<Boolean> = _dnsStatus.asStateFlow()

    // Dual-Channel status
    private val _dualChannelStatus = MutableStateFlow(false)
    val dualChannelStatus: StateFlow<Boolean> = _dualChannelStatus.asStateFlow()

    // Current network type
    private val _networkType = MutableStateFlow("Unknown")
    val networkType: StateFlow<String> = _networkType.asStateFlow()

    // Wi-Fi low-latency status
    private val _lowLatencyStatus = MutableStateFlow(false)
    val lowLatencyStatus: StateFlow<Boolean> = _lowLatencyStatus.asStateFlow()

    // Connection quality (0 = poor, 1 = fair, 2 = good, 3 = excellent)
    private val _connectionQuality = MutableStateFlow(0)
    val connectionQuality: StateFlow<Int> = _connectionQuality.asStateFlow()

    // ============ LIVE DATA (Legacy/Compat) ============

    private val _ping = MutableLiveData<Int>()
    val ping: LiveData<Int> = _ping

    private val _packetLoss = MutableLiveData<Float>()
    val packetLoss: LiveData<Float> = _packetLoss

    // ============ INIT ============

    init {
        // Load preferences
        val prefs = FSPEDXApplication.instance.prefs
        _vpnStatus.value = GameVpnService.isRunning()
        _dnsStatus.value = prefs.getBoolean(Constants.KEY_SMART_DNS, true)
        _dualChannelStatus.value = prefs.getBoolean(Constants.KEY_DUAL_CHANNEL, false)
        _lowLatencyStatus.value = prefs.getBoolean(Constants.KEY_WIFI_LOW_LATENCY, true)

        // Start network monitoring
        startNetworkMonitoring()
    }

    // ============ PUBLIC METHODS ============

    /**
     * Start monitoring network changes.
     * Updates networkType, connectionQuality, and ping automatically.
     */
    private fun startNetworkMonitoring() {
        val connectivityManager = FSPEDXApplication.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkInfo()
            }

            override fun onLost(network: Network) {
                updateNetworkInfo()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                updateNetworkInfo()
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Initial update
        updateNetworkInfo()
    }

    /**
     * Update network info (type, quality).
     */
    private fun updateNetworkInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = withContext(Dispatchers.Main) {
                getNetworkInfo(FSPEDXApplication.instance)
            }
            _networkType.value = info.first
            _connectionQuality.value = info.second
        }
    }

    /**
     * Get the current network type and quality.
     * @return Pair(networkType, quality)
     * Quality: 0=poor, 1=fair, 2=good, 3=excellent
     */
    private fun getNetworkInfo(context: Context): Pair<String, Int> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return Pair("None", 0)
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return Pair("None", 0)

        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }

        // Estimate quality based on bandwidth and latency
        val downSpeed = caps.linkDownstreamBandwidthKbps
        val quality = when {
            downSpeed >= 100000 -> 3 // Excellent (>100 Mbps)
            downSpeed >= 20000 -> 2  // Good (>20 Mbps)
            downSpeed >= 5000 -> 1   // Fair (>5 Mbps)
            else -> 0                // Poor
        }

        return Pair(type, quality)
    }

    /**
     * Toggle Smart DNS.
     * If enabled, switches to Cloudflare DNS.
     */
    fun toggleSmartDNS(enabled: Boolean) {
        _dnsStatus.value = enabled
        val prefs = FSPEDXApplication.instance.prefs
        prefs.edit().putBoolean(Constants.KEY_SMART_DNS, enabled).apply()

        // If VPN is running, restart it to apply new DNS
        if (GameVpnService.isRunning()) {
            GameVpnService.stop(FSPEDXApplication.instance)
            // Give it a moment to stop, then restart
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                GameVpnService.start(FSPEDXApplication.instance)
            }, 500)
        }
    }

    /**
     * Toggle Dual-Channel Rescue.
     * Opens system settings for the user to enable.
     */
    fun toggleDualChannel(enabled: Boolean) {
        _dualChannelStatus.value = enabled
        val prefs = FSPEDXApplication.instance.prefs
        prefs.edit().putBoolean(Constants.KEY_DUAL_CHANNEL, enabled).apply()
    }

    /**
     * Update VPN status.
     * Called when the VPN service starts or stops.
     */
    fun updateVpnStatus(isRunning: Boolean) {
        _vpnStatus.value = isRunning
    }

    /**
     * Update ping value from the network.
     * Estimates packet loss from ping stability: consecutive samples that jump
     * by more than 3x (or time out) indicate loss. We track a small history.
     */
    private val pingHistory = ArrayDeque<Int>()

    fun updatePing(pingMs: Int) {
        _ping.value = pingMs

        // Estimate packet loss from ping variance over the last 10 samples.
        pingHistory.addLast(pingMs)
        if (pingHistory.size > 10) pingHistory.removeFirst()

        if (pingHistory.size < 3) {
            _packetLoss.value = 0f
            return
        }

        val avg = pingHistory.average()
        var lost = 0
        for (p in pingHistory) {
            if (p < 0) { lost++; continue }
            // A sample more than 2.5x the average is treated as a lost/retransmit
            if (avg > 0 && p > avg * 2.5) lost++
        }
        val loss = lost.toFloat() / pingHistory.size.toFloat()
        _packetLoss.value = loss
    }

    /**
     * Check if any network connection is available.
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = FSPEDXApplication.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
