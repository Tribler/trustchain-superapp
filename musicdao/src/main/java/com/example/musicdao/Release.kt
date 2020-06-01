package com.example.musicdao

import android.content.Context
import android.widget.*
import androidx.core.text.HtmlCompat
import com.example.musicdao.util.TorrentUtil
import com.frostwire.jlibtorrent.AlertListener
import com.frostwire.jlibtorrent.FileStorage
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.alerts.*
import kotlinx.android.synthetic.main.fragment_trackplaying.view.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import java.io.File

/**
 * A release is an audio album, EP, single, etc.
 */
class Release(
    context: Context,
    magnet: String,
    trackLibrary: TrackLibrary,
    private val musicService: MusicService,
    transaction: TrustChainTransaction
) : TableLayout(context), AlertListener {
    private var metadata: TorrentInfo? = null
    private var tracks: MutableMap<Int, Track> = hashMapOf()
    private var currentFileIndex = -1
    private var currentFile: File? = null
    private var fetchingMetadataRow = TableRow(context)
    private var metadataRow = TableRow(context)

    init {
        // Generate the UI
        this.setColumnStretchable(2, true)
        this.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val blockMetadata = TextView(context)
        blockMetadata.text =
            HtmlCompat.fromHtml(
                "Signed block with release:<br>$transaction\n<br><b>" +
                    "${transaction["artists"]} - ${transaction["title"]}<br>" +
                    "Released at ${transaction["date"]}</b>", 0
            )
        blockMetadata.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        metadataRow.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        val params = blockMetadata.layoutParams as TableRow.LayoutParams
        params.span = 4
        blockMetadata.layoutParams = params
        metadataRow.addView(blockMetadata)
        this.addView(metadataRow)

        // When the Release is added, it will try to fetch the metadata for the corresponding magnet
        trackLibrary.downloadMagnet(this, magnet, context.cacheDir)
    }

    private fun setMetadata(metadata: TorrentInfo) {
        this.metadata = metadata
        println("Metadata: $metadata")
        val num = metadata.numFiles()
        val filestorage = metadata.files()

        val allowedExtensions =
            listOf<String>("flac", "mp3", "3gp", "aac", "mkv", "wav", "ogg", "mp4", "m4a")

        for (index in 0 until num) {
            val fileName = filestorage.fileName(index)
            println("Discovered track $fileName")
            var found = false
            for (s in allowedExtensions) {
                if (fileName.endsWith(s)) {
                    found = true
                }
            }
            if (found) {
                val track = Track(context, fileName, index, this, musicService)
                musicService.runOnUiThread {
                    this.addView(track)
                }
                tracks[index] = track
            }
        }
    }

    /**
     * Select a track from the Release and start downloading and seeding it
     */
    fun selectTrackAndDownload(index: Int) {
        currentFileIndex = index

        val files = this.metadata?.files() ?: return
        val fileName = files.fileName(index) ?: throw Error("Unknown file being played")

        musicService.prepareNextTrack()

        musicService.setSongArtistText("Selected: ${fileName}, searching for peers")

        val filePath = files.filePath(
            currentFileIndex,
            context.cacheDir.absolutePath
        )
        val audioFile = File(filePath ?: "")

        // TODO needs to have a solid check whether the file was already downloaded before
        if (audioFile.isFile && audioFile.length() == files.fileSize(currentFileIndex)
        ) {
            startPlaying(audioFile, index, files)
        }
    }

    override fun alert(alert: Alert<*>) {
        when (alert.type()) {
            AlertType.ADD_TORRENT -> {
                (alert as AddTorrentAlert).handle().resume()
                println("Torrent added")
            }
            AlertType.PIECE_FINISHED -> {
                val handle = (alert as PieceFinishedAlert).handle()
                updateFileProgress(handle.fileProgress())
                val wantedPiece = TorrentUtil.calculatePieceIndex(
                    currentFileIndex,
                    handle.torrentFile()
                )
                // Set the currently selected file (if any) to the highest priority
                // Also, we want the first couple of pieces of a file to have high priority so we
                // can start playing the audio early
                if (currentFileIndex != -1) {
                    if (handle.filePriority(currentFileIndex) != Priority.SIX) {
                        handle.filePriority(currentFileIndex, Priority.SIX)
                    }
                    if (handle.piecePriority(wantedPiece) != Priority.SIX) {
                        handle.piecePriority(wantedPiece, Priority.SIX)
                    }
                }
                if (handle.fileProgress()[currentFileIndex] > 1024 * 1024 * 2) {
                    if (handle.havePiece(wantedPiece)) {
                        // The file completed is the one we were focusing on; let's play it
                        val files = alert.handle().torrentFile().files()
                        val filePath = files.filePath(
                            currentFileIndex,
                            context.cacheDir.absolutePath
                        )
                        startPlaying(File(filePath), currentFileIndex, files)
                    }
                }
            }
            AlertType.METADATA_RECEIVED -> {
                val handle = (alert as MetadataReceivedAlert).handle()
                setMetadata(handle.torrentFile())
            }
            AlertType.METADATA_FAILED -> {
                Toast.makeText(context, "Error fetching torrent metadata", Toast.LENGTH_SHORT)
                    .show()
            }
            AlertType.FILE_COMPLETED -> {
                alert as FileCompletedAlert
                tracks[alert.index()]?.setCompleted()
                if (alert.index() == currentFileIndex) {
                    // The file completed is the one we were focusing on; let's play it
                    val filePath = alert.handle().torrentFile().files().filePath(
                        currentFileIndex,
                        context.cacheDir.absolutePath
                    )
                    startPlaying(File(filePath), currentFileIndex, alert.handle().torrentFile().files())
                }
            }
            else -> {
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

    @Synchronized
    private fun startPlaying(file: File, index: Int, files: FileStorage) {
        musicService.startPlaying(file, index, files)
    }

    override fun types(): IntArray? {
        return null
    }

}
