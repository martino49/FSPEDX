package com.fspedx.booster.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.fspedx.booster.data.database.AppDatabase
import com.fspedx.booster.data.models.TouchEvent
import kotlinx.coroutines.*

/**
 * Accessibility Service that records all touch events.
 * Classifies touches as Tap, Swipe, or Hold.
 * Data is saved to Room database for heatmap generation.
 */
class TouchAccessibilityService : AccessibilityService() {

    private var isRecording = false
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Gesture tracking
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartTime = 0L
    private var isTouchDown = false

    companion object {
        private var instance: TouchAccessibilityService? = null

        fun isRunning(): Boolean = instance != null

        fun toggleRecording(context: Context) {
            val svc = instance ?: return
            svc.isRecording = !svc.isRecording
        }

        fun startRecording() {
            instance?.isRecording = true
        }

        fun stopRecording() {
            instance?.isRecording = false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRecording = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRecording) return
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Tap detected (short click) — bounds are reliable on all devices
                val node = event.source
                node?.let {
                    val rect = Rect()
                    it.getBoundsInScreen(rect)
                    if (rect.width() > 0 || rect.height() > 0) {
                        val x = rect.centerX().toFloat()
                        val y = rect.centerY().toFloat()
                        saveTouchEvent(x, y, TouchEvent.TYPE_TAP)
                    }
                }
            }
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                // Touch down. AccessibilityEvent has no reliable x/y fields for
                // touch interaction events delivered to a background service, so
                // we approximate using the active window's bounds.
                val point = approximateTouchPoint()
                touchStartX = point.first
                touchStartY = point.second
                touchStartTime = System.currentTimeMillis()
                isTouchDown = true
            }
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
                // Touch up
                if (isTouchDown) {
                    val duration = System.currentTimeMillis() - touchStartTime
                    val point = approximateTouchPoint()
                    val x = point.first
                    val y = point.second

                    val type = when {
                        duration > 500 -> TouchEvent.TYPE_HOLD
                        else -> TouchEvent.TYPE_TAP
                    }

                    // Check if it moved (swipe)
                    val deltaX = x - touchStartX
                    val deltaY = y - touchStartY
                    val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

                    val finalType = if (distance > 100) TouchEvent.TYPE_SWIPE else type

                    saveTouchEvent(x, y, finalType)
                    isTouchDown = false
                }
            }
        }
    }

    /**
     * Approximate a touch point using the active window bounds, since raw
     * touch coordinates aren't exposed to background accessibility services.
     */
    private fun approximateTouchPoint(): Pair<Float, Float> {
        val root = rootInActiveWindow
        if (root != null) {
            val rect = Rect()
            root.getBoundsInScreen(rect)
            if (rect.width() > 0 || rect.height() > 0) {
                return Pair(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
        }
        return Pair(touchStartX, touchStartY)
    }

    private fun saveTouchEvent(x: Float, y: Float, type: String) {
        serviceScope.launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val touchEvent = TouchEvent(
                    x = x,
                    y = y,
                    timestamp = System.currentTimeMillis(),
                    type = type
                )
                db.touchEventDao().insert(touchEvent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
    }
}
