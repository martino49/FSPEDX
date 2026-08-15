package com.fspedx.booster.data.models

/**
 * Represents device performance statistics at a given moment.
 * Used by PerformanceViewModel and Side Panel display.
 * This is NOT a Room entity—it's a transient model.
 */
data class PerformanceStats(
    val fps: Int = 0,                   // Frames per second
    val batteryLevel: Int = 0,          // Percentage (0-100)
    val batteryTemperature: Double = 0.0, // Celsius
    val cpuTemperature: Double = 0.0,    // Celsius (may not be available)
    val thermalStatus: Int = 0,          // 0=normal, 1=warning, 2=critical
    val refreshRate: Int = 60,          // Hz (60 or 120)
    val ramUsed: Int = 0,               // MB used
    val ramTotal: Int = 4096,           // MB total
    val timestamp: Long = System.currentTimeMillis()
)
