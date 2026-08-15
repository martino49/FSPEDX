package com.fspedx.booster.ui.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Edge Guard overlay (Feature #29).
 *
 * A full-screen transparent overlay that absorbs touches in thin strips along
 * the screen edges while gaming, preventing accidental "back"/"home" edge
 * swipes from leaving the game. The centre of the screen passes all touches
 * through untouched so gameplay is never affected.
 *
 * The overlay is NOT focusable and its touch consumption is limited to the
 * guard strips, so normal taps and swipes in the middle of the screen behave
 * exactly as before.
 */
class EdgeGuardView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        /** Guard strip thickness in dp (each screen edge). */
        const val EDGE_GUARD_DP = 18f
    }

    private val guardPx: Int
        get() = (EDGE_GUARD_DP * resources.displayMetrics.density).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Full screen
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // Consume the touch ONLY if it lands within an edge guard strip.
            return isInGuardZone(event.x, event.y)
        }
        // After an initial DOWN was consumed, keep consuming this gesture so it
        // can't leak to the app underneath; otherwise return false (pass through).
        return isInGuardZone(event.x, event.y)
    }

    private fun isInGuardZone(x: Float, y: Float): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()
        val g = guardPx.toFloat()
        // Left / right edge strips, plus a bottom strip (where the gesture nav
        // bar lives) — this is what blocks accidental back/recents swipes.
        return x <= g || x >= w - g || y >= h - g
    }
}
