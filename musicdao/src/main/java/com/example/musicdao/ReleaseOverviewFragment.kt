package com.example.musicdao

import android.os.Bundle
import android.text.Editable
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.musicdao.ui.SubmitReleaseDialog
import kotlinx.android.synthetic.main.fragment_release_overview.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android

class ReleaseOverviewFragment : MusicFragment(R.layout.fragment_release_overview) {
    private var lastReleaseBlocksSize = -1
    private val maxReleases = 10

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        lastReleaseBlocksSize = -1

        lifecycleScope.launchWhenCreated {
            while (isActive) {
                if (activity is MusicService && debugText != null) {
                    debugText.text = (activity as MusicService).getStatsOverview()
                }
                Thread(Runnable {
                    showAllReleases()
                }).start()
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
     * List all the releases that are currently loaded in the local trustchain database
     */
    private fun showAllReleases() {
        val releaseBlocks = getMusicCommunity().database.getBlocksWithType("publish_release")
        if (releaseBlocks.size == lastReleaseBlocksSize) {
            return
        }
        lastReleaseBlocksSize = releaseBlocks.size
        var count = 0
        if (release_overview_layout is ViewGroup) {
            release_overview_layout.removeAllViews()
        }
        for (block in releaseBlocks) {
            if (count == maxReleases) return
            val magnet = block.transaction["magnet"]
            val title = block.transaction["title"]
            val torrentInfoName = block.transaction["torrentInfoName"]
            var query = ""
            if (requireActivity() is MusicService) {
                query = (requireActivity() as MusicService).filter
            }
            if (magnet is String && magnet.length > 0 && title is String && title.length > 0 &&
                torrentInfoName is String && torrentInfoName.length > 0) {
                count += 1
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
                val coverFragment = ReleaseCoverFragment(block)
                if (coverFragment.filter(query)) {
                    transaction.add(R.id.release_overview_layout, coverFragment, "releaseCover")
                    if (loadingReleases.visibility == View.VISIBLE) loadingReleases.visibility = View.GONE
                }
                transaction.commit()
            }
        }
    }

    /**
     * Creates a trustchain block which uses the example creative commons release magnet
     * This is useful for testing the download speed of tracks over libtorrent
     */
    private fun showCreateReleaseDialog() {
        publishTrack()
    }

    /**
     * Once a magnet link to publish is chosen, show an alert dialog which asks to add metadata for
     * the Release (album title, release date etc)
     */
    private fun publishTrack() {
        SubmitReleaseDialog(this)
            .show(childFragmentManager, "Submit metadata")
    }

    /**
     * After the user inserts some metadata for the release to be published, this function is called
     * to create the proposal block
     */
    fun finishPublishing(
        title: Editable?,
        artists: Editable?,
        releaseDate: Editable?,
        magnet: Editable?,
        torrentInfoName: String
    ) {
        publish(magnet.toString(), title.toString(), artists.toString(), releaseDate.toString(), torrentInfoName)
    }

    private fun publish(magnet: String, title: String, artists: String, releaseDate: String, torrentInfoName: String) {
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
