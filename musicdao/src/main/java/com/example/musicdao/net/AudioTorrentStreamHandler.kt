package com.example.musicdao.net

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.ProgressBar
import androidx.core.net.toUri
import com.example.musicdao.R
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
import java.io.File
import java.lang.Exception

class AudioTorrentStreamHandler(private val progressBar: ProgressBar, val context: Context): TorrentListener, MediaPlayer.OnPreparedListener {
    private var playingAudio = false
    private var timerStart: Long = 0

    override fun onStreamReady(torrent: Torrent) {
        println("Stream completed! ${torrent.videoFile}")

        val ms = System.currentTimeMillis() - timerStart
        println("Took $ms ms")
    }

    override fun onStreamPrepared(torrent: Torrent) {
        //TODO in the future we assume that torrents only have 1 audio file (probably?)
        torrent.setSelectedFileIndex(0)
        torrent.startDownload()
        timerStart = System.currentTimeMillis()

        println("Prepared: " + torrent.videoFile)
    }

    override fun onStreamStopped() {
        println("Stream stopped")
    }

    override fun onStreamStarted(torrent: Torrent) {
        println("Stream started: " + torrent.videoFile)
    }

    override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {
        if (!playingAudio && status.bufferProgress == 100) {
            MediaPlayer().apply {
                setDataSource(context, torrent.videoFile.toUri())
                setOnPreparedListener(this@AudioTorrentStreamHandler)
                prepareAsync()
            }
            playingAudio = true
        }

        println("BufferProgress: ${status.bufferProgress}, progress: ${status.progress}")
        this.progressBar.progress = status.bufferProgress
    }

    override fun onStreamError(torrent: Torrent, e: Exception) {
        e.printStackTrace()
    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.start()
    }

}
