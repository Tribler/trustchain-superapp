package com.example.musicdao

import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.musicdao.ui.SubmitReleaseDialog
import kotlinx.android.synthetic.main.fragment_release_overview.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.common.ui.BaseFragment

class ReleaseOverviewFragment : BaseFragment(R.layout.fragment_release_overview) {
    private var lastReleaseBlocksSize = -1
    private val maxReleases = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        registerBlockListener()

        lastReleaseBlocksSize = -1

        lifecycleScope.launchWhenCreated {
            while(isActive) {
                showAllReleases()
                delay(3000)
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        addPlaylistFab.setOnClickListener {
            showCreateReleaseDialog()
        }
    }

    /**
     * List all the releases that are currently loaded in the local trustchain database
     */
    private fun showAllReleases() {
        val releaseBlocks = getTrustChainCommunity().database.getBlocksWithType("publish_release")
        if (releaseBlocks.size == lastReleaseBlocksSize) {
            return
        }
        var count = 0
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
                }
                transaction.commit()
            }
        }
        lastReleaseBlocksSize = releaseBlocks.size
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
        val myPeer = IPv8Android.getInstance().myPeer

        val transaction = mapOf(
            "magnet" to magnet.toString(),
            "title" to title.toString(),
            "artists" to artists.toString(),
            "date" to releaseDate.toString(),
            "torrentInfoName" to torrentInfoName
        )
        val trustchain = getTrustChainCommunity()
        Toast.makeText(context, "Creating proposal block", Toast.LENGTH_SHORT).show()
        trustchain.createProposalBlock("publish_release", transaction, myPeer.publicKey.keyToBin())
    }

    /**
     * Once blocks on the trustchain arrive, which are audio release blocks, try to fetch and render
     * its metadata from its torrent file structure.
     */
    private fun registerBlockListener() {
        val trustchain = getTrustChainCommunity()
        trustchain.addListener("publish_release", object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Toast.makeText(
                    context,
                    "Discovered signed block ${block.blockId}",
                    Toast.LENGTH_LONG
                ).show()
                val magnet = block.transaction["magnet"]
                if (magnet != null && magnet is String) {
                    val transaction = requireActivity().supportFragmentManager.beginTransaction()
                    val coverFragment = ReleaseCoverFragment(block)
                    transaction.add(R.id.release_overview_layout, coverFragment, "releaseCover")
                    transaction.commit()
                }
                Log.d("TrustChainDemo", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }
}
