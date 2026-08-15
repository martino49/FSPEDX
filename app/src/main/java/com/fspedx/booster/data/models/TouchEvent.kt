package com.fspedx.booster.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single touch event captured by the Accessibility Service.
 * Used for heatmap generation and K-Means clustering.
 */
@Entity(tableName = "touch_events")
data class TouchEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val x: Float,                        // X coordinate on screen
    val y: Float,                        // Y coordinate on screen
    val timestamp: Long,                 // When the touch occurred (System.currentTimeMillis())
    val type: String = TYPE_TAP          // Tap, Swipe, or Hold
) {
    companion object {
        const val TYPE_TAP = "tap"
        const val TYPE_SWIPE = "swipe"
        const val TYPE_HOLD = "hold"
    }
}
