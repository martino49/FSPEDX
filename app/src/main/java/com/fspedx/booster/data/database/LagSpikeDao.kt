package com.fspedx.booster.data.database

import androidx.room.*
import com.fspedx.booster.data.models.LagSpike
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for LagSpike.
 * Provides insert, query, and delete methods.
 */
@Dao
interface LagSpikeDao {

    /**
     * Insert a single lag spike measurement.
     */
    @Insert
    suspend fun insert(spike: LagSpike)

    /**
     * Insert multiple lag spikes.
     */
    @Insert
    suspend fun insertAll(spikes: List<LagSpike>)

    /**
     * Get all lag spikes (ordered by timestamp).
     */
    @Query("SELECT * FROM lag_spikes ORDER BY timestamp ASC")
    fun getAll(): Flow<List<LagSpike>>

    /**
     * Get all lag spikes as a snapshot list (non-reactive).
     * Used for backup/export purposes.
     */
    @Query("SELECT * FROM lag_spikes ORDER BY timestamp ASC")
    suspend fun getAllSnapshot(): List<LagSpike>

    /**
     * Get lag spikes for a specific session.
     */
    @Query("SELECT * FROM lag_spikes WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: Long): List<LagSpike>

    /**
     * Get lag spikes within a timestamp range.
     */
    @Query("SELECT * FROM lag_spikes WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getByTimestampRange(startTime: Long, endTime: Long): List<LagSpike>

    /**
     * Get the latest lag spike measurement.
     */
    @Query("SELECT * FROM lag_spikes ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): LagSpike?

    /**
     * Delete all lag spikes.
     */
    @Query("DELETE FROM lag_spikes")
    suspend fun deleteAll()

    /**
     * Delete lag spikes older than a certain timestamp.
     */
    @Query("DELETE FROM lag_spikes WHERE timestamp < :beforeTime")
    suspend fun deleteOlderThan(beforeTime: Long)
}
