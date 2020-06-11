package com.example.musicdao

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.frostwire.jlibtorrent.FileStorage
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.android.synthetic.main.fragment_trackplaying.*
import nl.tudelft.trustchain.common.ui.BaseFragment
import java.io.File


lateinit var instance: AudioPlayer

/**
 * Implements an Android MediaPlayer. Is a singleton.
 */
class AudioPlayer : BaseFragment(R.layout.fragment_trackplaying) {
    //TODO most of the code down below should be removed or adapted in order to migrate to ExoPlayer
    private var interestedFraction: Float = 0F
    private var playingFile: File? = null
    private var currentReleaseFiles: FileStorage? = null
    private var currentFileIndex: Int = 0

    private var testExoPlayer: SimpleExoPlayer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setInstance(this)

//        songArtist.text = "No track currently playing"
//        seekBarAudioPlayer.setOnSeekBarChangeListener(this)

        // Handle playing and pausing tracks
//        playButtonAudioPlayer.setOnClickListener {
////            if (mediaPlayer.isPlaying) {
////                playButtonAudioPlayer.setImageResource(android.R.drawable.ic_media_play)
////                mediaPlayer.pause()
////            } else {
////                playButtonAudioPlayer.setImageResource(android.R.drawable.ic_media_pause)
////                mediaPlayer.start()
////            }
//        }

        initExoPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.release()
    }

    private fun initExoPlayer() {
        testExoPlayer = ExoPlayerFactory.newSimpleInstance(context)
        playerView.player = testExoPlayer
    }

    fun release() {
        testExoPlayer?.release()
        testExoPlayer = null
    }

    private fun buildMediaSource(uri: Uri): MediaSource? {
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(context, "musicdao-audioplayer")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(uri)
    }

    companion object {
        fun getInstance(): AudioPlayer {
            return instance
        }

        @Synchronized
        private fun setInstance(audioPlayer: AudioPlayer) {
            instance =
                audioPlayer
        }
    }

    fun isPlaying(): Boolean {
        val player = testExoPlayer ?: return false
        return player.isPlaying
    }

    /**
     * Reset internal state to prepare for playing a track
     */
    fun prepareNextTrack() {
        testExoPlayer?.stop()
        testExoPlayer?.seekTo(0)
//        if (mediaPlayer.isPlaying) mediaPlayer.stop()
//        seekBarAudioPlayer.progress = 0
//        playButtonAudioPlayer.setImageResource(drawable.ic_media_play)
//        mediaPlayer.reset()
    }

    fun retry() {
        val player = testExoPlayer ?: throw Error("ExoPlayer is null")
        println("buff%: ${player.bufferedPercentage}, buff pos: ${player.bufferedPosition}, total buff: ${player.totalBufferedDuration}")
        //Try to load more of the audio track
        if (playingFile != null && player.totalBufferedDuration < 10000) {
            val mediaSource = buildMediaSource(Uri.fromFile(playingFile))
            requireActivity().runOnUiThread {
                player.prepare(mediaSource, false, false)
//                player.retry()
            }
        }
    }

    /**
     * Select file to play and prepare the mediaPlayer to play it.
     * Also present the user with some information on this file.
     */
    fun setAudioResource(file: File, index: Int) {
        currentFileIndex = index
        playingFile = file
        val mediaSource = buildMediaSource(Uri.fromFile(file))
            ?: throw Error("Media source could not be instantiated")
        val player = testExoPlayer ?: throw Error("ExoPlayer is null")
        requireActivity().runOnUiThread {
            player.playWhenReady = true
            player.seekTo(0, 0)
            player.prepare(mediaSource, false, false)
        }
    }

}
