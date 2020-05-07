package com.example.musicdao.net

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.net.toUri
import java.io.File

lateinit var instance: AudioPlayer

class AudioPlayer(context: Context) : LinearLayout(context), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, SeekBar.OnSeekBarChangeListener {
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var timerStart: Long = 0
    private var interestedFraction: Float = 0F
    private var piecesInfoLog = ""
    private var seekProgress = SeekProgress(context)
    private val bufferInfo = TextView(context)
    private val progressBar = ProgressBar(context)
    private val seekBar = SeekBar(context)
    private val playButton = ImageButton(context)

    init {
        val linearLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        this.addView(seekProgress, linearLayoutParams)
        this.addView(bufferInfo, linearLayoutParams)
        this.addView(progressBar, linearLayoutParams)
        this.addView(seekBar, linearLayoutParams)
        this.addView(playButton, linearLayoutParams)
        seekBar.setOnSeekBarChangeListener(this)
    }

    companion object {
        fun getInstance(context: Context) : AudioPlayer {
            if (!::instance.isInitialized) {
                createInstance(context)
            }
            return instance
        }

        @Synchronized
        private fun createInstance(context: Context) {
            instance = AudioPlayer(context)
        }
    }

    public fun setAudioResource(file: File) {
        mediaPlayer.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(context, file.toUri())
            prepareAsync()
            setOnPreparedListener(this@AudioPlayer)
            setOnErrorListener(this@AudioPlayer)
        }
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
        this.playButton.isClickable = true
        this.playButton.isActivated = true
        this.playButton.isEnabled = true
        this.playButton.setOnClickListener {
            if (mp.isPlaying) {
                this.playButton.setImageResource(android.R.drawable.ic_media_play)
                mp.pause()
            } else {
                this.playButton.setImageResource(android.R.drawable.ic_media_pause)
                mp.start()
            }
        }
        //Directly play the track when it is prepared
        this.playButton.callOnClick()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return
        interestedFraction = (progress.toFloat() / 100.toFloat())
        val duration = mediaPlayer?.duration
        if (duration != null) {
            val seekMs: Int = (duration * interestedFraction).toInt()
            mediaPlayer?.seekTo(seekMs)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }

}
