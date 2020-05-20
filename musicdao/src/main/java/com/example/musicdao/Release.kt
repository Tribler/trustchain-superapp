package com.example.musicdao

import android.content.Context
import android.text.Html
import android.widget.*
import com.frostwire.jlibtorrent.AlertListener
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.alerts.*
import kotlinx.android.synthetic.main.music_app_main.*
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
//    private lateinit var torrent: Torrent

    init {
        // Generate the UI
        this.setColumnStretchable(2, true)
        this.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val blockMetadata = TextView(context)
        blockMetadata.text =
            Html.fromHtml(
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
        val fileName =
            this.metadata?.files()?.fileName(index) ?: throw Error("Unknown file being played")
        AudioPlayer.getInstance(context, musicService).prepareNextTrack()
        musicService.runOnUiThread {
            musicService.bufferInfo.text = "Selected: ${fileName}, searching for peers"
        }

        val filePath = this.metadata?.files()?.filePath(
            currentFileIndex,
            context.cacheDir.absolutePath
        )
        val audioFile = File(filePath ?: "")

        // TODO needs to have a solid check whether the file was already downloaded before
        if (audioFile.isFile && audioFile.length() == this.metadata?.files()
                ?.fileSize(currentFileIndex)
        ) {
            startPlaying(audioFile)
        }
    }

    override fun alert(alert: Alert<*>) {
        when (alert.type()) {
            AlertType.ADD_TORRENT -> {
                (alert as AddTorrentAlert).handle().resume()
                println("Torrent added")
            }
            AlertType.BLOCK_FINISHED -> {
                val handle = (alert as BlockFinishedAlert).handle()
                updateFileProgress(handle.fileProgress())
                // Set the currently selected file (if any) to the highest priority
                if (currentFileIndex != -1) {
                    if (handle.filePriority(currentFileIndex) != Priority.SEVEN) {
                        handle.filePriority(currentFileIndex, Priority.SEVEN)
                    }
                }
            }
            AlertType.PIECE_FINISHED -> {

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
                val handle = (alert as FileCompletedAlert).handle()
                // The file completed is the one we were focusing on; let's play it
                updateFileProgress(handle.fileProgress())
                if (alert.index() == currentFileIndex) {
                    val filePath = alert.handle().torrentFile().files().filePath(
                        currentFileIndex,
                        context.cacheDir.absolutePath
                    )
                    startPlaying(File(filePath))
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
            tracks[index]?.setDownloadProgress(fileProgress,
                metadata?.files()?.fileSize(index))
        }
    }

    @Synchronized
    private fun startPlaying(file: File) {
        musicService.runOnUiThread {
            musicService.bufferInfo.text =
                "Selected: ${file.nameWithoutExtension}"
        }
        AudioPlayer.getInstance(context, musicService).setAudioResource(file)
    }

    override fun types(): IntArray? {
        return null
    }

    /**
     * When the track is buffered, this is called.
     * Then set the audio resource on the audioplayer.
     */
//    override fun onStreamReady(torrent: Torrent) {
//        println("Stream ready")
//        AudioPlayer.getInstance(context, musicService).setAudioResource(torrent.videoFile)
//    }
//
//    /**
//     * This is called when the metadata is fetched. Then we can render a table with all
//     * of the songs
//     */
//    override fun onStreamPrepared(torrent: Torrent) {
//        // TODO add a check here for whether this torrent is the torrent of this Release
//        println("Stream prepared")
//        this.removeView(fetchingMetadataRow)
//        this.torrent = torrent
//    }
//
//    override fun onStreamStopped() {
//        println("Stream stopped")
//    }
//
//    override fun onStreamStarted(torrent: Torrent) {
//        println("Stream started: " + torrent.videoFile)
//    }
//
//    /**
//     * This is called when the torrent client downloaded a torrent piece.
//     */
//    override fun onStreamProgress(torrent: Torrent, status: StreamStatus) {
//        if (currentFileIndex == -1) return
//        val track = tracks[currentFileIndex]
//        track.handleDownloadProgress(torrent, status)
//    }
//
//    override fun onStreamError(torrent: Torrent, e: Exception) {
//        e.printStackTrace()
//    }

}
