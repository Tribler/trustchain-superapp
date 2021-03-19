package com.example.musicdao.playlist

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.musicdao.MusicBaseFragment
import com.example.musicdao.MusicService
import com.example.musicdao.R
import com.example.musicdao.dialog.TipArtistDialog
import com.example.musicdao.net.ContentSeeder
import com.example.musicdao.player.AudioPlayer
import com.example.musicdao.util.Util
import com.example.musicdao.wallet.CryptoCurrencyConfig
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.PieceFinishedAlert
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert
import kotlinx.android.synthetic.main.fragment_release.*
import kotlinx.coroutines.*
import org.bitcoinj.core.Address
import java.io.File
import java.net.URLDecoder

/**
 * A release is an audio album, EP, single, etc.
 * It is related to 1 trustchain block, with the tag "publish_release" and its corresponding
 * structure.
 * It also implements a TorrentListener, which allows for configuring callbacks on download progress
 * of the magnet link that is contained in the Release TrustChain block
 */
class ReleaseFragment(
    private var magnet: String,
    private val artists: String,
    private val title: String,
    private val releaseDate: String,
    private val publisher: String,
    private val torrentInfoName: String?,
    private val sessionManager: SessionManager
) : MusicBaseFragment(R.layout.fragment_release) {
    private var metadata: FileStorage? = null
    private var tracks: MutableMap<Int, TrackFragment> = hashMapOf()
    private var currentFileIndex = -1
    private var currentTorrent: TorrentInfo? = null
    private var torrentListener: AlertListener? = null
    private var selectedToPlay = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        val torrentInfo = currentTorrent
        if (torrentInfo != null) {
            val tor = sessionManager.find(torrentInfo.infoHash())
            if (torrentListener != null && tor != null) {
                sessionManager.removeListener(torrentListener)
            }
        }
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        blockMetadata.text =
            HtmlCompat.fromHtml(
                "<b>$artists - $title<br></b>" +
                    releaseDate,
                0
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

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                retrieveTorrent()
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

    /**
     * Set-up torrent connection for current magnet link; if we already have it predownloaded we
     * will load all metadata from cache
     */
    private fun retrieveTorrent() {
        initTorrentListener()
        val saveDir = context?.cacheDir ?: return
        if (torrentInfoName == null) return
        val torrentFileName = "${context?.cacheDir}/$torrentInfoName.torrent"
        var torrentFile = File(torrentFileName)
        if (!torrentFile.isFile) {
            torrentFile = File(URLDecoder.decode(torrentFileName, "UTF-8"))
        }
        if (torrentFile.isFile && TorrentInfo(torrentFile).isValid) {
            // This means we have the torrent file already locally and we can skip the step of
            // obtaining the TorrentInfo from magnet link
            val torrentInfo = TorrentInfo(torrentFile)
            currentTorrent = torrentInfo
            if (Util.isTorrentCompleted(torrentInfo, saveDir)) {
                // All files are already downloaded so we do not need to use SessionManager
                activity?.runOnUiThread {
                    setMetadata(torrentInfo.files(), preloaded = true)
                }
                return
            }
            currentTorrent?.addTracker("udp://130.161.119.207:8000/announce")
            currentTorrent?.addTracker("http://130.161.119.207:8000/announce")
            currentTorrent?.addTracker("udp://130.161.119.207:8000")
            currentTorrent?.addTracker("http://130.161.119.207:8000")
            sessionManager.download(currentTorrent, saveDir)
        } else {
            // The torrent has not been finished yet previously, so start downloading
            val torrentInfo = fetchTorrentInfo(saveDir)
            if (torrentInfo == null) {
                return
            } else {
                currentTorrent = torrentInfo
            }
            currentTorrent?.addTracker("udp://130.161.119.207:8000/announce")
            currentTorrent?.addTracker("http://130.161.119.207:8000/announce")
            currentTorrent?.addTracker("udp://130.161.119.207:8000")
            currentTorrent?.addTracker("http://130.161.119.207:8000")
            sessionManager.download(currentTorrent, saveDir)
        }
        val torrent = sessionManager.find(currentTorrent?.infoHash())

        // Prioritize file 1
        activity?.runOnUiThread {
            setMetadata(torrent.torrentFile().files())
            updateFileProgress(torrent.fileProgress())
        }

        Util.setTorrentPriorities(torrent, false, 0, 0)
        torrent.pause()
        torrent.resume()
    }

    private fun initTorrentListener() {
        torrentListener = object : AlertListener {
            override fun types(): IntArray {
                return intArrayOf(
                    AlertType.PIECE_FINISHED.swig(),
                    AlertType.TORRENT_FINISHED.swig()
                )
            }

            override fun alert(alert: Alert<*>) {
                when (alert.type()) {
                    AlertType.PIECE_FINISHED -> {
                        val handle = (alert as PieceFinishedAlert).handle()
                        if (handle.infoHash() != currentTorrent?.infoHash()) return
                        onStreamProgress(handle)
                    }
                    AlertType.TORRENT_FINISHED -> {
                        val handle = (alert as TorrentFinishedAlert).handle()
                        if (handle.infoHash() != currentTorrent?.infoHash()) return
                        onStreamProgress(handle)
                    }
                    else -> {
                    }
                }
            }
        }
        sessionManager.addListener(torrentListener)
    }

    private fun fetchTorrentInfo(saveDir: File): TorrentInfo? {
        magnet = Util.addTrackersToMagnet(magnet)
        val torrentData =
            sessionManager.fetchMagnet(magnet, 100) ?: return null // 100 second time-out for
        // fetching the TorrentInfo metadata from peers, when no torrent file is available locally
        val torrentInfo = TorrentInfo.bdecode(torrentData)
        ContentSeeder.getInstance(saveDir, sessionManager)
            .saveTorrentInfoToFile(torrentInfo, torrentInfo.name())
        return torrentInfo
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
        if (this.metadata != null) return
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
                transaction2.commit()

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

        val downloadedTor = currentTorrent ?: return
        val handle = sessionManager.find(downloadedTor.infoHash())
        // Put high priority on the first couple of pieces from the selected track
        if (handle != null) {
            val pieceIndex = Util.calculatePieceIndex(currentFileIndex, downloadedTor)
            Util.setTorrentPriorities(handle, false, pieceIndex, currentFileIndex)
        }
        val fileToPlay =
            File("${context?.cacheDir}/${downloadedTor.files().filePath(currentFileIndex)}")
        // It is not a streaming torrent, and therefore we can conclude we already have
        // it locally so we can just start playing it
        if (fileToPlay.isFile && fileToPlay.length() > 200 * 1024) {
            GlobalScope.launch { getRecommenderCommunity().recommendStore.updateLocalFeatures(fileToPlay) }
            startPlaying(fileToPlay, currentFileIndex)
        } else {
            AudioPlayer.getInstance()
                ?.setTrackInfo(
                    "Buffering track: " + Util.checkAndSanitizeTrackNames(
                        downloadedTor.files().fileName(currentFileIndex)
                    )
                )
        }
    }

    /**
     * For each track in the list, try to update its progress bar UI state which corresponds to the
     * percentage of downloaded pieces from that track
     */
    private fun updateFileProgress(fileProgressArray: LongArray) {
        fileProgressArray.forEachIndexed { index, fileProgress ->
            val progress = Util.calculateDownloadProgress(fileProgress, metadata?.fileSize(index))
            tracks[index]?.setDownloadProgress(progress)
        }
    }

    private fun startPlaying(file: File, index: Int) {
        val audioPlayer = AudioPlayer.getInstance()
        audioPlayer?.setAudioResource(file, index)
        val trackInfo = Util.checkAndSanitizeTrackNames(file.name)
        audioPlayer?.setTrackInfo(trackInfo ?: "")
    }

    /**
     * Update the UI with the latest state of the selected TorrentHandle
     */
    fun onStreamProgress(torrentHandle: TorrentHandle) {
        val saveDir = context?.cacheDir ?: return
        val fileIndex = currentFileIndex
        val currentProgress = torrentHandle.fileProgress()
        if (currentProgress != null) updateFileProgress(currentProgress)

        if (!currentProgress.indices.contains(fileIndex)) return
        // Progress, measured in downloaded bytes
        val currentFileProgress =
            currentProgress[currentFileIndex]
        val audioPlayer = AudioPlayer.getInstance()
        val audioFile = File(
            torrentHandle.torrentFile().files().filePath(currentFileIndex, saveDir.absolutePath)
        )
        if (!audioFile.isFile) return
        // If we selected a file to play but it is not playing, start playing it after 30% progress
        if (currentFileProgress > 600 * 1024 && audioPlayer != null && !audioPlayer.isPlaying() &&
            currentFileIndex != -1 && selectedToPlay != currentFileIndex
        ) {
            selectedToPlay = currentFileIndex
            startPlaying(
                audioFile,
                currentFileIndex
            )
        }
    }
}
