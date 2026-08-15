package com.fspedx.booster.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fspedx.booster.R
import com.fspedx.booster.ui.adapters.ReplayGridAdapter
import com.fspedx.booster.utils.constants.Constants
import java.io.File

class GalleryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.galleryRecyclerView)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        loadRecordings()
    }

    override fun onResume() {
        super.onResume()
        // Refresh when the user returns (e.g. after recording a new session)
        loadRecordings()
    }

    private fun loadRecordings() {
        // Prefer scoped storage; fall back to legacy public path.
        val recordingsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            ?.resolve(Constants.RECORDINGS_DIR)

        val videoFiles = recordingsDir?.listFiles { file ->
            file.extension in listOf("mp4", "mkv", "3gp")
        } ?: emptyArray()

        if (videoFiles.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            val adapter = ReplayGridAdapter(
                videoFiles.sortedByDescending { it.lastModified() },
                onItemClick = { playRecording(it) }
            )
            recyclerView.adapter = adapter
        }
    }

    private fun playRecording(file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(file), "video/mp4")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                requireContext(),
                "No video player found",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
