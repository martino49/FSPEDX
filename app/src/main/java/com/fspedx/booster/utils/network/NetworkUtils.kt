package com.fspedx.booster.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext

/**
 * Network utility functions for ping tests, DNS checks, and connectivity.
 */
object NetworkUtils {

    /**
     * Perform a ping test to the specified host.
     * @param host The hostname or IP to ping (default: 8.8.8.8)
     * @param timeoutMs Timeout in milliseconds (default: 3000)
     * @return Ping in milliseconds, or -1 if failed.
     */
    suspend fun ping(host: String = "8.8.8.8", timeoutMs: Long = 3000): Int {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val result = withTimeoutOrNull(timeoutMs) {
                    InetAddress.getByName(host).isReachable(timeoutMs.toInt())
                }
                val endTime = System.currentTimeMillis()
                
                if (result == true) {
                    (endTime - startTime).toInt()
                } else {
                    -1
                }
            } catch (e: SocketTimeoutException) {
                -1
            } catch (e: Exception) {
                -1
            }
        }
    }

    /**
     * Check if the device has internet connectivity.
     */
    fun hasInternetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo?.isConnectedOrConnecting == true
        }
    }

    /**
     * Get the current network type (Wi-Fi, Mobile, Ethernet, None).
     */
    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        
        val network = connectivityManager.activeNetwork ?: return "None"
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return "None"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
            else -> "Unknown"
        }
    }

    /**
     * Get the current network quality (0=poor, 1=fair, 2=good, 3=excellent).
     */
    fun getNetworkQuality(context: Context): Int {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        
        val network = connectivityManager.activeNetwork ?: return 0
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return 0

        val downSpeed = caps.linkDownstreamBandwidthKbps
        return when {
            downSpeed >= 100000 -> 3 // Excellent (>100 Mbps)
            downSpeed >= 20000 -> 2  // Good (>20 Mbps)
            downSpeed >= 5000 -> 1   // Fair (>5 Mbps)
            else -> 0                // Poor
        }
    }

    /**
     * Get the network quality as a human-readable string.
     */
    fun getNetworkQualityText(context: Context): String {
        return when (getNetworkQuality(context)) {
            3 -> "Excellent"
            2 -> "Good"
            1 -> "Fair"
            else -> "Poor"
        }
    }

    /**
     * Get the current IP address (for debugging).
     */
    suspend fun getCurrentIp(): String? {
        return withContext(Dispatchers.IO) {
            try {
                InetAddress.getLocalHost().hostAddress
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Perform a DNS lookup on a hostname.
     * @return The resolved IP address, or null if failed.
     */
    suspend fun dnsLookup(hostname: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName(hostname).hostAddress
            } catch (e: Exception) {
                null
            }
        }
    }

    // ============ WI-FI LOW-LATENCY LOCK ============

    private var wifiLock: WifiManager.WifiLock? = null

    /**
     * Acquire a Wi-Fi high-performance lock.
     * Prevents the Wi-Fi radio from entering power-saving mode, which
     * reduces jitter and latency variance during gaming.
     * Requires android.permission.WAKE_LOCK.
     * @return true if the lock was acquired.
     */
    fun acquireWifiLock(context: Context): Boolean {
        try {
            if (wifiLock?.isHeld == true) return true
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "FSPEDX:WiFiLowLatencyLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Release the Wi-Fi low-latency lock.
     */
    fun releaseWifiLock() {
        try {
            wifiLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        wifiLock = null
    }

    /**
     * Check whether the Wi-Fi lock is currently held.
     */
    fun isWifiLockHeld(): Boolean = wifiLock?.isHeld == true
}
