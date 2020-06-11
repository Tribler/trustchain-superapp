package com.example.musicdao

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.track_row.trackTitle
import kotlinx.android.synthetic.main.track_table_row.*


class Track(
    private val name: String,
    private val index: Int,
    private val release: Release,
    private val size: String
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.track_table_row, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackTitle.text = name
        trackArtist.text = size

        playButton.setOnClickListener {
            release.selectTrackAndDownload(index)
        }
    }

    fun setDownloadProgress(fileProgress: Long, fullSize: Long?): Int {
        val size = fullSize ?: Long.MAX_VALUE
        val progress: Double = (fileProgress.toDouble() / size.toDouble()) * 100.0
        progressBar.progress = progress.toInt()
        return progress.toInt()
    }

    fun setCompleted() {
        progressBar.progress = 100
    }
}
