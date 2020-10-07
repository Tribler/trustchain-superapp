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
import kotlinx.android.synthetic.main.fragment_release_overview.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

class ReleaseOverviewFragment : MusicBaseFragment(R.layout.fragment_release_overview) {
    private var lastReleaseBlocksSize = -1
    private var searchQuery = ""
    private val maxReleases = 10

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

        lifecycleScope.launchWhenCreated {
            while (isActive) {
                if (activity is MusicService && debugText != null) {
                    debugText.text = (activity as MusicService).getStatsOverview()
                }
                showAllReleases()
                delay(3000)
            }
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
        val releaseBlocks = getMusicCommunity().database.getBlocksWithType("publish_release")
        if (releaseBlocks.size == lastReleaseBlocksSize) {
            return
        }
        lastReleaseBlocksSize = releaseBlocks.size
        if (release_overview_layout is ViewGroup) {
            activity?.runOnUiThread {
                release_overview_layout.removeAllViews()
            }
        }
        refreshReleaseBlocks(releaseBlocks)
    }

    fun refreshReleaseBlocks(releaseBlocks: List<TrustChainBlock>): Int {
        var count = 0
        for (block in releaseBlocks) {
            if (count == maxReleases) return count
            val magnet = block.transaction["magnet"]
            val title = block.transaction["title"]
            val torrentInfoName = block.transaction["torrentInfoName"]
            if (magnet is String && magnet.length > 0 && title is String && title.length > 0 &&
                torrentInfoName is String && torrentInfoName.length > 0) {
                val transaction = activity?.supportFragmentManager?.beginTransaction()
                val coverFragment = ReleaseCoverFragment(block)
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
    fun publish(magnet: String, title: String, artists: String, releaseDate: String, torrentInfoName: String) {
        val myPeer = IPv8Android.getInstance().myPeer
        val transaction = mutableMapOf<String, String>(
            "magnet" to magnet,
            "title" to title,
            "artists" to artists,
            "date" to releaseDate,
            "torrentInfoName" to torrentInfoName
        )
        if (activity is MusicService) {
            val musicWallet = (activity as MusicService).walletService
            transaction["publisher"] = musicWallet.publicKey()
        }
        val trustchain = getMusicCommunity()
        trustchain.createProposalBlock("publish_release", transaction, myPeer.publicKey.keyToBin())
    }
}
