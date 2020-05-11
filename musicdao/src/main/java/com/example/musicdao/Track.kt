package com.example.musicdao

import android.content.Context
import android.widget.*
import com.example.musicdao.net.AudioPlayer
import com.example.musicdao.net.MusicService
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
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

    init {
        //Initialize all UI elements
        val tableLayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        tableLayoutParams.setMargins(5,5,5,5)
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
        nameView.text = name
        this.addView(nameView)

        playButton.setOnClickListener {
            release.selectTrackAndDownload(index)
        }
    }

    /**
     * Load the song into the AudiPlayer to prepare it for playback
     */
    fun selectToPlay(torrent: Torrent) {
        AudioPlayer.getInstance(context, musicService).prepareNextTrack()
        musicService.bufferInfo.text = "Selected: ${torrent.videoFile.nameWithoutExtension}, searching for peers"
    }

    /**
     * Update the progressBar once a torrent piece is downloaded.
     * BufferProgress is from 0-100 and is 100 when the initial pieces for streaming are downloaded
     * (see interestedPieces in Android Streaming library).
     * status.progress is from 0-100 and is the overall file progress.
     */
    fun handleDownloadProgress(torrent: Torrent, status: StreamStatus) {
        musicService.progressBar.progress = status.progress.toInt()
        musicService.bufferInfo.text = "Selected: ${torrent.videoFile.nameWithoutExtension}, buffer progress: ${status.bufferProgress}%"
    }
}
