package com.fspedx.booster.ui.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.fspedx.booster.R
import com.fspedx.booster.utils.constants.Constants

/**
 * Draggable floating pill that sits on top of games.
 * Contains Boost and Record quick-action buttons.
 * Managed by OverlayService.
 */
@SuppressLint("ViewConstructor")
class FloatingPillView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // Buttons
    private lateinit var btnBoost: ImageButton
    private lateinit var btnRecord: ImageButton
    
    // Callbacks
    var onBoostClick: (() -> Unit)? = null
    var onRecordClick: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    // Dragging state — for WindowManager overlays we must update LayoutParams,
    // so we report drag deltas to the host (OverlayService) via this callback.
    var onDragMove: ((Float, Float) -> Unit)? = null

    // Dragging state
    private var dX = 0f
    private var dY = 0f
    private var isDragging = false
    private val touchSlop = 10f // Minimum movement to detect drag

    init {
        inflate(context, R.layout.view_floating_pill, this)
        
        btnBoost = findViewById(R.id.btnPillBoost)
        btnRecord = findViewById(R.id.btnPillRecord)
        
        // Close button (X) to dismiss the pill
        val btnClose = findViewById<ImageButton>(R.id.btnPillClose)
        
        btnBoost.setOnClickListener {
            onBoostClick?.invoke()
        }
        
        btnRecord.setOnClickListener {
            onRecordClick?.invoke()
        }
        
        btnClose.setOnClickListener {
            onDismiss?.invoke()
        }
        
        // Make the whole view draggable
        setOnTouchListener { _, event ->
            handleTouch(event)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouch(event: MotionEvent): Boolean {
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
                    // Report the drag delta to the host so it can update the
                    // WindowManager.LayoutParams (required for overlay views).
                    onDragMove?.invoke(deltaX, deltaY)
                    dX = event.rawX - x
                    dY = event.rawY - y
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

    /**
     * Set the state of the record button (Recording / Not Recording).
     */
    fun setRecordingState(isRecording: Boolean) {
        if (isRecording) {
            btnRecord.setImageResource(R.drawable.ic_stop)
            btnRecord.setColorFilter(context.getColor(R.color.danger_red))
        } else {
            btnRecord.setImageResource(R.drawable.ic_record)
            btnRecord.setColorFilter(context.getColor(R.color.primary_cyan))
        }
    }
}
