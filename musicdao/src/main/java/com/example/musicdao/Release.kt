package com.example.musicdao

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.alerts.*
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
import kotlinx.android.synthetic.main.fragment_release.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.common.ui.BaseFragment
import java.io.File
import java.lang.Exception

/**
 * A release is an audio album, EP, single, etc.
 */
class Release(
    private val magnet: String,
    private val artists: String,
    private val title: String,
    private val date: String,
    private val publisher: String
) : Fragment(), TorrentListener {
    private var metadata: TorrentInfo? = null
    private var tracks: MutableMap<Int, Track> = hashMapOf()
    private var currentFileIndex = -1
    private var enqueuedFileIndex = -1
    private var prevFileIndex = -1
    private var prevProgress = -1.0f
    private var torrentStream: TorrentStream? = null
    private var localTorrent: Torrent? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_release, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blockMetadata.text =
            HtmlCompat.fromHtml(
                "<b>$artists - $title<br></b>" +
                    date, 0
            )

        // Generate the UI
        // When the Release is added, it will try to fetch the metadata for the corresponding magnet
//        trackLibrary.downloadMagnet(this, magnet, localContext.cacheDir)

        val torrentOptions = TorrentOptions.Builder()
            .saveLocation(context?.cacheDir)
            .removeFilesAfterStop(true)
            .autoDownload(false)
            .build()
        torrentStream = TorrentStream.init(torrentOptions)
        torrentStream?.addListener(this)
        torrentStream?.startStream(magnet)
    }

    override fun onDestroy() {
        super.onDestroy()
        torrentStream?.stopStream()
        torrentStream = null
    }

    private fun setMetadata(metadata: TorrentInfo) {
        this.metadata = metadata
        val num = metadata.numFiles()
        val filestorage = metadata.files()

        val allowedExtensions =
            listOf("flac", "mp3", "3gp", "aac", "mkv", "wav", "ogg", "mp4", "m4a")

        for (index in 0 until num) {
            val fileName = filestorage.fileName(index)
            var found = false
            for (s in allowedExtensions) {
                if (fileName.endsWith(s)) {
                    found = true
                }
            }
            if (found) {
                val track = Track(
                    fileName,
                    index,
                    this,
                    Util.readableBytes(filestorage.fileSize(index))
                )

                // Add a table row (Track) to the table (Release)
                val transaction = childFragmentManager.beginTransaction()
                transaction.add(R.id.release_table_layout, track, "track$index")
                transaction.commit()
                tracks[index] = track
            }
        }
    }

    /**
     * Select a track from the Release and start downloading and seeding it
     */
    fun selectTrackAndDownload(index: Int) {
        currentFileIndex = index

        val audioPlayer = AudioPlayer.getInstance()
        audioPlayer.prepareNextTrack()

        val tor = localTorrent
        if (tor != null) {
            torrentStream?.removeListener(this)
            tor.setSelectedFileIndex(currentFileIndex)
            Util.setSequentialPriorities(tor)
            torrentStream?.addListener(this)

            // TODO needs to have a solid check whether the file was already downloaded before
            if (tor.videoFile.isFile && tor.videoFile.length() > 1024 * 512) {
                startPlaying(tor.videoFile, currentFileIndex)
            }
        }
    }

    /**
     * For each track in the list, try to update its progress bar UI state which corresponds to the
     * percentage of downloaded pieces from that track
     */
    private fun updateFileProgress(fileProgressArray: LongArray) {
        fileProgressArray.forEachIndexed { index, fileProgress ->
            tracks[index]?.setDownloadProgress(
                fileProgress,
                metadata?.files()?.fileSize(index)
            )
        }
    }

    private fun startPlaying(file: File, index: Int) {
        val audioPlayer = AudioPlayer.getInstance()
        audioPlayer.setAudioResource(file, index)
    }

    override fun onStreamReady(torrent: Torrent) {
        if (!AudioPlayer.getInstance().isPlaying()) {
            startPlaying(
                torrent.videoFile,
                currentFileIndex
            )
        }
    }

    override fun onStreamPrepared(torrent: Torrent) {
        localTorrent = torrent
        torrent.setSelectedFileIndex(0)
        torrent.startDownload()
        Util.setSequentialPriorities(torrent)
        val torrentFile = torrent.torrentHandle?.torrentFile()
            ?: throw Error("Unknown torrent file metadata")
        setMetadata(torrentFile)
    }

    override fun onStreamStopped() {}

    override fun onStreamStarted(torrent: Torrent) {}

    override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {
        val fileProgress = torrent.torrentHandle?.fileProgress()
        if (fileProgress != null) updateFileProgress(fileProgress)
        val progress = status.progress
        if (progress > 30 && !AudioPlayer.getInstance().isPlaying()) {
            startPlaying(
                torrent.videoFile,
                currentFileIndex
            )
        }
        if (progress != prevProgress) {
            AudioPlayer.getInstance().retry()
            println("Buffer: ${status.bufferProgress}, progress: $progress")
        }
        prevProgress = progress
        prevFileIndex = currentFileIndex
    }

    override fun onStreamError(torrent: Torrent, e: Exception) {
        Log.e("TorrentStream", "Torrent stream error")
        Toast.makeText(context, "Torrent stream error: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }

}
