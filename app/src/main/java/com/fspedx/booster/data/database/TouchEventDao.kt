package com.fspedx.booster.data.database

import androidx.room.*
import com.fspedx.booster.data.models.TouchEvent
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TouchEvent.
 * Provides insert, query, and delete methods.
 */
@Dao
interface TouchEventDao {

    /**
     * Insert a single touch event.
     */
    @Insert
    suspend fun insert(event: TouchEvent)

    /**
     * Insert multiple touch events.
     */
    @Insert
    suspend fun insertAll(events: List<TouchEvent>)

    /**
     * Get all touch events (ordered by timestamp).
     */
    @Query("SELECT * FROM touch_events ORDER BY timestamp ASC")
    fun getAll(): Flow<List<TouchEvent>>

    /**
     * Get all touch events as a snapshot list (non-reactive).
     * Used for backup/export purposes.
     */
    @Query("SELECT * FROM touch_events ORDER BY timestamp ASC")
    suspend fun getAllSnapshot(): List<TouchEvent>

    /**
     * Get touch events within a specific timestamp range.
     * Used for session-based filtering.
     */
    @Query("SELECT * FROM touch_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getByTimestampRange(startTime: Long, endTime: Long): List<TouchEvent>

    /**
     * Get touch events of a specific type (Tap, Swipe, Hold).
     */
    @Query("SELECT * FROM touch_events WHERE type = :type ORDER BY timestamp ASC")
    suspend fun getByType(type: String): List<TouchEvent>

    /**
     * Count total number of touch events.
     */
    @Query("SELECT COUNT(*) FROM touch_events")
    suspend fun getCount(): Int

    /**
     * Get the most recent touch event.
     */
    @Query("SELECT * FROM touch_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): TouchEvent?

    /**
     * Delete all touch events (reset heatmap data).
     */
    @Query("DELETE FROM touch_events")
    suspend fun deleteAll()

    /**
     * Delete touch events older than a certain timestamp.
     */
    @Query("DELETE FROM touch_events WHERE timestamp < :beforeTime")
    suspend fun deleteOlderThan(beforeTime: Long)
}
