package com.fspedx.booster.di

import android.content.Context
import com.fspedx.booster.data.database.AppDatabase
import com.fspedx.booster.data.repository.StatsRepository
import com.fspedx.booster.utils.backup.BackupManager
import com.fspedx.booster.utils.ml.KMeansCluster
import com.fspedx.booster.utils.network.NetworkUtils
import com.fspedx.booster.utils.performance.RamCleaner
import com.fspedx.booster.utils.performance.RefreshRateUtils
import com.fspedx.booster.utils.performance.ThermalUtils
import com.fspedx.booster.utils.permissions.PermissionHelper

/**
 * Manual dependency injection container.
 * Creates and holds singleton instances of repositories and utilities.
 * Accessible via FSPEDXApplication.appContainer.
 */
class AppContainer(private val context: Context) {

    // ============ DATABASE ============
    /**
     * Singleton database instance.
     * Uses the companion object's getInstance() method.
     */
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    // ============ REPOSITORIES ============
    /**
     * Singleton StatsRepository instance.
     * Provides access to touch events, lag spikes, and network operations.
     */
    val statsRepository: StatsRepository by lazy {
        StatsRepository(context)
    }

    // ============ UTILITIES (Singletons) ============
    /**
     * Permission helper - all methods are static/object-based.
     * No instance needed, but we keep a reference for consistency.
     */
    val permissionHelper: PermissionHelper by lazy {
        PermissionHelper // Kotlin object reference
    }

    /**
     * Network utilities - object-based singleton.
     */
    val networkUtils: NetworkUtils by lazy {
        NetworkUtils
    }

    /**
     * RAM cleaner - object-based singleton.
     */
    val ramCleaner: RamCleaner by lazy {
        RamCleaner
    }

    /**
     * Thermal utilities - object-based singleton.
     */
    val thermalUtils: ThermalUtils by lazy {
        ThermalUtils
    }

    /**
     * Refresh rate utilities - object-based singleton.
     */
    val refreshRateUtils: RefreshRateUtils by lazy {
        RefreshRateUtils
    }

    /**
     * K-Means clustering - object-based singleton.
     */
    val kMeansCluster: KMeansCluster by lazy {
        KMeansCluster
    }

    /**
     * Backup manager - object-based singleton.
     */
    val backupManager: BackupManager by lazy {
        BackupManager
    }

    // ============ CONVENIENCE METHODS ============

    /**
     * Clear all cached data (for testing or logout).
     * This does not clear the database, only in-memory caches if any.
     */
    fun clearCaches() {
        // If we had in-memory caches, clear them here.
        // For now, the repository uses Room which persists.
        // We could add a cache clearing mechanism in the repository.
        // StatsRepository doesn't have a cache yet, but we can add one later.
    }

    /**
     * Destroy all instances (for testing).
     */
    fun destroy() {
        // In a real scenario, we might close database connections.
        // Room handles this automatically.
        AppDatabase.destroyInstance()
        // We don't destroy the repository since it's just a wrapper.
        // But we can clear any in-memory state if added.
    }
}
