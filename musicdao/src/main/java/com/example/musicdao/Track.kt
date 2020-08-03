package com.example.musicdao

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.track_row.trackTitle
import kotlinx.android.synthetic.main.track_table_row.*

class Track(
    private val name: String,
    private val index: Int,
    private val release: Release,
    private val size: String
) : Fragment(R.layout.track_table_row) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackTitle.text = name
        trackArtist.text = size

        contentRow.setOnClickListener {
            release.selectTrackAndPlay(index)
        }

        playButton.setOnClickListener {
            release.selectTrackAndPlay(index)
        }
    }

    fun setDownloadProgress(fileProgress: Long, fullSize: Long?): Int {
        if (progressBar == null) return -1
        val size = fullSize ?: Long.MAX_VALUE
        val progress: Double = (fileProgress.toDouble() / size.toDouble()) * 100.0
        progressBar.progress = progress.toInt()
        return progress.toInt()
    }
}
