package com.fspedx.booster.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.fspedx.booster.services.GameVpnService
import com.fspedx.booster.utils.constants.Constants

/**
 * Background Data Starvation (Feature #30).
 *
 * Goal: stop WhatsApp / Instagram / other apps from syncing in the background
 * while PES is active, which otherwise wakes the CPU every few seconds.
 *
 * Reality check: NetworkPolicyManager.setUidPolicy() requires the privileged
 * MANAGE_NETWORK_POLICY permission that normal apps cannot hold. What a normal
 * app CAN do:
 *  1. Run the blocking VPN (only PES traffic is allowed through the tunnel,
 *     everything else is dropped by the VPN itself). This is the real, effective
 *     mechanism and it's already implemented in GameVpnService.
 *  2. Bind the active network to the app so the network is kept on and stable.
 *
 * This governor simply coordinates those: when "starvation" is enabled AND PES
 * is in the foreground, it ensures the VPN is running (which blocks background
 * sync). When disabled or PES leaves the foreground, it stops the VPN.
 */
object DataGovernor {

    private var lastState: Boolean? = null

    /**
     * Evaluate whether background data starvation should be active.
     * Call whenever PES foreground state or the pref changes.
     * @param pesForeground Whether PES is currently the foreground app.
     */
    fun evaluate(context: Context, pesForeground: Boolean) {
        val prefs = (context.applicationContext as com.fspedx.booster.FSPEDXApplication).prefs
        val enabled = prefs.getBoolean(Constants.KEY_BACKGROUND_DATA_STARVATION, false)
        val shouldRun = enabled && pesForeground

        if (shouldRun == lastState) return
        lastState = shouldRun

        if (shouldRun) {
            // Ensure the blocking VPN is running: it drops all non-PES traffic.
            val vpnIntent = android.net.VpnService.prepare(context)
            if (vpnIntent == null && !GameVpnService.isRunning()) {
                GameVpnService.start(context)
            }
        } else {
            // PES is no longer active or feature disabled — restore normal traffic.
            if (GameVpnService.isRunning()) {
                GameVpnService.stop(context)
            }
        }
    }

    /**
     * Reset the cached state (e.g. on app start).
     */
    fun reset() {
        lastState = null
    }

    /**
     * Keep the device's network connected and stable while gaming.
     * This helps the VPN tunnel stay up under heavy load.
     */
    fun bindToNetwork(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    try {
                        cm.bindProcessToNetwork(network)
                    } catch (e: Exception) {
                        // Ignore — binding is best-effort
                    }
                }
            })
        } catch (e: Exception) {
            // Ignore
        }
    }
}
