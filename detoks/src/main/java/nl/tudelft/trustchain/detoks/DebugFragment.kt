package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper

class DetoksDebugFragment : BaseFragment(R.layout.fragment_detoks_debug) {

    private val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
    private lateinit var torrentManager: TorrentManager
    private val strategyDescr = mapOf(0 to "Random (randomly chosen torrent)",
        1 to "Hot (watchtime in descending order with time cut-off)",
        2 to "Rising (watchtime in descending order with lower time cut-off than hot)",
        3 to "New (upload date from newest to oldest)",
        4 to "Top (number of likes in descending order)",
        5 to "Hopcount (hopcount in descending order)",
        6 to "Highest watch time (watchtime in descending order)",
        7 to "Lowest watch time (watchtime in ascending order)")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        torrentManager = TorrentManager.getInstance(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val estimatedNetworkSizeTV = view.findViewById<TextView>(R.id.estimatedNetworkSizeTextView)
        val nbConnectedPeersTV = view.findViewById<TextView>(R.id.nbConnectedPeersTextView)
        val listConnectedPeersTV = view.findViewById<TextView>(R.id.listConnectedPeersTextView)
        val top3LeechingTorrentsTV = view.findViewById<TextView>(R.id.top3LeechingTorrentsTextView)
        val top3SeedingTorrentsTV = view.findViewById<TextView>(R.id.top3SeedingTorrentsTextView)
        val seedingStatusTV = view.findViewById<TextView>(R.id.seedingStatusTextView)
        val walletTokensTV = view.findViewById<TextView>(R.id.walletTokensTextView)
        val peerIdTV = view.findViewById<TextView>(R.id.peerIdTextView)

        fun updateDebugPage() {
            val estimatedNetworkSize = NetworkSizeGossiper.networkSizeEstimate
            estimatedNetworkSizeTV.text = getString(R.string.estimated_network_size, estimatedNetworkSize)

            val nbConnectedPeers = deToksCommunity.getPeers().size
            nbConnectedPeersTV.text = getString(R.string.nb_connected_peers, nbConnectedPeers)

            val connectedPeers = deToksCommunity.getPeers()
            var listConnectedPeers = ""
            for (peer in connectedPeers) {
                listConnectedPeers += "\n" + peer.mid
            }
            listConnectedPeersTV.text = getString(R.string.list_connected_peers, listConnectedPeers)

            val torrentsList = torrentManager.getListOfTorrents()
            val currentIndex = torrentManager.getCurrentIndex()
            var top3LeechingTorrentsString = ""

            val lower = currentIndex%torrentsList.size
            val upper = (currentIndex+3)%torrentsList.size
            if (lower == upper) {
                top3LeechingTorrentsString += "\n" + torrentsList[lower].name()
            }
            for (i in lower until upper) {
                top3LeechingTorrentsString += "\n" + torrentsList[i].name()
            }
            val leechingStrategy = torrentManager.strategies.leechingStrategy
            top3LeechingTorrentsString += "\n" + "Leeching strategy: " + strategyDescr[leechingStrategy]
            top3LeechingTorrentsTV.text = getString(R.string.top_3_leeching_torrents, top3LeechingTorrentsString)

            val seedingTorrentsList = torrentManager.getListOfSeedingTorrents()
            var top3SeedingTorrentsString = ""
            if (seedingTorrentsList.isEmpty()) {
                top3SeedingTorrentsString = "No seeding torrents yet."
            }
            else {
                for (i in 0 until 3.coerceAtMost(torrentsList.size)) {
                    top3SeedingTorrentsString += "\n" + seedingTorrentsList[i].name()
                }
                val seedingStrategy = torrentManager.strategies.seedingStrategy
                top3SeedingTorrentsString += "\n" + "Seeding strategy: " + strategyDescr[seedingStrategy]
            }
            top3SeedingTorrentsTV.text = getString(R.string.top_3_seeding_torrents, top3SeedingTorrentsString)

            val seedingStatus = torrentManager.strategies.isSeeding
            seedingStatusTV.text = getString(R.string.seeding_status, seedingStatus)

            val walletTokens = deToksCommunity.getBalance()
            walletTokensTV.text = getString(R.string.wallet_tokens, walletTokens)

            val peerId = deToksCommunity.myPeer.mid
            peerIdTV.text = getString(R.string.peer_id, peerId)
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
