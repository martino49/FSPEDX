package com.fspedx.booster.ui.adapters

import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fspedx.booster.R
import java.io.File

/**
 * Adapter for displaying recorded session videos in a grid layout.
 * Loads a real video frame as the thumbnail and shows duration.
 */
class ReplayGridAdapter(
    private val files: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<ReplayGridAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_replay_thumbnail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]

        // Remove file extension for display
        val fileName = file.name.replace(".mp4", "").replace(".mkv", "").replace(".3gp", "")
        holder.tvFileName.text = fileName

        // Load a real thumbnail frame from the video
        holder.thumbnail.setImageResource(R.drawable.ic_video_placeholder) // fallback
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            retriever.release()

            if (frame != null) {
                holder.thumbnail.setImageBitmap(frame)
            }
            if (durationMs > 0) {
                holder.tvDuration.text = formatDuration(durationMs)
                holder.tvDuration.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            holder.tvDuration.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(file)
        }
    }

    override fun getItemCount(): Int = files.size

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%d:%02d", min, sec)
    }
}
