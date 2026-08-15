package com.fspedx.booster.utils.performance

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import com.fspedx.booster.utils.constants.Constants

/**
 * RAM management utilities.
 * Handles force-stopping background apps and tracking memory usage.
 */
object RamCleaner {

    /**
     * Force-clean background apps to free RAM.
     * @param context Application context.
     * @param whitelist Set of package names to protect from killing.
     * @return MB of RAM freed.
     */
    fun forceClean(context: Context, whitelist: Set<String>): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageManager = context.packageManager

        // Get running app processes
        val runningProcesses = activityManager.runningAppProcesses ?: return 0
        
        val memoryBefore = getFreeRam(context)
        
        // Iterate and kill non-whitelisted apps
        for (process in runningProcesses) {
            val packageName = process.processName ?: continue
            
            // Skip whitelisted apps
            if (whitelist.contains(packageName)) continue
            
            // Skip the current app (FSPEDX) and system apps
            if (packageName == context.packageName) continue
            if (packageName.startsWith("android.")) continue
            if (packageName.startsWith("com.android.")) continue
            if (packageName.startsWith("com.google.android.")) continue
            
            // Skip PES itself
            if (packageName in Constants.PES_PACKAGE_NAMES) continue
            
            try {
                // Get app info to check if it's a game or user app
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                
                // Don't kill system apps
                if (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0) continue
                
                // Force stop the app
                activityManager.killBackgroundProcesses(packageName)
                
            } catch (e: PackageManager.NameNotFoundException) {
                // App not found - skip
            } catch (e: SecurityException) {
                // Can't kill this app - skip
            }
        }
        
        val memoryAfter = getFreeRam(context)
        return memoryAfter - memoryBefore
    }

    /**
     * Get current free RAM in MB.
     */
    fun getFreeRam(context: Context): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return (memoryInfo.availMem / (1024 * 1024)).toInt()
    }

    /**
     * Get total and used RAM in MB.
     * @return Pair(usedMB, totalMB)
     */
    fun getRamUsage(context: Context): Pair<Int, Int> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMB = (memoryInfo.totalMem / (1024 * 1024)).toInt()
        val availMB = (memoryInfo.availMem / (1024 * 1024)).toInt()
        val usedMB = totalMB - availMB
        
        return Pair(usedMB, totalMB)
    }

    /**
     * Get current RAM usage as a percentage (0-100).
     */
    fun getRamUsagePercent(context: Context): Int {
        val (used, total) = getRamUsage(context)
        return if (total > 0) (used * 100 / total) else 0
    }

    /**
     * Check if RAM usage is above the 80% threshold.
     */
    fun isRamCritical(context: Context): Boolean {
        val (used, total) = getRamUsage(context)
        return used.toFloat() / total.toFloat() >= 0.8f
    }

    /**
     * Get a list of currently running apps (for debugging/display).
     */
    fun getRunningApps(context: Context): List<String> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return emptyList()
        return runningProcesses.mapNotNull { it.processName }
    }

    /**
     * Check if an app is running in the background.
     */
    fun isAppRunning(context: Context, packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false
        return runningProcesses.any { it.processName == packageName }
    }
}
