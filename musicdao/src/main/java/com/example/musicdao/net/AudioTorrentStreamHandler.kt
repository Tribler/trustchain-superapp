package com.example.musicdao.net

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.opengl.Visibility
import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.musicdao.R
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener


class AudioTorrentStreamHandler(
    private val progressBar: ProgressBar,
    val context: Context, val bufferInfo: TextView, val torrentButton: ImageButton
) : TorrentListener, MediaPlayer.OnPreparedListener {
    private var playingAudio = false
    private val BUFFER_PIECES = 3
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

        if (status.bufferProgress <= BUFFER_PIECES) {
            this.bufferInfo.text = "Downloading torrent: ${torrent.videoFile.name}\n Torrent state: BUFFERING\n ${(status.bufferProgress / BUFFER_PIECES) * 100}%"
        } else {
            this.bufferInfo.text =
                "Downloading torrent: ${torrent.videoFile.name}\n Torrent state: STREAMING\n Total file progress: ${((status.progress / 100) * 100).toInt()}%"
        }

        if (!playingAudio && status.bufferProgress >= BUFFER_PIECES) {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, torrent.videoFile.toUri())
                setOnPreparedListener(this@AudioTorrentStreamHandler)
                prepareAsync()
            }
            playingAudio = true
        }
    }

    override fun onStreamError(torrent: Torrent, e: Exception) {
        e.printStackTrace()
    }

    override fun onPrepared(mp: MediaPlayer) {
        this.torrentButton.isClickable = true
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
