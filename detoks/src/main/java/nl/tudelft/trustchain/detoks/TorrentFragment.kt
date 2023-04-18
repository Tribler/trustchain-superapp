package nl.tudelft.trustchain.detoks

import android.os.Bundle
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
        val totalWatchTimeTV = view.findViewById<TextView>(R.id.totalWatchTimeTextView)
        val hopCountTV = view.findViewById<TextView>(R.id.hopCountTextView)
        val watchedTV = view.findViewById<TextView>(R.id.watchedTextView)
        val durationTV = view.findViewById<TextView>(R.id.durationTextView)
        val likesTV = view.findViewById<TextView>(R.id.likesTextView)
        val timesSeenTV = view.findViewById<TextView>(R.id.timesSeenTextView)
        val uploadDateTV = view.findViewById<TextView>(R.id.uploadDateTextView)

        val errorTV = view.findViewById<TextView>(R.id.errorTextView)

        val passedName = "electricsheep-flock-248-7500-9"

        var torrent: TorrentHandle? = null
        for (torrentHandle in torrentManager.getListOfTorrents()) {
            if (torrentHandle.name().equals(passedName)) {
                torrent = torrentHandle
            }
        }

        if (torrent != null) {
            val infoHashText = "Info hash: " + torrent.infoHash().toString()
            infoHashTV.text = infoHashText

            val magnetLinkText = "Magnet link: " + torrent.makeMagnetUri()
            magnetLinkTV.text = magnetLinkText

            val downloadedBytesText = "Bytes downloaded: " + torrent.status().totalDone().toString()
            downloadedBytesTV.text = downloadedBytesText

            val torrentInfo = torrent.torrentFile()
            val fileStorage = torrentInfo.files()
            var fileStrings = "Files:"
            for (i in 0 until fileStorage.numFiles()-1) {
                fileStrings += "\n" + fileStorage.fileName(i)
            }
            filesTV.text = fileStrings

            val totalWatchTimeText = "Total watch time: " +
                torrentManager.profile.profiles[torrent.infoHash().toString()]!!.watchTime
            totalWatchTimeTV.text = totalWatchTimeText

            val hopCountText = "Hop count: " +
                torrentManager.profile.profiles[torrent.infoHash().toString()]!!.hopCount
            hopCountTV.text = hopCountText

            val watchedText = "Watched: " +
                torrentManager.profile.profiles[torrent.infoHash().toString()]!!.watched
            watchedTV.text = watchedText

            val durationText = "Duration: " +
                torrentManager.profile.profiles[torrent.infoHash().toString()]!!.duration
            durationTV.text = durationText

            val likesText = "Likes: " +
                torrentManager.profile.profiles[torrent.infoHash().toString()]!!.likes
            likesTV.text = likesText

            val timesSeenText = "Times seen: " +
                torrentManager.profile.profiles[torrent.infoHash().toString()]!!.timesSeen
            timesSeenTV.text = timesSeenText

            val uploadDateText = "Upload date: " +
                torrentManager.profile.profiles[torrent.infoHash().toString()]!!.uploadDate
            uploadDateTV.text = uploadDateText

        }
        else {
            errorTV.visibility = View.VISIBLE
            val errorText = "Torrent $passedName not found"
            errorTV.text = errorText
        }




    }
}
