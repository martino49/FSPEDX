package com.fspedx.booster.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.R
import com.fspedx.booster.ui.activities.MainActivity
import com.fspedx.booster.ui.customviews.CrosshairOverlayView
import com.fspedx.booster.ui.customviews.FloatingPillView
import com.fspedx.booster.ui.customviews.SidePanelDrawerView
import com.fspedx.booster.utils.constants.Constants
import com.fspedx.booster.utils.performance.ThermalUtils

/**
 * Service that manages all system overlays:
 * - FPS / Temperature counter (top-right corner)
 * - Floating Quick-Pill (draggable, right edge)
 * - Aiming Crosshair (draggable, anywhere)
 * - Side Pull-Out Table (swipe from left)
 */
class OverlayService : android.app.Service() {

    private lateinit var windowManager: WindowManager
    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // Overlay views
    private var fpsView: View? = null
    private var floatingPill: FloatingPillView? = null
    private var floatingPillParams: WindowManager.LayoutParams? = null
    private var crosshair: CrosshairOverlayView? = null
    private var crosshairParams: WindowManager.LayoutParams? = null
    private var sidePanel: SidePanelDrawerView? = null
    private var edgeGuard: View? = null
    private var bannerView: View? = null
    private var bannerTimer: Runnable? = null

    // FPS counter
    private var frameCount = 0
    private var lastFpsUpdate = 0L
    private var currentFps = 0
    private var choreographer: android.view.Choreographer? = null
    private val frameCallback = object : android.view.Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (isRunning) {
                onFrame()
                choreographer?.postFrameCallback(this)
            }
        }
    }

    companion object {
        private var instance: OverlayService? = null

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }

        fun isRunning(): Boolean = instance != null

        fun showFloatingPill(context: Context) {
            instance?.showPill(true)
        }

        fun hideFloatingPill(context: Context) {
            instance?.showPill(false)
        }

        fun showCrosshair(context: Context) {
            instance?.showCrosshair(true)
        }

        fun hideCrosshair(context: Context) {
            instance?.showCrosshair(false)
        }

        fun toggleSidePanel() {
            instance?.sidePanel?.togglePanel()
        }

        fun setEdgeBlocking(enabled: Boolean) {
            val svc = instance ?: return
            if (enabled) {
                svc.setupEdgeGuard()
            } else {
                svc.removeEdgeGuard()
            }
        }

        /**
         * Show a floating banner over the game (notification/call) without
         * closing the game. The game stays in the foreground.
         */
        fun showBanner(title: String, text: String, iconRes: Int, isCall: Boolean) {
            val svc = instance ?: return
            svc.showInterceptionBanner(title, text, iconRes, isCall)
        }

        fun dismissBanner() {
            val svc = instance ?: return
            svc.dismissInterceptionBanner()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        isRunning = true
        startForeground(Constants.NOTIFICATION_ID_OVERLAY, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle incoming actions
        intent?.action?.let { action ->
            when (action) {
                Constants.ACTION_TOGGLE_PILL -> {
                    floatingPill?.let {
                        if (it.visibility == View.VISIBLE) {
                            it.visibility = View.GONE
                        } else {
                            it.visibility = View.VISIBLE
                        }
                    }
                }
                else -> { /* no-op for unknown actions */ }
            }
        }
        
        // Start overlays if not already running
        if (fpsView == null) {
            setupFpsOverlay()
        }
        
        val prefs = (application as FSPEDXApplication).prefs
        if (prefs.getBoolean(Constants.KEY_FLOATING_PILL, true)) {
            setupFloatingPill()
        }
        if (prefs.getBoolean(Constants.KEY_CROSSHAIR, false)) {
            setupCrosshair()
        }
        if (prefs.getBoolean(Constants.KEY_SIDE_PANEL, true)) {
            setupSidePanel()
        }
        if (prefs.getBoolean(Constants.KEY_EDGE_BLOCKING, false)) {
            setupEdgeGuard()
        }

        startUpdatingStats()
        startFpsMonitoring()

        return START_STICKY
    }

    private fun startFpsMonitoring() {
        if (choreographer == null) {
            choreographer = android.view.Choreographer.getInstance()
        }
        choreographer?.postFrameCallback(frameCallback)
    }

    // ============ FPS OVERLAY ============

    private fun setupFpsOverlay() {
        if (fpsView != null) return
        if (!(application as FSPEDXApplication).prefs.getBoolean(Constants.KEY_FPS_OVERLAY, true)) return

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        fpsView = inflater.inflate(R.layout.view_fps_overlay, null)
        val tvFps = fpsView?.findViewById<TextView>(R.id.tvFps)
        val tvTemp = fpsView?.findViewById<TextView>(R.id.tvTemp)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 80
        }

        windowManager.addView(fpsView, params)
    }

    // ============ FLOATING PILL ============

    private fun setupFloatingPill() {
        if (floatingPill != null) return

        floatingPill = FloatingPillView(this).apply {
            onBoostClick = {
                // Trigger RAM boost on a background thread, then report the result
                Thread {
                    val prefs = (application as FSPEDXApplication).prefs
                    val whitelist = prefs.getStringSet(Constants.KEY_WHITELIST_APPS, emptySet())
                        ?: emptySet()
                    val freedMB = com.fspedx.booster.utils.performance.RamCleaner
                        .forceClean(this@OverlayService, whitelist)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(
                            this@OverlayService,
                            "RAM Boost: +$freedMB MB freed",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }.start()
            }
            onRecordClick = {
                // Toggle recording. Starting requires MediaProjection permission
                // which must be granted from an Activity (HomeFragment).
                if (SessionRecorderService.isRunning()) {
                    SessionRecorderService.stop(this@OverlayService)
                    setRecordingState(false)
                } else {
                    android.widget.Toast.makeText(
                        this@OverlayService,
                        "Start recording from the Home screen",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            onDismiss = {
                floatingPill?.visibility = View.GONE
            }
            onDragMove = { dx, dy ->
                updateOverlayPosition(floatingPillParams, dx, dy)
            }
        }

        floatingPillParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 160
        }

        windowManager.addView(floatingPill, floatingPillParams)
    }

    /**
     * Move a WindowManager overlay by (dx, dy) and clamp to screen bounds.
     * Overlay views must be moved by updating their LayoutParams.
     */
    private fun updateOverlayPosition(params: WindowManager.LayoutParams?, dx: Float, dy: Float) {
        val p = params ?: return
        val metrics = resources.displayMetrics
        p.x = (p.x + dx).toInt().coerceIn(0, metrics.widthPixels - 100)
        p.y = (p.y + dy).toInt().coerceIn(0, metrics.heightPixels - 100)
        try {
            windowManager.updateViewLayout(floatingPill, p)
        } catch (e: Exception) {
            // View may have been removed
        }
    }

    private fun showPill(show: Boolean) {
        floatingPill?.visibility = if (show) View.VISIBLE else View.GONE
    }

    // ============ CROSSHAIR ============

    private fun setupCrosshair() {
        if (crosshair != null) return

        crosshair = CrosshairOverlayView(this)
        val crossSize = (80 * resources.displayMetrics.density).toInt()
        crosshairParams = WindowManager.LayoutParams(
            crossSize,
            crossSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        crosshair?.onDragMove = { dx, dy ->
            updateCrosshairPosition(dx, dy)
        }

        windowManager.addView(crosshair, crosshairParams)
    }

    private fun updateCrosshairPosition(dx: Float, dy: Float) {
        val params = crosshairParams ?: return
        val metrics = resources.displayMetrics
        params.x = (params.x + dx).toInt().coerceIn(-metrics.widthPixels, metrics.widthPixels)
        params.y = (params.y + dy).toInt().coerceIn(-metrics.heightPixels, metrics.heightPixels)
        try {
            windowManager.updateViewLayout(crosshair, params)
        } catch (e: Exception) {
            // View may have been removed
        }
    }

    private fun showCrosshair(show: Boolean) {
        crosshair?.visibility = if (show) View.VISIBLE else View.GONE
    }

    // ============ SIDE PANEL ============

    private fun setupSidePanel() {
        if (sidePanel != null) return

        sidePanel = SidePanelDrawerView(this)

        val panelWidth = (resources.displayMetrics.widthPixels * 0.75).toInt()
        val params = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = -panelWidth // Hidden off-screen initially
            y = 0
        }

        windowManager.addView(sidePanel, params)
        // Start closed
        sidePanel?.closePanel()
    }

    // ============ EDGE GUARD (Gesture/Edge Blocking) ============

    private fun setupEdgeGuard() {
        if (edgeGuard != null) return

        edgeGuard = com.fspedx.booster.ui.customviews.EdgeGuardView(this)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        windowManager.addView(edgeGuard, params)
    }

    private fun removeEdgeGuard() {
        try {
            edgeGuard?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            // Already removed
        }
        edgeGuard = null
    }

    // ============ INTERCEPTION BANNER ============

    /**
     * Shows a translucent floating banner at the top of the screen OVER the
     * game. The game is NOT closed or paused by us; the banner is just an
     * overlay that can be dismissed. Auto-dismisses after 5s unless it's a call.
     */
    private fun showInterceptionBanner(title: String, text: String, iconRes: Int, isCall: Boolean) {
        try {
            // Only meaningful when the overlay service is running and overlay
            // permission is held.
            if (!android.provider.Settings.canDrawOverlays(this)) return
            if (bannerView != null) dismissInterceptionBanner()

            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            bannerView = inflater.inflate(R.layout.view_interception_banner, null)

            bannerView?.findViewById<TextView>(R.id.bannerTitle)?.text = title
            bannerView?.findViewById<TextView>(R.id.bannerText)?.text = text
            bannerView?.findViewById<ImageView>(R.id.bannerIcon)?.setImageResource(iconRes)
            bannerView?.findViewById<TextView>(R.id.bannerTime)?.text =
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())

            // Close button
            bannerView?.findViewById<ImageView>(R.id.bannerClose)?.setOnClickListener {
                dismissInterceptionBanner()
            }

            // Call actions: route to the system call UI (we cannot answer calls)
            if (isCall) {
                val actions = bannerView?.findViewById<android.view.View>(R.id.bannerActions)
                actions?.visibility = View.VISIBLE
                bannerView?.findViewById<com.google.android.material.button.MaterialButton>(
                    R.id.bannerBtnPrimary
                )?.text = "Open"
                bannerView?.findViewById<com.google.android.material.button.MaterialButton>(
                    R.id.bannerBtnPrimary
                )?.setOnClickListener {
                    dismissInterceptionBanner()
                    openSystemCallUi()
                }
                bannerView?.findViewById<com.google.android.material.button.MaterialButton>(
                    R.id.bannerBtnSecondary
                )?.setOnClickListener {
                    dismissInterceptionBanner()
                }
            }

            val widthPx = (resources.displayMetrics.widthPixels * 0.92).toInt()
            val params = WindowManager.LayoutParams(
                widthPx,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (16 * resources.displayMetrics.density).toInt()
            }

            windowManager.addView(bannerView, params)

            // Auto-dismiss non-call banners after 5s
            if (!isCall) {
                bannerTimer = Runnable {
                    dismissInterceptionBanner()
                }
                mainHandler.postDelayed(bannerTimer!!, 5000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissInterceptionBanner() {
        bannerTimer?.let { mainHandler.removeCallbacks(it) }
        bannerTimer = null
        try {
            bannerView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            // Already removed
        }
        bannerView = null
    }

    private fun openSystemCallUi() {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // No dialer found
        }
    }

    // ============ STATS UPDATES ============

    private fun startUpdatingStats() {
        val runnable = object : Runnable {
            override fun run() {
                updateStats()
                if (isRunning) {
                    mainHandler.postDelayed(this, Constants.OVERLAY_UPDATE_INTERVAL_MS)
                }
            }
        }
        mainHandler.post(runnable)
    }

    private fun updateStats() {
        // Update FPS counter
        val tvFps = fpsView?.findViewById<TextView>(R.id.tvFps)
        val tvTemp = fpsView?.findViewById<TextView>(R.id.tvTemp)

        tvFps?.text = "$currentFps FPS"
        // Persist real FPS so the side panel can read it
        try {
            (application as FSPEDXApplication).prefs.edit()
                .putInt("last_fps", currentFps)
                .apply()
        } catch (e: Exception) {
            // Ignore
        }

        val temp = ThermalUtils.getBatteryTemperature(this)
        tvTemp?.text = "${temp.toInt()}°C"

        // Update side panel if open
        if (sidePanel?.isOpen() == true) {
            sidePanel?.updateStats()
        }
    }

    // Called from Choreographer on every frame to measure real FPS
    fun onFrame() {
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsUpdate >= 1000) {
            currentFps = frameCount
            frameCount = 0
            lastFpsUpdate = now
        }
    }

    // ============ OVERRIDES ============

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        choreographer?.removeFrameCallback(frameCallback)
        choreographer = null
        mainHandler.removeCallbacksAndMessages(null)

        // Remove all views
        try {
            fpsView?.let { windowManager.removeView(it) }
            floatingPill?.let { windowManager.removeView(it) }
            crosshair?.let { windowManager.removeView(it) }
            sidePanel?.let { windowManager.removeView(it) }
            removeEdgeGuard()
            dismissInterceptionBanner()
        } catch (e: Exception) {
            // View already removed
        }

        fpsView = null
        floatingPill = null
        crosshair = null
        sidePanel = null
        instance = null
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ============ NOTIFICATIONS ============

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_OVERLAY)
            .setContentTitle("FSPEDX Active")
            .setContentText("Optimizing PES performance")
            .setSmallIcon(R.drawable.ic_speed)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_OVERLAY,
                "FSPEDX Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when FSPEDX overlays are active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
