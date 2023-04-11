package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.View
import android.widget.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.ui.BaseFragment


class TorrentFragment : BaseFragment(R.layout.fragment_torrent) {

    private lateinit var torrentManager: TorrentManager
    val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        torrentManager = TorrentManager.getInstance(requireActivity())

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var peerText = view.findViewById<TextView>(R.id.peerText)
        var isSeedingText = view.findViewById<TextView>(R.id.isSeedingText)

        val peerId = "Peer ID: ${deToksCommunity.myPeer.mid}"
        peerText.text = peerId

        var torrent = torrentManager.getListOfTorrents()[0]
        var isSeeding = torrent.status().isSeeding()
        var seedingStatus = if (isSeeding) {
            "Seeding status: Seeding"
        } else {
            "Seeding status: Not seeding"
        }
        isSeedingText.text = seedingStatus
    }
}
