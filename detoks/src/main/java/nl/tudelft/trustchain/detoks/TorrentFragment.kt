package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.View
import android.widget.*
import com.frostwire.jlibtorrent.TorrentHandle
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
        var infoHashTV = view.findViewById<TextView>(R.id.infoHashTextView)
        var magnetLinkTV = view.findViewById<TextView>(R.id.magnetLinkTextView)
        var downloadedBytesTV = view.findViewById<TextView>(R.id.downloadedBytesTextView)

        var passedName = "electricsheep-flock-248-7500-9"

        lateinit var torrent: TorrentHandle
        for (torrentHandle in torrentManager.getListOfTorrents()) {
            if (torrentHandle.name().equals(passedName)) {
                torrent = torrentHandle
            }
        }

        var infoHashText = "Info hash: " + torrent.infoHash().toString()
        infoHashTV.text = infoHashText

        var magnetLinkText = "Magnet link: " + torrent.makeMagnetUri()
        magnetLinkTV.text = magnetLinkText

        var downloadedBytesText = torrent.status().totalDone().toString() + " Bytes downloaded"
        downloadedBytesTV.text = downloadedBytesText


    }
}
