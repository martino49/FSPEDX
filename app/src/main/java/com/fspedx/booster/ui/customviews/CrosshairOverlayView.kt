package com.fspedx.booster.ui.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.fspedx.booster.R

/**
 * Aiming crosshair overlay that can be dragged anywhere on the screen.
 * Useful for aiming passes or shots in PES.
 * Renders a neon green/grey crosshair with a central dot.
 */
class CrosshairOverlayView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val CROSSHAIR_SIZE_DP = 40f
        private const val GAP_SIZE_DP = 8f
        private const val DOT_RADIUS_DP = 4f
        private const val LINE_WIDTH_DP = 2f
    }

    // Paint objects
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(LINE_WIDTH_DP)
        color = ContextCompat.getColor(context, R.color.primary_cyan)
    }

    private val dotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.primary_cyan)
    }

    private val shadowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(LINE_WIDTH_DP)
        color = Color.BLACK
        alpha = 100
    }

    // Dimensions
    private var crosshairSize = dpToPx(CROSSHAIR_SIZE_DP)
    private var gapSize = dpToPx(GAP_SIZE_DP)
    private var dotRadius = dpToPx(DOT_RADIUS_DP)
    
    // Drag state
    private var dX = 0f
    private var dY = 0f
    private var isDragging = false
    private val touchSlop = 10f

    // WindowManager overlays must be moved via LayoutParams — report deltas to host.
    var onDragMove: ((Float, Float) -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // For transparency
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val cx = width / 2f
        val cy = height / 2f
        
        // Draw shadow (offset slightly for depth)
        canvas.save()
        canvas.translate(2f, 2f)
        drawCrosshair(canvas, cx, cy, shadowPaint)
        canvas.restore()
        
        // Draw main crosshair
        drawCrosshair(canvas, cx, cy, paint)
        
        // Draw center dot
        canvas.drawCircle(cx, cy, dotRadius, dotPaint)
    }

    private fun drawCrosshair(canvas: Canvas, cx: Float, cy: Float, paint: Paint) {
        // Top line
        canvas.drawLine(cx, cy - gapSize, cx, cy - gapSize - crosshairSize, paint)
        // Bottom line
        canvas.drawLine(cx, cy + gapSize, cx, cy + gapSize + crosshairSize, paint)
        // Left line
        canvas.drawLine(cx - gapSize, cy, cx - gapSize - crosshairSize, cy, paint)
        // Right line
        canvas.drawLine(cx + gapSize, cy, cx + gapSize + crosshairSize, cy, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dX = event.rawX - x
                dY = event.rawY - y
                isDragging = false
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - (x + dX)
                val deltaY = event.rawY - (y + dY)
                
                if (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop) {
                    isDragging = true
                }
                
                if (isDragging) {
                    // Report the drag delta so the host can update the
                    // WindowManager.LayoutParams (overlay views can't set x/y).
                    onDragMove?.invoke(deltaX, deltaY)
                    dX = event.rawX - x
                    dY = event.rawY - y
                    invalidate()
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    performClick()
                }
                isDragging = false
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
