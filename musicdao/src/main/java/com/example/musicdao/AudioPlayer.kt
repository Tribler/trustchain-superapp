package com.example.musicdao

import android.R.drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.frostwire.jlibtorrent.FileStorage
import kotlinx.android.synthetic.main.fragment_trackplaying.*
import java.io.File
import java.io.FileInputStream

lateinit var instance: AudioPlayer

/**
 * Implements an Android MediaPlayer. Is a singleton.
 */
class AudioPlayer : Fragment(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var interestedFraction: Float = 0F
    private var previousFile: File? = null
    private var currentReleaseFiles: FileStorage? = null
    private var currentFileIndex: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trackplaying, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        songArtist.text = "No track currently playing"
        seekBarAudioPlayer.setOnSeekBarChangeListener(this)

        // Handle playing and pausing tracks
        playButtonAudioPlayer.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                playButtonAudioPlayer.setImageResource(android.R.drawable.ic_media_play)
                mediaPlayer.pause()
            } else {
                playButtonAudioPlayer.setImageResource(android.R.drawable.ic_media_pause)
                mediaPlayer.start()
            }
        }

        // Fast forward: go to the next track in the playlist
        fastforwardButton.setOnClickListener {
            goToTrack(currentFileIndex + 1)
        }

        // Rewind: if playing, go to the start of the track.
        // If at the start of a track, go to the previous track
        rewindButton.setOnClickListener {
            if (mediaPlayer.currentPosition < 2000) {
                goToTrack(currentFileIndex - 1)
            } else {
                mediaPlayer.seekTo(0)
            }
        }

        followSeekBarWithTrack()
    }

    /**
     * Fast forward to another track in the playlist
     * Playlist is currentReleaseFiles var
     * @param index the index of the track in the current playlist
     */
    private fun goToTrack(index: Int) {
        val files = currentReleaseFiles
        if (files is FileStorage) {
            val nextFile = File(files.filePath(index,
                context?.cacheDir?.absolutePath) ?: "")
            if (nextFile.isFile) {
                setAudioResource(nextFile, index, files)
            }
        }
    }

    companion object {
        fun getInstance(): AudioPlayer {
            if (!::instance.isInitialized) {
                createInstance()
            }
            return instance
        }

        @Synchronized
        private fun createInstance() {
            instance =
                AudioPlayer()
        }
    }

    /**
     * This function updates the seek bar location every second with the playing position
     */
    private fun followSeekBarWithTrack() {
        val mHandler = Handler()
        val thread: Thread = object : Thread() {
            override fun run() {
                val mCurrentPosition: Int = mediaPlayer.currentPosition / 1000
                seekBarAudioPlayer.progress = mCurrentPosition
                mHandler.postDelayed(this, 1000)
            }
        }
        thread.start()
    }

    /**
     * Reset internal state to prepare for playing a track
     */
    fun prepareNextTrack() {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        seekBarAudioPlayer.progress = 0
        playButtonAudioPlayer.setImageResource(drawable.ic_media_play)
        playButtonAudioPlayer.isClickable = false
        playButtonAudioPlayer.isActivated = false
        playButtonAudioPlayer.isEnabled = false
        mediaPlayer.reset()
    }

    /**
     * Select file to play and prepare the mediaPlayer to play it.
     * Also present the user with some information on this file.
     */
    fun setAudioResource(file: File, index: Int, files: FileStorage) {
        if (previousFile == file) return
        currentReleaseFiles = files
        currentFileIndex = index
        previousFile = file
        songArtist.text =
            "${file.nameWithoutExtension}"
        val fis = FileInputStream(file)
        prepareNextTrack()
        mediaPlayer.apply {
            setOnPreparedListener(this@AudioPlayer)
            setOnErrorListener(this@AudioPlayer)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(fis.fd)
            prepareAsync()
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp?.reset()
        var message = ""
        when (what) {
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
        val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        toast.show()
        return true
    }

    override fun onPrepared(mp: MediaPlayer) {
        // Sync the seek bar horizontal location with track duration
        seekBarAudioPlayer.max = mediaPlayer.duration / 1000
        // Directly play the track when it is prepared
        playButtonAudioPlayer.callOnClick()
    }

    /**
     * For now simply reset the state of the media player when we reach the end of a track
     */
    override fun onCompletion(mp: MediaPlayer?) {
        goToTrack(currentFileIndex + 1)
    }

    /**
     * This enables seeking through the track
     */
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return
        mediaPlayer.seekTo(progress * 1000)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    //TODO this should probably be replaced by a nicer solution in the future
    // (see Release->MusicService->AudioPlayer calling this)
    fun setSongArtistText(s: String) {
        songArtist.text = s
    }
}
