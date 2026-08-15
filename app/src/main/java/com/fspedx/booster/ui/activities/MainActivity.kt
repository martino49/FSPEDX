package com.fspedx.booster.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fspedx.booster.R
import com.fspedx.booster.ui.fragments.AnalyticsFragment
import com.fspedx.booster.ui.fragments.FeaturesFragment
import com.fspedx.booster.ui.fragments.GalleryFragment
import com.fspedx.booster.ui.fragments.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    private val homeFragment = HomeFragment()
    private val featuresFragment = FeaturesFragment()
    private val analyticsFragment = AnalyticsFragment()
    private val galleryFragment = GalleryFragment()

    private val fragmentMap = mapOf(
        R.id.nav_home to homeFragment,
        R.id.nav_features to featuresFragment,
        R.id.nav_analytics to analyticsFragment,
        R.id.nav_gallery to galleryFragment
    )

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNavigation)

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(homeFragment)
        }

        // Bottom navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            val fragment = fragmentMap[item.itemId]
            if (fragment != null && fragment != currentFragment) {
                loadFragment(fragment)
                true
            } else {
                false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        currentFragment = fragment
    }
}
