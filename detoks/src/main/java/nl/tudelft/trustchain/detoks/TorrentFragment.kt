package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import com.frostwire.jlibtorrent.FileStorage
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.android.synthetic.main.fragment_torrent.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.ui.BaseFragment


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

        val errorTV = view.findViewById<TextView>(R.id.errorTextView)

        val args = arguments
        var passedName = args?.getString("video_name")
        if (passedName == null) {
            passedName = args?.getString("torrent_name")
        }
        var torrent: TorrentHandle? = null
        for (torrentHandle in torrentManager.getListOfTorrents()) {
            if (torrentHandle.name().equals(passedName)) {
                torrent = torrentHandle
                break
            }
            val torrentInfo = torrentHandle.torrentFile()
            val fileStorage = torrentInfo.files()
            for (i in 0 until fileStorage.numFiles()-1) {
                if (fileStorage.fileName(i).equals(passedName)) {
                    torrent = torrentHandle
                    break
                }
            }
        }

        fun updateDebugPage() {
            if (torrent != null) {
                val infoHash = torrent.infoHash().toString()
                infoHashTV.text = getString(R.string.info_hash, infoHash)

                val magnetLink = torrent.makeMagnetUri()
                magnetLinkTV.text = getString(R.string.magnet_link, magnetLink)

                val downloadedBytes = torrent.status().totalDone()
                downloadedBytesTV.text = getString(R.string.downloaded_bytes, downloadedBytes)

                val torrentInfo = torrent.torrentFile()
                val fileStorage = torrentInfo.files()
                var fileStrings = ""
                for (i in 0 until fileStorage.numFiles()-1) {
                    fileStrings += "\n" + fileStorage.fileName(i)
                }
                filesTV.text = getString(R.string.files, fileStrings)

                val watchTime = torrentManager.profile.profiles[torrent.infoHash().toString()]!!.watchTime
                watchTimeTV.text = getString(R.string.watch_time, watchTime)

                val hopCount = torrentManager.profile.profiles[torrent.infoHash().toString()]!!.hopCount
                hopCountTV.text = getString(R.string.hop_count, hopCount)

                val watched = torrentManager.profile.profiles[torrent.infoHash().toString()]!!.watched
                watchedTV.text = getString(R.string.watched, watched)

                val duration = torrentManager.profile.profiles[torrent.infoHash().toString()]!!.duration
                durationTV.text = getString(R.string.duration, duration)

                val likes = torrentManager.profile.profiles[torrent.infoHash().toString()]!!.likes
                likesTV.text = getString(R.string.likes, likes)

                val timesSeen = torrentManager.profile.profiles[torrent.infoHash().toString()]!!.timesSeen
                timesSeenTV.text = getString(R.string.times_seen, timesSeen)

                val uploadDate = torrentManager.profile.profiles[torrent.infoHash().toString()]!!.uploadDate
                uploadDateTV.text = getString(R.string.upload_date, uploadDate)

            }
            else {
                errorTV.visibility = View.VISIBLE
                val errorText = "Torrent $passedName not found"
                errorTV.text = errorText

                infoHashTV.visibility = View.GONE
                magnetLinkTV.visibility = View.GONE
                downloadedBytesTV.visibility = View.GONE
                filesTV.visibility = View.GONE
                watchTimeTV.visibility = View.GONE
                hopCountTV.visibility = View.GONE
                watchedTV.visibility = View.GONE
                durationTV.visibility = View.GONE
                likesTV.visibility = View.GONE
                timesSeenTV.visibility = View.GONE
                uploadDateTV.visibility = View.GONE
            }
        }

        val handler = Handler((Looper.getMainLooper()))
        val runnable : Runnable = object : Runnable {
            override fun run() {
                if (isAdded)
                    updateDebugPage()
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(runnable,2000)


    }
}
