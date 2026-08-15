package com.fspedx.booster.ui.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.fspedx.booster.R

/**
 * Adapter for the 4-step onboarding permission wizard.
 * Each step explains a permission and guides the user to enable it.
 */
class OnboardingPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return OnboardingStepFragment.newInstance(position)
    }

    /**
     * Inner Fragment for each onboarding step.
     * Uses position to determine which step to display.
     */
    class OnboardingStepFragment : Fragment() {

        companion object {
            private const val ARG_POSITION = "position"

            fun newInstance(position: Int): OnboardingStepFragment {
                val fragment = OnboardingStepFragment()
                val args = Bundle()
                args.putInt(ARG_POSITION, position)
                fragment.arguments = args
                return fragment
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val position = arguments?.getInt(ARG_POSITION) ?: 0
            val view = inflater.inflate(R.layout.item_onboarding_step, container, false)

            val ivIcon = view.findViewById<ImageView>(R.id.ivStepIcon)
            val tvTitle = view.findViewById<TextView>(R.id.tvStepTitle)
            val tvDesc = view.findViewById<TextView>(R.id.tvStepDescription)

            when (position) {
                0 -> {
                    ivIcon.setImageResource(R.drawable.ic_visibility)
                    tvTitle.text = "Show Live FPS & Temp"
                    tvDesc.text = "Allow FSPEDX to draw over other apps so you can see your frame rate and battery temperature while playing PES."
                }
                1 -> {
                    ivIcon.setImageResource(R.drawable.ic_touch_app)
                    tvTitle.text = "Record Taps for Heatmaps"
                    tvDesc.text = "Enable Accessibility so FSPEDX can capture where you tap on the screen. This data generates heatmaps and suggests better button positions."
                }
                2 -> {
                    ivIcon.setImageResource(R.drawable.ic_vpn_lock)
                    tvTitle.text = "Zero-Lag Network Tunnel"
                    tvDesc.text = "Setup the VPN tunnel to route only PES traffic through our optimized path. This reduces ping and prioritizes your game packets."
                }
                3 -> {
                    ivIcon.setImageResource(R.drawable.ic_do_not_disturb)
                    tvTitle.text = "Block Calls & Notifications"
                    tvDesc.text = "Allow DND mode so you never get interrupted by WhatsApp calls or notifications during a crucial match moment."
                }
            }

            return view
        }
    }
}
