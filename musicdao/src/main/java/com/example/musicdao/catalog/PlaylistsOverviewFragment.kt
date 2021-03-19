package com.example.musicdao.catalog

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.musicdao.MusicBaseFragment
import com.example.musicdao.MusicService
import com.example.musicdao.R
import com.example.musicdao.dialog.SubmitReleaseDialog
import com.example.musicdao.util.Util
import com.example.musicdao.wallet.WalletService
import com.frostwire.jlibtorrent.Sha1Hash
import kotlinx.android.synthetic.main.fragment_release_overview.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import java.io.File

/**
 * A screen showing an overview of playlists to browse through
 */
class PlaylistsOverviewFragment : MusicBaseFragment(R.layout.fragment_release_overview) {
    private var releaseRefreshCount = 0
    private var lastReleaseBlocksSize = -1
    private var lastSwarmHealthMapSize = -1
    private var searchQuery = ""
    private val maxPlaylists = 100 // Max playlists to show

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchQuery = arguments?.getString("filter", "") ?: ""
        if (searchQuery != "") {
            getMusicCommunity().performRemoteKeywordSearch(searchQuery)
            activity?.title = "Search results"
            setMenuVisibility(false)
            setHasOptionsMenu(false)
        } else {
            setHasOptionsMenu(true)
        }

        lastReleaseBlocksSize = -1
        lastSwarmHealthMapSize = -1
        releaseRefreshCount = 0

        lifecycleScope.launchWhenCreated {
            while (isActive && isAdded && !isDetached) {
                if (activity is MusicService && debugText != null) {
                    debugText.text = (activity as MusicService).getStatsOverview()
                }
                if (releaseRefreshCount < 3) {
                    showAllReleases()
                }
                delay(3000)
            }
        }

        swipeRefresh.setOnRefreshListener {
            showAllReleases()
            swipeRefresh.isRefreshing = false
        }

        addPlaylistFab.setOnClickListener {
            showCreateReleaseDialog()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Debug button is a simple toggle for a connectivity stats display
            R.id.action_debug -> {
                if (debugText != null) {
                    if (debugText.visibility == View.VISIBLE) {
                        debugText.visibility = View.GONE
                    } else {
                        debugText.visibility = View.VISIBLE
                    }
                }
                true
            }
            R.id.action_wallet -> {
                findNavController().navigate(R.id.walletFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * List all the releases that are currently loaded in the local trustchain database. If keyword
     * search is enabled (searchQuery variable is set) then it also filters the database
     */
    private fun showAllReleases() {
        val swarmHealthMap = (activity as MusicService).swarmHealthMap
        val releaseDataMap = mutableMapOf<TrustChainBlock, Int>()
        val releaseBlocks = getMusicCommunity().database.getBlocksWithType("publish_release")
        if (releaseBlocks.size == lastReleaseBlocksSize &&
            swarmHealthMap.size == lastSwarmHealthMapSize
        ) {
            return
        }
        for (block in releaseBlocks) {
            // If we know the amount of seeders for the corresponding block, we add it; otherwise we
            // assume the amount of seeders is 0
            var numSeeds = 0
            var numPeers = 0
            val magnet = block.transaction["magnet"]
            if (magnet is String) {
                val infoHash = Util.extractInfoHash(magnet)
                if (infoHash is Sha1Hash) {
                    numSeeds = swarmHealthMap[infoHash]?.numSeeds?.toInt() ?: 0
                    numPeers = swarmHealthMap[infoHash]?.numPeers?.toInt() ?: 0
                }
            }
            releaseDataMap[block] = numSeeds + numPeers
        }
        val sortedMap = releaseDataMap
            .toList()
            .sortedByDescending { (_, value) -> value }
            .toMap()

        lastReleaseBlocksSize = releaseBlocks.size
        lastSwarmHealthMapSize = swarmHealthMap.size
        if (release_overview_layout is ViewGroup) {
            activity?.runOnUiThread {
                release_overview_layout.removeAllViews()
            }
        }
        refreshReleaseBlocks(sortedMap)
    }

    /**
     * @param releaseBlocks map of: release block, number of (known) seeders
     */
    fun refreshReleaseBlocks(releaseBlocks: Map<TrustChainBlock, Int>): Int {
        var count = 0
        for ((block, connectivity) in releaseBlocks) {
            if (count == maxPlaylists) return count
            val magnet = block.transaction["magnet"]
            val title = block.transaction["title"]
            val torrentInfoName = block.transaction["torrentInfoName"]
            val publisher = block.transaction["publisher"]
            if (magnet is String && magnet.length > 0 && title is String && title.length > 0 &&
                torrentInfoName is String && torrentInfoName.length > 0 && publisher is String &&
                publisher.length > 0
            ) {
                val coverArt = Util.findCoverArt(
                    File(
                        context?.cacheDir?.path + "/" + Util.sanitizeString(torrentInfoName)
                    )
                )
                val transaction = activity?.supportFragmentManager?.beginTransaction()
                val coverFragment = PlaylistCoverFragment(block, connectivity, coverArt)
                if (coverFragment.filter(searchQuery)) {
                    transaction?.add(R.id.release_overview_layout, coverFragment, "releaseCover")
                    if (loadingReleases?.visibility == View.VISIBLE) {
                        loadingReleases.visibility = View.GONE
                    }
                    count += 1
                }
                activity?.runOnUiThread {
                    transaction?.commitAllowingStateLoss()
                }
            }
        }
        if (count != 0) {
            releaseRefreshCount += 1
        }
        return count
    }

    /**
     * Show a form dialog which asks to add metadata for a new Release (album title, release date,
     * track files etc)
     */
    private fun showCreateReleaseDialog() {
        SubmitReleaseDialog(this)
            .show(childFragmentManager, "Submit metadata")
    }

    /**
     * After the user inserts some metadata for the release to be published, this function is called
     * to create the proposal block
     */
    fun publish(
        magnet: String,
        title: String,
        artists: String,
        releaseDate: String,
        torrentInfoName: String
    ) {
        val myPeer = IPv8Android.getInstance().myPeer
        val transaction = mutableMapOf<String, String>(
            "magnet" to magnet,
            "title" to title,
            "artists" to artists,
            "date" to releaseDate,
            "torrentInfoName" to torrentInfoName
        )
        val walletDir = context?.cacheDir
        if (walletDir != null) {
            val musicWallet = WalletService.getInstance(walletDir, (activity as MusicService))
            transaction["publisher"] = musicWallet.publicKey()
        }
        val trustchain = getMusicCommunity()
        trustchain.createProposalBlock("publish_release", transaction, myPeer.publicKey.keyToBin())
    }
}
