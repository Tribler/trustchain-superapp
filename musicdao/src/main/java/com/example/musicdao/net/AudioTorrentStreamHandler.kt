package com.example.musicdao.net

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener


class AudioTorrentStreamHandler(
    private val progressBar: ProgressBar,
    val context: Context, val bufferInfo: TextView, val torrentButton: ImageButton
) : TorrentListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    private var initializedAudioPlayer = false
    private val BUFFER_PIECES = 5
    private var timerStart: Long = 0

    override fun onStreamReady(torrent: Torrent) {
        this.bufferInfo.text = "Downloading torrent: ${torrent.videoFile.name}\n Torrent state: ${torrent.state}"
        println("Stream completed! ${torrent.videoFile}")
        this.progressBar.progress = 100

        val ms = System.currentTimeMillis() - timerStart
        println("Took $ms ms")
    }

    override fun onStreamPrepared(torrent: Torrent) {
        //TODO in the future we assume that torrents only have 1 audio file (probably?)
        torrent.setSelectedFileIndex(0)
        torrent.startDownload()
        timerStart = System.currentTimeMillis()

        this.bufferInfo.text = "Downloading torrent: ${torrent.videoFile.name}\n Torrent state: ${torrent.state}"
        println("Prepared: " + torrent.videoFile)
    }

    override fun onStreamStopped() {
        println("Stream stopped")
    }

    override fun onStreamStarted(torrent: Torrent) {
        println("Stream started: " + torrent.videoFile)
    }

    override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {
        println("BufferProgress: ${status.bufferProgress}, progress: ${status.progress}, done: ${torrent.hasInterestedBytes()}")
        this.progressBar.progress = status.progress.toInt()

        if (status.bufferProgress < 100) {
            this.bufferInfo.text = "Downloading torrent: ${torrent.videoFile.name}\n Torrent state: BUFFERING\n ${status.bufferProgress}%"
        } else {
            this.bufferInfo.text =
                "Downloading torrent: ${torrent.videoFile.name}\n Torrent state: STREAM READY\n Total file progress: ${status.progress}%"
        }

        if (!initializedAudioPlayer && status.bufferProgress == 100) {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, torrent.videoFile.toUri())
                prepareAsync()
                setOnPreparedListener(this@AudioTorrentStreamHandler)
                setOnErrorListener(this@AudioTorrentStreamHandler)
            }
            initializedAudioPlayer = true
        }
    }

    override fun onStreamError(torrent: Torrent, e: Exception) {
        e.printStackTrace()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp?.reset()
        var message = ""
        when (extra) {
            MediaPlayer.MEDIA_ERROR_IO -> {
                message = "Media error: IO"
            }
            MediaPlayer.MEDIA_ERROR_MALFORMED -> {
                message = "Media error: malformed"
            }
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> {
                message = "Media error: invalid for progressive playback"
            }
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                message = "Media error: server died"
            }
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> {
                message = "Media error: timed out"
            }
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                message = "Media error: unknown"
            }
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> {
                message = "Media error: unsupported"
            }
        }
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast.show()
        return true
    }

    override fun onPrepared(mp: MediaPlayer) {
        this.torrentButton.isClickable = true
        this.torrentButton.isEnabled = true
        this.torrentButton.setOnClickListener {
            if (mp.isPlaying) {
                this.torrentButton.setImageResource(android.R.drawable.ic_media_play)
                mp.pause()
            } else {
                this.torrentButton.setImageResource(android.R.drawable.ic_media_pause)
                mp.start()
            }
        }
    }

}
