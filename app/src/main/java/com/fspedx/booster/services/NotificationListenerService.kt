package com.fspedx.booster.services

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.service.notification.NotificationListenerService as AndroidNotificationListenerService
import android.service.notification.StatusBarNotification
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.R
import com.fspedx.booster.utils.constants.Constants

/**
 * Listens for incoming notifications and silences them while PES is active.
 *
 * Safety rules:
 *  - Phone calls are NEVER muted or cancelled (they use a separate, critical
 *    category and get a full-screen alert regardless of ringer mode).
 *  - Only non-critical notifications (no full-screen intent, not call-related)
 *    are cancelled while PES is in the foreground.
 *  - Ringer mode is only touched while PES is active and restored afterwards,
 *    and only if we changed it (we never force-mute the user permanently).
 */
class NotificationListenerService : AndroidNotificationListenerService() {

    private var isDndEnabled = false
    private var originalRingerMode = AudioManager.RINGER_MODE_NORMAL
    private var weChangedRinger = false

    companion object {
        private var instance: NotificationListenerService? = null

        fun isRunning(): Boolean = instance != null

        fun toggleDnd(context: Context) {
            instance?.toggleDndMode()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Check if DND is enabled in settings
        val prefs = (application as FSPEDXApplication).prefs
        isDndEnabled = prefs.getBoolean(Constants.KEY_DND_MODE, true)

        if (isDndEnabled) {
            enableDnd()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val app = application as FSPEDXApplication
        val notification = sbn.notification ?: return

        // Only show banners while PES is in the foreground.
        if (!app.isPesForeground.value) return

        // Respect the interception-banner pref; if disabled, fall back to the
        // old DND-style behaviour (silence non-critical, never cancel calls).
        val prefs = app.prefs
        val showBanner = prefs.getBoolean(Constants.KEY_INTERCEPTION_BANNER, true)

        val isCall = isCallNotification(sbn)
        val hasFullScreenIntent = notification.fullScreenIntent != null
        val priority = notification.priority
        val isCritical = hasFullScreenIntent ||
                priority == android.app.Notification.PRIORITY_MAX ||
                priority == android.app.Notification.PRIORITY_HIGH

        // Calls: always show a banner (or let the system handle the call UI).
        if (isCall) {
            if (showBanner) {
                OverlayService.showBanner(
                    appName(sbn.packageName),
                    "Incoming call",
                    appIcon(sbn.packageName),
                    isCall = true
                )
            }
            return
        }

        // Non-call, non-critical notifications → floating banner over the game.
        if (showBanner && !isCritical) {
            OverlayService.showBanner(
                appName(sbn.packageName),
                notification.extras?.getString(
                    android.app.Notification.EXTRA_TEXT
                ) ?: "New notification",
                appIcon(sbn.packageName),
                isCall = false
            )
            return
        }

        // DND fallback (banner disabled): silence non-critical, never cancel calls.
        if (!isDndEnabled) return
        if (isCritical) return

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(sbn.packageName, sbn.id)
        } catch (e: Exception) {
            // Ignore
        }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            originalRingerMode = audioManager.ringerMode
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            weChangedRinger = true
        }
    }

    /** Best-effort app label for a package. */
    private fun appName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /** Best-effort app icon resource id for a package. */
    private fun appIcon(packageName: String): Int {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appInfo.icon
        } catch (e: Exception) {
            R.drawable.ic_app_placeholder
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Restore ringer if PES is no longer running
        val app = application as FSPEDXApplication
        if (!app.isPesForeground.value) {
            restoreRinger()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        restoreRinger()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        restoreRinger()
    }

    private fun enableDnd() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalRingerMode = audioManager.ringerMode
        // We do NOT mute the ringer here permanently. Muting happens only while
        // PES is active, per-notification, in onNotificationPosted.
        weChangedRinger = false
    }

    private fun restoreRinger() {
        if (!weChangedRinger) return
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != originalRingerMode) {
            audioManager.ringerMode = originalRingerMode
        }
        weChangedRinger = false
    }

    private fun toggleDndMode() {
        isDndEnabled = !isDndEnabled

        val prefs = (application as FSPEDXApplication).prefs
        prefs.edit().putBoolean(Constants.KEY_DND_MODE, isDndEnabled).apply()

        if (isDndEnabled) {
            enableDnd()
        } else {
            restoreRinger()
        }
    }

    /**
     * Check if the notification is from a phone call.
     * Calls are always allowed through.
     */
    private fun isCallNotification(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName
        val category = sbn.notification?.category
        // Notification category CALL is authoritative on modern Android
        if (category == android.app.Notification.CATEGORY_CALL) return true
        return packageName == "com.android.dialer" ||
                packageName == "com.android.incallui" ||
                packageName == "com.google.android.dialer"
    }
}
