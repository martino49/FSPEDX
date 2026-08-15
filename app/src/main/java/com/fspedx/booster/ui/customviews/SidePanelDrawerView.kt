package com.fspedx.booster.ui.customviews

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.fspedx.booster.R
import com.fspedx.booster.utils.constants.Constants

/**
 * Side panel that slides out from the left edge.
 * Displays live performance and network stats.
 * Users can swipe to open/close.
 */
class SidePanelDrawerView(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    // UI Elements
    private lateinit var tvFps: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvBattery: TextView
    private lateinit var tvPing: TextView
    private lateinit var tvPacketLoss: TextView
    private lateinit var tvNetworkMode: TextView
    private lateinit var tvRam: TextView
    private lateinit var progressRam: ProgressBar
    
    // Toggle switches (using custom views or standard switches)
    private lateinit var toggleRamBooster: View
    private lateinit var toggleDnd: View
    private lateinit var toggleVpn: View
    private lateinit var toggleThermal: View

    // Panel state
    private var isOpen = false
    private val animator = ValueAnimator()
    
    // Constants
    private val panelWidthPx = (context.resources.displayMetrics.widthPixels * 0.75).toInt()

    init {
        // Inflate the panel layout
        val view = LayoutInflater.from(context).inflate(R.layout.view_side_panel, this, true)
        
        // Bind UI elements
        tvFps = findViewById(R.id.tvStatFps)
        tvTemp = findViewById(R.id.tvStatTemp)
        tvBattery = findViewById(R.id.tvStatBattery)
        tvPing = findViewById(R.id.tvStatPing)
        tvPacketLoss = findViewById(R.id.tvStatPacketLoss)
        tvNetworkMode = findViewById(R.id.tvStatNetwork)
        tvRam = findViewById(R.id.tvStatRam)
        progressRam = findViewById(R.id.progressStatRam)
        
        // Toggle views (these are custom layouts that act as switches)
        toggleRamBooster = findViewById(R.id.toggleRamBooster)
        toggleDnd = findViewById(R.id.toggleDnd)
        toggleVpn = findViewById(R.id.toggleVpn)
        toggleThermal = findViewById(R.id.toggleThermal)
        
        // Set initial state (collapsed off-screen)
        translationX = -panelWidthPx.toFloat()
        visibility = View.GONE
        
        // Touch handling for swipe detection is managed by OverlayService
        // This view just renders and handles internal click events
    }

    /**
     * Open the panel with animation.
     */
    fun openPanel() {
        if (isOpen) return
        visibility = View.VISIBLE
        animate()
            .translationX(0f)
            .setDuration(300)
            .start()
        isOpen = true
        updateStats()
    }

    /**
     * Close the panel with animation.
     */
    fun closePanel() {
        if (!isOpen) return
        animate()
            .translationX(-panelWidthPx.toFloat())
            .setDuration(300)
            .withEndAction {
                visibility = View.GONE
            }
            .start()
        isOpen = false
    }

    /**
     * Toggle the panel open/closed.
     */
    fun togglePanel() {
        if (isOpen) {
            closePanel()
        } else {
            openPanel()
        }
    }

    fun isOpen(): Boolean = isOpen

    /**
     * Update all stats displayed in the panel.
     * Called every second by OverlayService. Pulls real values from the system.
     */
    fun updateStats() {
        val ctx = context
        // Real FPS — OverlayService sets this on the service; fall back gracefully.
        val fps = (ctx.applicationContext as com.fspedx.booster.FSPEDXApplication)
            .let { it.prefs.getInt("last_fps", 0) }
        tvFps.text = if (fps > 0) "$fps FPS" else "-- FPS"

        // Real battery temp & level
        val temp = com.fspedx.booster.utils.performance.ThermalUtils.getBatteryTemperature(ctx)
        val battery = com.fspedx.booster.utils.performance.ThermalUtils.getBatteryLevel(ctx)
        tvTemp.text = "${temp.toInt()}°C"
        tvBattery.text = "$battery%"

        // Real ping & packet loss (last known from prefs)
        val ping = ctx.getSharedPreferences(
            com.fspedx.booster.utils.constants.Constants.PREF_NAME,
            android.content.Context.MODE_PRIVATE
        ).getInt(com.fspedx.booster.utils.constants.Constants.KEY_LAST_PING, 0)
        tvPing.text = if (ping > 0) "$ping ms" else "-- ms"
        tvPacketLoss.text = "--"

        // Real network type
        tvNetworkMode.text = com.fspedx.booster.utils.network.NetworkUtils.getNetworkType(ctx)

        // Real RAM stats
        val ramInfo = com.fspedx.booster.utils.performance.RamCleaner.getRamUsage(ctx)
        val usedMB = ramInfo.first
        val totalMB = ramInfo.second
        tvRam.text = "$usedMB / $totalMB MB"
        progressRam.max = totalMB
        progressRam.progress = usedMB

        // Reflect real feature toggle states from prefs
        val prefs = (ctx.applicationContext as com.fspedx.booster.FSPEDXApplication).prefs
        setToggleState(R.id.toggleRamBooster, true) // RAM booster is on when panel visible
        setToggleState(R.id.toggleDnd, prefs.getBoolean(Constants.KEY_DND_MODE, true))
        setToggleState(R.id.toggleVpn, com.fspedx.booster.services.GameVpnService.isRunning())
        setToggleState(R.id.toggleThermal, prefs.getBoolean(Constants.KEY_THERMAL_SENTINEL, true))
    }

    /**
     * Set the state of a toggle in the panel.
     * This should reflect the actual feature state.
     */
    fun setToggleState(toggleId: Int, isChecked: Boolean) {
        when (toggleId) {
            R.id.toggleRamBooster -> {
                toggleRamBooster.isSelected = isChecked
                toggleRamBooster.setBackgroundResource(
                    if (isChecked) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off
                )
            }
            R.id.toggleDnd -> {
                toggleDnd.isSelected = isChecked
                toggleDnd.setBackgroundResource(
                    if (isChecked) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off
                )
            }
            R.id.toggleVpn -> {
                toggleVpn.isSelected = isChecked
                toggleVpn.setBackgroundResource(
                    if (isChecked) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off
                )
            }
            R.id.toggleThermal -> {
                toggleThermal.isSelected = isChecked
                toggleThermal.setBackgroundResource(
                    if (isChecked) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off
                )
            }
        }
    }

    /**
     * Simulate a swipe gesture from the edge.
     * Called by OverlayService when a left-edge swipe is detected.
     */
    fun onSwipeDetected() {
        togglePanel()
    }
}
