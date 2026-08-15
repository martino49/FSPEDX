package com.fspedx.booster.utils.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.fragment.app.Fragment

/**
 * Helper class for managing the 4 dangerous permissions:
 * 1. Overlay (SYSTEM_ALERT_WINDOW)
 * 2. Accessibility (BIND_ACCESSIBILITY_SERVICE)
 * 3. VPN (BIND_VPN_SERVICE)
 * 4. DND (ACCESS_NOTIFICATION_POLICY)
 */
object PermissionHelper {

    // ============ OVERLAY PERMISSION ============

    /**
     * Check if overlay permission is granted.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Request overlay permission.
     * Opens system settings.
     */
    fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${activity.packageName}")
        )
        activity.startActivity(intent)
    }

    /**
     * Request overlay permission from fragment.
     */
    fun requestOverlayPermission(fragment: Fragment) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${fragment.requireContext().packageName}")
        )
        fragment.startActivity(intent)
    }

    // ============ ACCESSIBILITY PERMISSION ============

    /**
     * Check if accessibility service is enabled for this app.
     */
    fun hasAccessibilityPermission(context: Context): Boolean {
        val service = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return service?.contains(context.packageName) == true
    }

    /**
     * Request accessibility permission.
     * Opens system accessibility settings.
     */
    fun requestAccessibilityPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        activity.startActivity(intent)
    }

    /**
     * Request accessibility permission from fragment.
     */
    fun requestAccessibilityPermission(fragment: Fragment) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        fragment.startActivity(intent)
    }

    // ============ VPN PERMISSION ============

    /**
     * Check if VPN permission is granted.
     * This is done by preparing the VPN service.
     * If prepare() returns null, permission is granted.
     */
    fun hasVpnPermission(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    /**
     * Request VPN permission.
     * This will show the system VPN permission dialog.
     * @return The intent to launch, or null if already granted.
     */
    fun requestVpnPermission(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Request VPN permission from fragment using the modern Activity Result API.
     * The caller should register an ActivityResultLauncher and pass its
     * launch call here.
     */
    fun requestVpnPermission(fragment: Fragment, onLaunch: (android.content.Intent) -> Unit) {
        val intent = VpnService.prepare(fragment.requireContext())
        if (intent != null) {
            onLaunch(intent)
        }
    }

    // ============ DND PERMISSION ============

    /**
     * Check if DND (Notification Policy) permission is granted.
     */
    fun hasDndPermission(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true // Older versions don't have this restriction
        }
    }

    /**
     * Request DND permission.
     * Opens system notification policy settings.
     */
    fun requestDndPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        activity.startActivity(intent)
    }

    /**
     * Request DND permission from fragment.
     */
    fun requestDndPermission(fragment: Fragment) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        fragment.startActivity(intent)
    }

    // ============ WRITE_SETTINGS PERMISSION ============

    /**
     * Check if WRITE_SETTINGS permission is granted.
     * Needed for the Refresh Rate Switcher and GPU rendering tuning.
     */
    fun hasWriteSettingsPermission(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Request WRITE_SETTINGS permission from an activity.
     * Opens the "Modify system settings" screen for this app.
     */
    fun requestWriteSettingsPermission(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            android.net.Uri.parse("package:${activity.packageName}")
        )
        activity.startActivity(intent)
    }

    /**
     * Request WRITE_SETTINGS permission from a fragment.
     */
    fun requestWriteSettingsPermission(fragment: Fragment) {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            android.net.Uri.parse("package:${fragment.requireContext().packageName}")
        )
        fragment.startActivity(intent)
    }

    // ============ ALL PERMISSIONS CHECK ============

    /**
     * Check if all 4 permissions are granted.
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasOverlayPermission(context) &&
                hasAccessibilityPermission(context) &&
                hasVpnPermission(context) &&
                hasDndPermission(context)
    }

    /**
     * Get a map of all permission statuses.
     */
    fun getPermissionStatus(context: Context): Map<String, Boolean> {
        return mapOf(
            "overlay" to hasOverlayPermission(context),
            "accessibility" to hasAccessibilityPermission(context),
            "vpn" to hasVpnPermission(context),
            "dnd" to hasDndPermission(context)
        )
    }
}
