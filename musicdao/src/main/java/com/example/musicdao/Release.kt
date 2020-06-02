package com.example.musicdao

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.AlertListener
import com.frostwire.jlibtorrent.FileStorage
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.alerts.*
import kotlinx.android.synthetic.main.fragment_release.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import java.io.File

/**
 * A release is an audio album, EP, single, etc.
 */
class Release(
    private val magnet: String,
    private val trackLibrary: TrackLibrary,
    private val musicService: MusicService,
    private val transaction: TrustChainTransaction
) : Fragment(), AlertListener {
    private var metadata: TorrentInfo? = null
    private var tracks: MutableMap<Int, Track> = hashMapOf()
    private var currentFileIndex = -1

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
                "<b>${transaction["artists"]} - ${transaction["title"]}<br></b>" +
                    "${transaction["date"]}", 0
            )

        // Generate the UI
        val localContext = context ?: throw Error("Unobtainable context")
        // When the Release is added, it will try to fetch the metadata for the corresponding magnet
        trackLibrary.downloadMagnet(this, magnet, localContext.cacheDir)
    }

    private fun setMetadata(metadata: TorrentInfo) {
        this.metadata = metadata
        println("Metadata: $metadata")
        val num = metadata.numFiles()
        val filestorage = metadata.files()

        val allowedExtensions =
            listOf("flac", "mp3", "3gp", "aac", "mkv", "wav", "ogg", "mp4", "m4a")

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
                val track = Track(
                    fileName,
                    index,
                    this,
                    Util.readableBytes(filestorage.fileSize(index)),
                    musicService
                )

                // Add a table row (Track) to the table (Release)
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
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

        val files = this.metadata?.files() ?: return
        val fileName = files.fileName(index) ?: throw Error("Unknown file being played")

        musicService.prepareNextTrack()

        musicService.setSongArtistText("Selected: ${fileName}, searching for peers")

        val localContext = context ?: throw Error("Unobtainable context")

        val filePath = files.filePath(
            currentFileIndex,
            localContext.cacheDir.absolutePath
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
                val wantedPiece = Util.calculatePieceIndex(
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
                        val localContext = context ?: throw Error("Unobtainable context")
                        val filePath = files.filePath(
                            currentFileIndex,
                            localContext.cacheDir.absolutePath
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
                    val localContext = context ?: throw Error("Unobtainable context")
                    // The file completed is the one we were focusing on; let's play it
                    val filePath = alert.handle().torrentFile().files().filePath(
                        currentFileIndex,
                        localContext.cacheDir.absolutePath
                    )
                    startPlaying(
                        File(filePath),
                        currentFileIndex,
                        alert.handle().torrentFile().files()
                    )
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
