package com.fspedx.booster.utils.performance

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.fspedx.booster.utils.constants.Constants
import java.io.BufferedReader
import java.io.FileReader

/**
 * Thermal management utilities.
 * Reads battery temperature, CPU temperature, and battery level.
 */
object ThermalUtils {

    /**
     * Get the battery temperature in Celsius.
     */
    fun getBatteryTemperature(context: Context): Double {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        if (batteryIntent == null) return 0.0
        
        val temp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        return temp / 10.0 // Temperature is in tenths of a degree Celsius
    }

    /**
     * Get the battery level as a percentage (0-100).
     */
    fun getBatteryLevel(context: Context): Int {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        if (batteryIntent == null) return 0
        
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return (level * 100 / scale)
    }

    /**
     * Get the CPU temperature (may not be available on all devices).
     * @return Temperature in Celsius, or 0.0 if not available.
     */
    fun getCpuTemperature(): Double {
        // Common thermal zone paths across devices/kernels
        val thermalPaths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone2/temp",
            "/sys/class/thermal/thermal_zone3/temp",
            "/sys/class/thermal/thermal_zone4/temp",
            "/sys/class/thermal/thermal_zone5/temp",
            "/sys/class/thermal/thermal_zone6/temp",
            "/sys/class/thermal/thermal_zone7/temp",
            "/sys/class/thermal/thermal_zone8/temp",
            "/sys/class/thermal/thermal_zone9/temp",
            "/sys/class/thermal/thermal_zone10/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/devices/virtual/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone2/temp",
            "/sys/class/hwmon/hwmon0/temp1_input",
            "/sys/class/hwmon/hwmon1/temp1_input",
            "/sys/class/thermal/cooling_device0/temp"
        )

        for (path in thermalPaths) {
            try {
                val reader = BufferedReader(FileReader(path))
                val tempStr = reader.readLine()
                reader.close()

                val temp = tempStr.toDoubleOrNull() ?: continue
                // Some zones report in millidegrees (e.g. 45000), some in degrees
                // (e.g. 45). Values above 1000 are milli-degrees.
                val normalized = if (temp > 1000) temp / 1000.0 else temp
                // Ignore implausible values (0 or > 120°C) and keep scanning.
                if (normalized in 1.0..120.0) return normalized
            } catch (e: Exception) {
                // File not found or unreadable - continue
            }
        }
        return 0.0
    }

    /**
     * Get the thermal status as an integer.
     * 0 = Normal, 1 = Warning, 2 = Critical
     */
    fun getThermalStatus(context: Context): Int {
        val temp = getBatteryTemperature(context)
        return when {
            temp >= Constants.TEMP_CRITICAL_THRESHOLD -> 2 // Critical
            temp >= Constants.TEMP_WARNING_THRESHOLD -> 1 // Warning
            else -> 0 // Normal
        }
    }

    /**
     * Get thermal status as a human-readable string.
     */
    fun getThermalStatusText(context: Context): String {
        return when (getThermalStatus(context)) {
            0 -> "Normal"
            1 -> "Warming"
            2 -> "Critical"
            else -> "Unknown"
        }
    }

    /**
     * Check if the device is overheating (critical threshold).
     */
    fun isOverheating(context: Context): Boolean {
        return getThermalStatus(context) >= 2
    }

    /**
     * Check if the device is warming up (warning threshold).
     */
    fun isWarming(context: Context): Boolean {
        return getThermalStatus(context) >= 1
    }

    /**
     * Check if the device is charging.
     */
    fun isCharging(context: Context): Boolean {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        if (batteryIntent == null) return false
        
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Get battery voltage (millivolts).
     */
    fun getBatteryVoltage(context: Context): Int {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        return batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
    }
}
