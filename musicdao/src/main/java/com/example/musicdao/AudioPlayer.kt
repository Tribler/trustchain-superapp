package com.example.musicdao

import android.net.Uri
import android.os.Bundle
import android.view.View
import com.frostwire.jlibtorrent.FileStorage
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.android.synthetic.main.fragment_trackplaying.*
import java.io.File

lateinit var instance: AudioPlayer

/**
 * Implements an Android MediaPlayer. Is a singleton.
 */
class AudioPlayer : MusicFragment(R.layout.fragment_trackplaying) {
    private var interestedFraction: Float = 0F
    private var playingFile: File? = null
    private var currentReleaseFiles: FileStorage? = null
    private var currentFileIndex: Int = 0

    private var testExoPlayer: SimpleExoPlayer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setInstance(this)
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

    private fun release() {
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
    }

    fun retry() {
        val player = testExoPlayer ?: return
        // Try to load more of the audio track
        if (playingFile != null && player.totalBufferedDuration < 10000) {
            val mediaSource = buildMediaSource(Uri.fromFile(playingFile))
            requireActivity().runOnUiThread {
                player.prepare(mediaSource, false, false)
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

    fun setTrackInfo(trackName: String) {
        trackInfo.text = trackName
        trackInfo.visibility = View.VISIBLE
    }

    fun hideTrackInfo() {
        trackInfo.visibility = View.GONE
    }
}
