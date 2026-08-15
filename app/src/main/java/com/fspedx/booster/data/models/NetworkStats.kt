package com.fspedx.booster.data.models

/**
 * Represents network statistics at a given moment.
 * Used by NetworkViewModel and Side Panel display.
 * This is NOT a Room entity—it's a transient model.
 */
data class NetworkStats(
    val ping: Int = 0,                   // Ping in milliseconds
    val packetLoss: Float = 0.0f,        // Packet loss percentage
    val networkType: String = "Unknown", // Wi-Fi, Mobile Data, Ethernet
    val connectionQuality: Int = 0,      // 0=poor, 1=fair, 2=good, 3=excellent
    val isVpnActive: Boolean = false,
    val isDualChannel: Boolean = false,
    val dnsServer: String = "1.1.1.1",   // Current DNS server
    val timestamp: Long = System.currentTimeMillis()
)
