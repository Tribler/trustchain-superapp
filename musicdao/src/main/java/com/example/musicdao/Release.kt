package com.example.musicdao

import android.content.Context
import android.widget.*
import com.example.musicdao.net.AudioPlayer
import com.example.musicdao.net.MusicService
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener

class Release(context: Context, private val magnet: String, private val musicService: MusicService) : TableLayout(context),
    TorrentListener {
    private var tracks: MutableList<Track> = mutableListOf<Track>()
    private var currentFileIndex = -1
    private lateinit var torrent: Torrent

    init {
        this.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val tableRow = TableRow(context)
        tableRow.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        val magnetTextView = TextView(context)
        magnetTextView.text = "Release: ${magnet.substring(0, 10)}"
        magnetTextView.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        this.addView(tableRow)

        try {
            musicService.torrentStream?.startStream(magnet)
            musicService.torrentStream?.addListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Select a track from the Release and start downloading and seeding it
     */
    public fun selectTrackAndDownload(index: Int) {
        if (this::torrent.isInitialized) {
            currentFileIndex = index
            torrent.setSelectedFileIndex(currentFileIndex)
            //If the file does not exist, we need to download it
            if (torrent.videoFile.isFile) {
                AudioPlayer.getInstance(context, musicService).setAudioResource(torrent.videoFile)
            } else {
                torrent.startDownload()
            }
            val track = tracks[currentFileIndex]
            track.selectToPlay(torrent)
        }
    }

    /**
     * When the track is buffered, this is called.
     * Then set the audio resource on the audioplayer.
     */
    override fun onStreamReady(torrent: Torrent) {
//        val track = tracks[currentFileIndex]TODO
//        track.handleDownloadProgress(torrent)
//        TorrentStream.getInstance().removeListener(this)TODO remove the listener at some point
        println("Stream ready")
        AudioPlayer.getInstance(context, musicService).setAudioResource(torrent.videoFile)
    }

    override fun onStreamPrepared(torrent: Torrent) {
        //TODO add a check here for whether this torrent is the torrent of this Release
        println("Stream prepared")
//        torrent.setSelectedFileIndex(currentFileIndex)
//        if (torrent.videoFile.length() < 1) {
//            torrent.startDownload()
//        }

        this.torrent = torrent
        torrent?.fileNames?.forEachIndexed { index, fileName ->
            val track = Track(context, magnet, fileName, index, this, musicService)
            this.addView(track)
            tracks.add(track)
        }
    }

    override fun onStreamStopped() {
        println("Stream stopped")
    }

    override fun onStreamStarted(torrent: Torrent) {
        println("Stream started: " + torrent.videoFile)
    }

    override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {
        println("Stream progress: Buffer: ${status.bufferProgress} Track: ${status.progress}")
        if (currentFileIndex == -1) return
        val track = tracks[currentFileIndex]
        track.handleDownloadProgress(torrent, status)
    }

    override fun onStreamError(torrent: Torrent, e: Exception) {
        e.printStackTrace()
    }
}
