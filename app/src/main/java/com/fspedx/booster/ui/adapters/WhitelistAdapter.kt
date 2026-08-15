package com.fspedx.booster.ui.adapters

import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fspedx.booster.R

/**
 * Adapter for displaying the list of apps protected from RAM killing.
 * Shows real app icon, name, and a remove button.
 */
class WhitelistAdapter(
    private val context: Context,
    private val packageNames: MutableSet<String>,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<WhitelistAdapter.ViewHolder>() {

    private val packageManager: PackageManager = context.packageManager

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvAppName)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_whitelist_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val packageName = packageNames.elementAt(position)

        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            holder.ivIcon.setImageDrawable(appInfo.loadIcon(packageManager))
            holder.tvName.text = packageManager.getApplicationLabel(appInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            // App uninstalled — still show it so the user can remove the stale entry
            holder.ivIcon.setImageResource(R.drawable.ic_app_placeholder)
            holder.tvName.text = packageName
        }

        holder.btnRemove.setOnClickListener {
            onRemove(packageName)
        }
    }

    override fun getItemCount(): Int = packageNames.size

    /**
     * Update the dataset with a new set.
     */
    fun updateData(newSet: Set<String>) {
        packageNames.clear()
        packageNames.addAll(newSet)
        notifyDataSetChanged()
    }
}
