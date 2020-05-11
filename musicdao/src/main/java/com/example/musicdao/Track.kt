package com.example.musicdao

import android.content.Context
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import com.example.musicdao.net.AudioPlayer
import com.example.musicdao.net.MusicService
import com.example.musicdao.net.SeekProgress
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import kotlinx.android.synthetic.main.music_app_main.*
import kotlinx.android.synthetic.main.music_app_main.view.*
import kotlinx.android.synthetic.main.music_app_main.view.bufferInfo

//The size of each block of the file to show progress
const val BLOCK_SIZE = 4 * 256L * 1024L

class Track(context: Context, magnet: String, name: String, private val index: Int, release: Release, private val musicService: MusicService) : TableRow(context) {
    private val bufferInfo: TextView = TextView(context)
//    public val progressBar: ProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
//        get() = field
    private val nameView: TextView = TextView(context)
    private val indexView: TextView = TextView(context)
    private val playButton: ImageButton = ImageButton(context)
//    private val seekProgress: SeekProgress = SeekProgress(context)

    //Initialize all UI elements
    init {
        val tableLayoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        tableLayoutParams.setMargins(5,5,5,5)
        this.layoutParams = tableLayoutParams
        val rowParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        playButton.layoutParams = rowParams
        playButton.setImageResource(android.R.drawable.ic_media_play)
        this.addView(playButton)
        indexView.layoutParams = rowParams
        indexView.text = index.toString()
        this.addView(indexView)
        nameView.layoutParams = rowParams
        nameView.text = name
        this.addView(nameView)
//        progressBar.progress = 50
//        progressBar.layoutParams = rowParams
//        this.addView(progressBar)

//        val seekProgressTableLayout = TableLayout(context)
//        seekProgressTableLayout.layoutParams = TableRow.LayoutParams(200, TableRow.LayoutParams.WRAP_CONTENT)

//        seekProgressTableLayout.addView(seekProgress)

//        this.addView(seekProgressTableLayout)

        playButton.setOnClickListener {
            release.selectTrackAndDownload(index)
        }
    }

    public fun selectToPlay(torrent: Torrent) {
        AudioPlayer.getInstance(context, musicService).prepareNextTrack()
        musicService.bufferInfo.text = "Selected: ${torrent.videoFile.nameWithoutExtension}, searching for peers"
//        var finalFileSize = torrent.torrentHandle.torrentFile().files().fileSize(index)
//        finalFileSize -= (finalFileSize % BLOCK_SIZE)
//        val blockAmount = finalFileSize / BLOCK_SIZE
//        seekProgress.createSquares(blockAmount.toInt())
    }

    public fun handleDownloadProgress(torrent: Torrent, status: StreamStatus) {
        musicService.progressBar.progress = status.progress.toInt()
        musicService.bufferInfo.text = "Selected: ${torrent.videoFile.nameWithoutExtension}, buffer progress: ${status.bufferProgress}%"
//        var finalFileSize = torrent.torrentHandle.torrentFile().files().fileSize(index)
//        var intermediateSize = finalFileSize
//        finalFileSize -= (finalFileSize % BLOCK_SIZE)
//        var index = 0
//        while (intermediateSize - BLOCK_SIZE >= 0) {TODO
//            index++
//            intermediateSize -= BLOCK_SIZE
//            if (torrent.hasBytes(intermediateSize)) {
//                this.seekProgress.setSquareDownloaded(index)
//            }
//        }
//        torrent.setInterestedBytes((interestedFraction * finalFileSize).toLong())TODO future work: set interested fraction. Needs testing
//        if (status.bufferProgress < 100) {
//            this.bufferInfo.text =
//                "Downloading torrent: ${torrent.videoFile.name}\n Torrent state: BUFFERING\n ${status.bufferProgress}%"
//        } else {
//            this.bufferInfo.text =
//                "Downloading torrent: ${torrent.videoFile.name}\n Torrent state: STREAM READY\n Total file progress: ${status.progress}%"
//        }
    }
}
