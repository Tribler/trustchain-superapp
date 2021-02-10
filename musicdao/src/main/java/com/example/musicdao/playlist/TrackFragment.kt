package com.example.musicdao.playlist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.musicdao.R
import kotlinx.android.synthetic.main.track_row.trackTitle
import kotlinx.android.synthetic.main.track_table_row.*

/**
 * This is one line in a Playlist showing a Track with its ID in the list, title, artist and size
 */
class TrackFragment(
    private val name: String,
    private val index: Int,
    private val release: ReleaseFragment,
    private val size: String,
    private val progress: Int? = 0
) : Fragment(R.layout.track_table_row) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackTitle.text = name
        trackArtist.text = size
        trackId.text = "${index + 1}."
        if (progress != null) progressBar.progress = progress

        contentRow.setOnClickListener {
            release.selectTrackAndPlay(index)
        }
    }

    fun setDownloadProgress(progresss: Int): Boolean {
        if (progressBar == null) return false
        progressBar.progress = progresss
        return true
    }

    fun getProgress(): Int {
        return progress ?: 0
    }
}
