package com.example.musicdao

import android.content.Context
import android.widget.*
import kotlinx.android.synthetic.main.music_app_main.*


class Track(
    context: Context,
    name: String,
    private val index: Int,
    release: Release,
    private val musicService: MusicService
) : TableRow(context) {
    private val bufferInfo: TextView = TextView(context)
    private val nameView: TextView = TextView(context)
    private val indexView: TextView = TextView(context)
    private val playButton: ImageButton = ImageButton(context)
    private val progressBar: ProgressBar = ProgressBar(
        context,
        null,
        android.R.attr.progressBarStyleHorizontal
    )

    init {
        // Initialize all UI elements
        val tableLayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        tableLayoutParams.setMargins(5, 5, 5, 5)
        this.layoutParams = tableLayoutParams
        val rowParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        playButton.layoutParams = rowParams
        playButton.setImageResource(android.R.drawable.ic_media_play)
        this.addView(playButton)
        indexView.layoutParams = rowParams
        indexView.text = index.toString()
        this.addView(indexView)
        nameView.layoutParams = rowParams
        nameView.maxWidth = 300
        nameView.text = name
        this.addView(nameView)
        progressBar.layoutParams = rowParams
        progressBar.max = 100
        progressBar.progress = 0
        this.addView(progressBar)

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
