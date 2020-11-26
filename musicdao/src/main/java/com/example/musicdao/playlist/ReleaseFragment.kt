package com.example.musicdao.playlist

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.musicdao.MusicService
import com.example.musicdao.R
import com.example.musicdao.dialog.TipArtistDialog
import com.example.musicdao.net.ContentSeeder
import com.example.musicdao.player.AudioPlayer
import com.example.musicdao.util.Util
import com.example.musicdao.wallet.CryptoCurrencyConfig
import com.frostwire.jlibtorrent.FileStorage
import com.frostwire.jlibtorrent.TorrentInfo
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
import kotlinx.android.synthetic.main.fragment_release.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bitcoinj.core.Address
import java.io.File

/**
 * A release is an audio album, EP, single, etc.
 * It is related to 1 trustchain block, with the tag "publish_release" and its corresponding
 * structure (see TODO doc).
 * It also implements a TorrentListener, which allows for configuring callbacks on download progress
 * of the magnet link that is contained in the Release TrustChain block
 */
class ReleaseFragment(
    private val magnet: String,
    private val artists: String,
    private val title: String,
    private val releaseDate: String,
    private val publisher: String,
    private val torrentInfoName: String?
) : Fragment(R.layout.fragment_release), TorrentListener {
    private var metadata: FileStorage? = null
    private var tracks: MutableMap<Int, TrackFragment> = hashMapOf()
    private var currentFileIndex = -1
    private var prevFileIndex = -1
    private var prevProgress = -1.0f
    private var streamingTorrent: Torrent? = null
    private var downloadedTorrent: TorrentInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        retrieveTorrent()

        blockMetadata.text =
            HtmlCompat.fromHtml(
                "<b>$artists - $title<br></b>" +
                    releaseDate, 0
            )

        enableTipButton()

        lifecycleScope.launchWhenCreated {
            while (isActive) {
                if (activity is MusicService && debugTextRelease != null) {
                    debugTextRelease.text = (activity as MusicService).getStatsOverview()
                }
                delay(1000)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Debug button is a simple toggle for a connectivity stats display
            R.id.action_debug -> {
                if (debugTextRelease != null) {
                    if (debugTextRelease.visibility == View.VISIBLE) {
                        debugTextRelease.visibility = View.GONE
                    } else {
                        debugTextRelease.visibility = View.VISIBLE
                    }
                }
                true
            }
            R.id.action_wallet -> {
                findNavController().navigate(R.id.walletFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        (activity as MusicService).torrentStream.removeListener(this)
        super.onDestroy()
    }

    /**
     * Set-up torrent connection for current magnet link; if we already have it predownloaded we
     * will load all metadata from cache; otherwise we will use TorrentStream to stream the content
     */
    private fun retrieveTorrent() {
        var torrentUrl = magnet
        if (torrentInfoName != null) {
            val torrentFileName =
                "${context?.cacheDir}/$torrentInfoName.torrent"
            val torrentFile = File(torrentFileName)
            val saveDir = context?.cacheDir
            if (torrentFile.isFile && TorrentInfo(torrentFile).isValid) {
                // This means we have the torrent file already locally and we can skip the step of
                // obtaining the TorrentInfo from magnet link
                torrentUrl = torrentFile.toURI().toURL().toString()
                if (saveDir != null &&
                    Util.isTorrentCompleted(TorrentInfo(torrentFile), saveDir)
                ) {
                    // All files are already downloaded so we do not need to use TorrentStream
                    val torrentInfo = TorrentInfo(torrentFile)
//                    (activity as MusicService).torrentStream.sessionManager.download(torrentInfo, saveDir)
                    downloadedTorrent = torrentInfo
                    setMetadata(torrentInfo.files(), preloaded = true)
                    return
                }
            }
        }
        (activity as MusicService).torrentStream.addListener(this)
        (activity as MusicService).torrentStream.startStream(torrentUrl)
    }

    /**
     * If the Release has a publisher, then this should point to the public key of the wallet of
     * this publisher. This method checks whether the publisher field exists and whether it is
     * properly formatted so money can be sent to it. If it is, a tip button is shown
     */
    private fun enableTipButton() {
        if (publisher.isNotEmpty()) {
            try {
                Address.fromString(CryptoCurrencyConfig.networkParams, publisher)
            } catch (e: Exception) {
                return
            }
            tipButton.visibility = View.VISIBLE
            tipButton.isClickable = true
            tipButton.setOnClickListener {
                TipArtistDialog(publisher)
                    .show(childFragmentManager, "Tip the artist")
            }
        }
    }

    /**
     * This sets the metadata of the TorrentInfo in the UI, it renders the file list and other
     * metadata
     */
    private fun setMetadata(metadata: FileStorage, preloaded: Boolean = false) {
        loadingTracks.visibility = View.GONE
        this.metadata = metadata
        val num = metadata.numFiles()
        val filestorage = metadata

        for (index in 0 until num) {
            var fileName = filestorage.fileName(index)
            fileName = Util.checkAndSanitizeTrackNames(fileName)
            var progress: Int? = null
            if (preloaded) {
                progress = 100
            }
            if (fileName != null) {
                val track = TrackFragment(
                    fileName,
                    index,
                    this,
                    Util.readableBytes(filestorage.fileSize(index)),
                    progress
                )

                // Add a table row (Track) to the table (Release)
                val transaction = childFragmentManager.beginTransaction()
                transaction.add(R.id.release_table_layout, track, "track$index")
                transaction.commit()

                val transaction2 = childFragmentManager.beginTransaction()
                transaction2.add(
                    R.id.release_table_layout,
                    Fragment(R.layout.track_table_divider),
                    "track$index-divider"
                )
                transaction2.commitAllowingStateLoss()

                tracks[index] = track
            }
        }
    }

    /**
     * Select a track from the Release and start downloading and seeding it
     */
    fun selectTrackAndPlay(index: Int) {
        currentFileIndex = index

        val audioPlayer = AudioPlayer.getInstance()
        audioPlayer?.prepareNextTrack()

        val tor = streamingTorrent
        val localTorrent = downloadedTorrent
        if (tor != null) {
            (activity as MusicService).torrentStream.removeListener(this)
            tor.setSelectedFileIndex(currentFileIndex)
            Util.setSequentialPriorities(tor)
            (activity as MusicService).torrentStream.addListener(this)

            // TODO needs to have a solid check whether the file was already downloaded before
            if (tor.videoFile.isFile && tor.videoFile.length() > 1024 * 512) {
                startPlaying(tor.videoFile, currentFileIndex)
            } else {
                AudioPlayer.getInstance()
                    ?.setTrackInfo("Buffering track: " + tor.videoFile.nameWithoutExtension)
            }
        } else if (localTorrent != null) {
            val fileToPlay =
                File("${context?.cacheDir}/${localTorrent.files().filePath(currentFileIndex)}")
            // It is not a streaming torrent, and therefore we can conclude we already have
            // it locally so we can just start playing it
            if (fileToPlay.isFile && fileToPlay.length() > 1024 * 512) {
                startPlaying(fileToPlay, currentFileIndex)
            }
        }
    }

    /**
     * For each track in the list, try to update its progress bar UI state which corresponds to the
     * percentage of downloaded pieces from that track
     */
    private fun updateFileProgress(fileProgressArray: LongArray) {
        if (this.isAdded) {
            fileProgressArray.forEachIndexed { index, fileProgress ->
                val progress = tracks[index]?.getDownloadProgress(
                    fileProgress,
                    metadata?.fileSize(index)
                )
                if (progress != null) tracks[index]?.setDownloadProgress(progress)
            }
        }
    }

    private fun startPlaying(file: File, index: Int) {
        val audioPlayer = AudioPlayer.getInstance()
        audioPlayer?.setAudioResource(file, index)
        audioPlayer?.setTrackInfo(
            Util.checkAndSanitizeTrackNames(file.name) ?: ""
        )
    }

    fun resolveTorrentUrl(magnet: String): String {
        var torrentUrl = magnet
        if (torrentInfoName != null) {
            val torrentFileName =
                "${context?.cacheDir}/$torrentInfoName.torrent"
            val torrentFile = File(torrentFileName)
            if (torrentFile.isFile && TorrentInfo(torrentFile).isValid) {
                // This means we have the torrent file already locally and we can skip the step of
                // obtaining the TorrentInfo from magnet link
                torrentUrl = torrentFile.toURI().toURL().toString()
            }
        }
        return torrentUrl
    }

    override fun onStreamReady(torrent: Torrent?) {
        val fileProgress = torrent?.torrentHandle?.fileProgress()
        if (fileProgress != null) updateFileProgress(fileProgress)
        val audioPlayer = AudioPlayer.getInstance() ?: return
        if (!audioPlayer.isPlaying() &&
            torrent != null &&
            currentFileIndex != -1
        ) {
            startPlaying(
                torrent.videoFile,
                currentFileIndex
            )
        }
    }

    override fun onStreamPrepared(torrent: Torrent?) {
        val fileProgress = torrent?.torrentHandle?.fileProgress()
        if (fileProgress != null) updateFileProgress(fileProgress)
        streamingTorrent = torrent
        if (torrent == null) return
        torrent.setSelectedFileIndex(0)
        torrent.startDownload()
        Util.setSequentialPriorities(torrent)
        val torrentFile = torrent.torrentHandle?.torrentFile()
            ?: throw Error("Unknown torrent file metadata")
        setMetadata(torrentFile.files())
        // Keep seeding the torrent, also after start-up or after browsing to a different Release
        val infoName = torrentInfoName ?: torrent.torrentHandle.name()
        val localContext = context
        if (localContext != null) {
            ContentSeeder.getInstance(localContext.cacheDir, localContext).add(torrentFile, infoName)
        }
    }

    override fun onStreamStopped() {
    }

    override fun onStreamStarted(torrent: Torrent?) {
        val fileProgress = torrent?.torrentHandle?.fileProgress()
        if (fileProgress != null) updateFileProgress(fileProgress)
    }

    override fun onStreamProgress(torrent: Torrent?, status: StreamStatus) {
        val torrentFile: TorrentInfo? = torrent?.torrentHandle?.torrentFile()
        if (metadata == null && torrentFile != null) setMetadata(torrentFile.files())
        val fileProgress = torrent?.torrentHandle?.fileProgress()
        if (fileProgress != null) updateFileProgress(fileProgress)
        val progress = status.progress
        val audioPlayer = AudioPlayer.getInstance()
        if (progress > 30 && audioPlayer != null && !audioPlayer.isPlaying() && torrent != null
            && currentFileIndex != -1
        ) {
            startPlaying(
                torrent.videoFile,
                currentFileIndex
            )
        }
        if (progress != prevProgress) {
            audioPlayer?.retry()
        }
        prevProgress = progress
        prevFileIndex = currentFileIndex
    }

    override fun onStreamError(torrent: Torrent?, e: Exception?) {
        Log.e("TorrentStream", "Torrent stream error")
        Toast.makeText(context, "Torrent stream error: ${e?.message}", Toast.LENGTH_LONG).show()
        e?.printStackTrace()
    }
}
