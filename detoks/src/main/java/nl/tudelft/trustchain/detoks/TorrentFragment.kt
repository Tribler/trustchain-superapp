package nl.tudelft.trustchain.detoks

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import com.frostwire.jlibtorrent.TorrentHandle
import nl.tudelft.trustchain.common.ui.BaseFragment
import java.text.SimpleDateFormat
import java.util.*

class TorrentFragment : BaseFragment(R.layout.fragment_torrent) {

    private lateinit var torrentManager: TorrentManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        torrentManager = TorrentManager.getInstance(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val infoHashTV = view.findViewById<TextView>(R.id.infoHashTextView)
        val magnetLinkTV = view.findViewById<TextView>(R.id.magnetLinkTextView)
        val filesTV = view.findViewById<TextView>(R.id.filesTextView)
        val downloadedBytesTV = view.findViewById<TextView>(R.id.downloadedBytesTextView)
        val watchTimeTV = view.findViewById<TextView>(R.id.watchTimeTextView)
        val hopCountTV = view.findViewById<TextView>(R.id.hopCountTextView)
        val watchedTV = view.findViewById<TextView>(R.id.watchedTextView)
        val durationTV = view.findViewById<TextView>(R.id.durationTextView)
        val likesTV = view.findViewById<TextView>(R.id.likesTextView)
        val timesSeenTV = view.findViewById<TextView>(R.id.timesSeenTextView)
        val uploadDateTV = view.findViewById<TextView>(R.id.uploadDateTextView)

        val args = arguments
        var passedName = args?.getString("video_name")
        if (passedName == null) {
            passedName = args?.getString("torrent_name")
        }

        val torrent: TorrentHandle?
        var fileName: TorrentManager.TorrentHandler? = torrentManager.torrentFiles.find { it.fileName == passedName }
        if (fileName == null) fileName = torrentManager.torrentFiles.find { it.torrentName == passedName }
        torrent = fileName!!.handle
        val idx = fileName.fileIndex

        fun updateDebugPage() {
            val infoHash = torrent.infoHash().toString()
            infoHashTV.text = getString(R.string.info_hash, infoHash)

            val magnetLink = torrent.makeMagnetUri()
            magnetLinkTV.text = getString(R.string.magnet_link, magnetLink)

            val downloadedBytes = torrent.status().totalDone()
            downloadedBytesTV.text = getString(R.string.downloaded_bytes, downloadedBytes)

            val torrentInfo = torrent.torrentFile()
            val fileStorage = torrentInfo.files()
            var filesString = ""
            for (i in 0 until fileStorage.numFiles()-1) {
                filesString += "\n" + fileStorage.fileName(i)
            }
            filesTV.text = getString(R.string.files, filesString)

            val profile = torrentManager.profile.profiles[TorrentManager.createKey(torrent.infoHash(), idx)]
            val watchTime = profile!!.watchTime
            watchTimeTV.text = getString(R.string.watch_time, watchTime)

            val hopCount = profile.hopCount
            hopCountTV.text = getString(R.string.hop_count, hopCount)

            val watched = profile.watched
            watchedTV.text = getString(R.string.watched, watched)

            val duration = profile.duration
            durationTV.text = getString(R.string.duration, duration)

            val likes = profile.likes
            likesTV.text = getString(R.string.likes, likes)

            val timesSeen = profile.timesSeen
            timesSeenTV.text = getString(R.string.times_seen, timesSeen)

            val uploadDate = Date(profile.uploadDate)
            
            @SuppressLint("SimpleDateFormat")
            val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
            val uploadDateStr = format.format(uploadDate)
            uploadDateTV.text = getString(R.string.upload_date, uploadDateStr)
        }

        val handler = Handler((Looper.getMainLooper()))
        val runnable : Runnable = object : Runnable {
            override fun run() {
                if (isAdded)
                    updateDebugPage()
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(runnable)


    }
}
