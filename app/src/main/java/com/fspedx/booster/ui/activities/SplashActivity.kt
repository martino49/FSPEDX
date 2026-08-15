package com.fspedx.booster.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.fspedx.booster.FSPEDXApplication
import com.fspedx.booster.R
import com.fspedx.booster.utils.constants.Constants

class SplashActivity : AppCompatActivity() {

    private val splashDuration = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        // Theme is applied via the manifest (Theme.FSPEDX.Splash) — no setTheme needed.
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Post delayed navigation
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, splashDuration)
    }

    private fun navigateToNextScreen() {
        val prefs = (application as FSPEDXApplication).prefs
        val isFirstRun = prefs.getBoolean(Constants.KEY_FIRST_RUN, true)

        val intent = if (isFirstRun) {
            // First time user -> Show Onboarding
            Intent(this, OnboardingActivity::class.java)
        } else {
            // Returning user -> Go straight to Main
            Intent(this, MainActivity::class.java)
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
