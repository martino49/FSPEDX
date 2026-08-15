package com.fspedx.booster.ui.activities

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.R
import com.fspedx.booster.ui.adapters.OnboardingPagerAdapter
import com.fspedx.booster.utils.constants.Constants
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: MaterialButton
    private lateinit var btnFinish: MaterialButton

    // Permission request launchers
    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkAllPermissions() }

    private val accessibilityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkAllPermissions() }

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // After the VPN consent dialog, mark VPN granted and re-check
        prefs.edit().putBoolean(Constants.KEY_VPN_GRANTED, true).apply()
        checkAllPermissions()
    }

    private val dndLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkAllPermissions() }

    private val prefs by lazy { (application as FSPEDXApplication).prefs }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btnNext)
        btnFinish = findViewById(R.id.btnFinish)

        // Set up ViewPager with 4 steps
        val adapter = OnboardingPagerAdapter(this)
        viewPager.adapter = adapter

        // Connect TabLayout with ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.setIcon(R.drawable.ic_visibility)
                1 -> tab.setIcon(R.drawable.ic_touch_app)
                2 -> tab.setIcon(R.drawable.ic_vpn_lock)
                3 -> tab.setIcon(R.drawable.ic_do_not_disturb)
            }
        }.attach()

        btnFinish.visibility = View.GONE

        // Next button: request the current permission, or advance if already granted.
        btnNext.setOnClickListener {
            when (viewPager.currentItem) {
                0 -> requestOverlayPermission()
                1 -> requestAccessibilityPermission()
                2 -> requestVpnPermission()
                3 -> requestDndPermission()
            }
        }

        btnFinish.setOnClickListener {
            completeOnboarding()
        }

        checkAllPermissions()
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when user returns from system settings
        checkAllPermissions()
    }

    private fun checkAllPermissions() {
        val overlayGranted = Settings.canDrawOverlays(this)
        val accessibilityGranted = isAccessibilityEnabled()
        val vpnGranted = isVpnEnabled()
        val dndGranted = isDndEnabled()

        // Save per-permission status so a reopened app doesn't re-ask
        prefs.edit().apply {
            putBoolean(Constants.KEY_OVERLAY_GRANTED, overlayGranted)
            putBoolean(Constants.KEY_ACCESSIBILITY_GRANTED, accessibilityGranted)
            putBoolean(Constants.KEY_VPN_GRANTED, vpnGranted)
            putBoolean(Constants.KEY_DND_GRANTED, dndGranted)
            apply()
        }

        val currentStep = viewPager.currentItem
        val currentGranted = when (currentStep) {
            0 -> overlayGranted
            1 -> accessibilityGranted
            2 -> vpnGranted
            3 -> dndGranted
            else -> false
        }

        // Show "Next"/"Finish" appropriately
        if (overlayGranted && accessibilityGranted && vpnGranted && dndGranted) {
            btnNext.visibility = View.GONE
            btnFinish.visibility = View.VISIBLE
        } else {
            btnNext.visibility = View.VISIBLE
            btnFinish.visibility = View.GONE
            btnNext.text = if (currentGranted) "Next" else "Allow"
        }

        // Auto-advance to the next permission if the current one is already granted
        if (currentGranted && !(overlayGranted && accessibilityGranted && vpnGranted && dndGranted)) {
            viewPager.post {
                if (viewPager.currentItem == currentStep) {
                    viewPager.setCurrentItem((currentStep + 1).coerceAtMost(3), true)
                }
            }
        }
    }

    // Permission request methods
    private fun requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            advance()
            return
        }
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:$packageName")
        )
        overlayLauncher.launch(intent)
    }

    private fun requestAccessibilityPermission() {
        if (isAccessibilityEnabled()) {
            advance()
            return
        }
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        accessibilityLauncher.launch(intent)
    }

    private fun requestVpnPermission() {
        if (isVpnEnabled()) {
            advance()
            return
        }
        // For VPN, we need to actually start the VPN service consent intent
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            // Already prepared — consent not needed
            prefs.edit().putBoolean(Constants.KEY_VPN_GRANTED, true).apply()
            advance()
        }
    }

    private fun requestDndPermission() {
        if (isDndEnabled()) {
            advance()
            return
        }
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        dndLauncher.launch(intent)
    }

    /** Move to the next page after a permission is granted. */
    private fun advance() {
        val next = (viewPager.currentItem + 1).coerceAtMost(3)
        viewPager.setCurrentItem(next, true)
        checkAllPermissions()
    }

    // Helper checks
    private fun isAccessibilityEnabled(): Boolean {
        val service = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return service?.contains(packageName) == true
    }

    private fun isVpnEnabled(): Boolean {
        // Real check: if VpnService.prepare() returns null, consent is already granted.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            VpnService.prepare(this) == null
        } else {
            false
        }
    }

    private fun isDndEnabled(): Boolean {
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager?.isNotificationPolicyAccessGranted == true
        } else {
            true // Older Android versions don't have this restriction
        }
    }

    private fun completeOnboarding() {
        prefs.edit().apply {
            putBoolean(Constants.KEY_FIRST_RUN, false)
            putBoolean(Constants.KEY_ONBOARDING_COMPLETE, true)
            apply()
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
