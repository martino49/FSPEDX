package com.fspedx.booster.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a ping measurement during a match.
 * Used to generate the Lag Timeline chart.
 */
@Entity(tableName = "lag_spikes")
data class LagSpike(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,                // When the spike was recorded
    val ping: Int,                      // Ping in milliseconds at that moment
    val sessionId: Long = 0             // Optional: group by session
)
