package com.example.musicdao

import android.content.Context
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.example.musicdao.net.AudioPlayer
import com.example.musicdao.net.MusicService
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
import kotlinx.android.synthetic.main.music_app_main.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction

/**
 * A release is an audio album, EP, single, etc.
 */
class Release(
    context: Context,
    private val magnet: String,
    private val musicService: MusicService,
    transaction: TrustChainTransaction
) : TableLayout(context),
    TorrentListener {
    private var tracks: MutableList<Track> = mutableListOf<Track>()
    private var currentFileIndex = -1
    private var fetchingMetadataRow = TableRow(context)
    private var metadataRow = TableRow(context)
    private lateinit var torrent: Torrent

    init {
        //Generate the UI
        this.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val blockMetadata = TextView(context)
        blockMetadata.text = "Signed block with release:\n$transaction"
        blockMetadata.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        val params = blockMetadata.layoutParams as TableRow.LayoutParams
        params.span = 3
        blockMetadata.layoutParams = params
        metadataRow.addView(blockMetadata)
        this.addView(metadataRow)
        val magnetTextView = TextView(context)
        magnetTextView.text = "Fetching metadata for magnet link..."
        magnetTextView.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        fetchingMetadataRow.addView(magnetTextView)
        this.addView(fetchingMetadataRow)

        //When the Release is added, it will try to fetch the metadata for the corresponding magnet
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
     fun selectTrackAndDownload(index: Int) {
        if (this::torrent.isInitialized) {
            currentFileIndex = index
            torrent.setSelectedFileIndex(currentFileIndex)
            val track = tracks[currentFileIndex]
            track.selectToPlay(torrent)
            //TODO needs to have a solid check whether the file was already downloaded before
            if (torrent.videoFile.isFile && torrent.videoFile.length() == torrent.torrentHandle.torrentFile()
                    .files().fileSize(index)
            ) {
                musicService.fillProgressBar()
                musicService.bufferInfo.text =
                    "Selected: ${torrent.videoFile.nameWithoutExtension}, buffer progress: 100%"
                AudioPlayer.getInstance(context, musicService).setAudioResource(torrent.videoFile)
            } else {
                musicService.resetProgressBar()
                torrent.startDownload()
            }
        }
    }

    /**
     * When the track is buffered, this is called.
     * Then set the audio resource on the audioplayer.
     */
    override fun onStreamReady(torrent: Torrent) {
        println("Stream ready")
        AudioPlayer.getInstance(context, musicService).setAudioResource(torrent.videoFile)
    }

    /**
     * This is called when the metadata is fetched. Then we can render a table with all
     * of the songs
     */
    override fun onStreamPrepared(torrent: Torrent) {
        //TODO add a check here for whether this torrent is the torrent of this Release
        println("Stream prepared")
        this.removeView(fetchingMetadataRow)
        this.torrent = torrent
        torrent?.fileNames?.forEachIndexed { index, fileName ->
            val allowedExtensions =
                listOf<String>("flac", "mp3", "3gp", "aac", "mkv", "wav", "ogg", "mp4", "m4a")
            var found = false
            for (s in allowedExtensions) {
                if (fileName.endsWith(s)) {
                    found = true
                }
            }
            if (found) {
                val track = Track(context, magnet, fileName, index, this, musicService)
                this.addView(track)
                tracks.add(track)
            }
        }
    }

    override fun onStreamStopped() {
        println("Stream stopped")
    }

    override fun onStreamStarted(torrent: Torrent) {
        println("Stream started: " + torrent.videoFile)
    }

    /**
     * This is called when the torrent client downloaded a torrent piece.
     */
    override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {
        if (currentFileIndex == -1) return
        val track = tracks[currentFileIndex]
        track.handleDownloadProgress(torrent, status)
    }

    override fun onStreamError(torrent: Torrent, e: Exception) {
        e.printStackTrace()
    }
}
