package nl.tudelft.trustchain.musicdao.ui.components.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import nl.tudelft.trustchain.musicdao.core.repositories.model.Song
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class PlayerViewModel(context: Context) : ViewModel() {

    private val _playingTrack: MutableStateFlow<Song?> = MutableStateFlow(null)
    val playingTrack: StateFlow<Song?> = _playingTrack

    private val _coverFile: MutableStateFlow<File?> = MutableStateFlow(null)
    val coverFile: StateFlow<File?> = _coverFile

    val exoPlayer by lazy {
        ExoPlayer.Builder(context).build()
    }

    private fun buildMediaSource(uri: Uri, context: Context): MediaSource? {
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(context, "musicdao-audioplayer")
        val mediaItem = MediaItem.fromUri(uri)
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }

    fun playDownloadedTrack(track: Song, context: Context, cover: File? = null) {
        _playingTrack.value = track
        _coverFile.value = cover
        val mediaItem = MediaItem.fromUri(Uri.fromFile(track.file))
        Log.d("MusicDAOTorrent", "Trying to play ${track.file}")
        exoPlayer.playWhenReady = true
        exoPlayer.seekTo(0, 0)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun playDownloadingTrack(track: Song, context: Context, cover: File? = null) {
        _playingTrack.value = track
        _coverFile.value = cover
        val mediaSource = buildMediaSource(Uri.fromFile(track.file), context)
            ?: throw Error("Media source could not be instantiated")
        Log.d("MusicDAOTorrent", "Trying to play ${track.file}")
        exoPlayer.playWhenReady = true
        exoPlayer.seekTo(0, 0)
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun release() {
        exoPlayer.release()
        exoPlayer.stop()
    }

    companion object {
        fun provideFactory(
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(
                    context
                ) as T
            }
        }
    }
}
