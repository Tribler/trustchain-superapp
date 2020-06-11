package com.example.musicdao

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.musicdao.ui.SubmitReleaseDialog
import kotlinx.android.synthetic.main.fragment_release_overview.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment

const val defaultBlockId =
    "4c69624e61434c504b3a084222f8a6f7e59db1dfbe88086da69e91e87295dc544d11e680875e0ed9d2097739668d469dac06f95482810bff7ed4ef66c2b7392e8065762145cbb0c3fde4"
const val defaultBlockHash = "dec73b6cf5b708404d47d6d878ab49116588dec30d1b6024214fdd2f721dd23e"

class ReleaseOverviewFragment : BaseFragment(R.layout.fragment_release_overview) {
    private var currentMagnetLoading: String? = null
    private val defaultMagnet =
        "magnet:?xt=urn:btih:30894385d7a3288cb6073055b6c743d2d977806c&dn=gd77-06-08.mtx.seamons.ht32.95361.t-flac16"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerBlockListener()

        torrentButton.setOnClickListener {
            createDefaultBlock()
        }

        shareTrackButton.setOnClickListener {
            selectLocalTrackFile()
        }

        iterateClientConnectivity()

        showAllReleases()
    }

    /**
     * List all the releases that are currently loaded in the local trustchain database
     */
    private fun showAllReleases() {
        val releaseBlocks = getTrustChainCommunity().database.getBlocksWithType("publish_release")
        for (block in releaseBlocks) {
            val magnet = block.transaction["magnet"]
            val title = block.transaction["title"]
            if (magnet is String && magnet.length > 0 && title is String && title.length > 0) {
                val transaction = requireActivity().supportFragmentManager.beginTransaction()
                val coverFragment = ReleaseCoverFragment(block)
                transaction.add(R.id.release_overview_layout, coverFragment, "releaseCover")
                transaction.commit()
            }
        }
    }

    /**
     * Creates a trustchain block which uses the example creative commons release magnet
     * This is useful for testing the download speed of tracks over libtorrent
     */
    private fun createDefaultBlock() {
        publishTrack(defaultMagnet)
    }

    /**
     * Once a magnet link to publish is chosen, show an alert dialog which asks to add metadata for
     * the Release (album title, release date etc)
     */
    private fun publishTrack(magnet: String) {
        this.currentMagnetLoading = magnet
        SubmitReleaseDialog(this)
            .show(childFragmentManager, "Submit metadata")
    }


    /**
     * After the user inserts some metadata for the release to be published, this function is called
     * to create the proposal block
     */
    fun finishPublishing(title: Editable?, artists: Editable?, releaseDate: Editable?) {
        val myPeer = IPv8Android.getInstance().myPeer

        val transaction = mapOf(
            "publisher" to myPeer.mid,
            "magnet" to currentMagnetLoading,
            "title" to title.toString(),
            "artists" to artists.toString(),
            "date" to releaseDate.toString()
        )
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        Toast.makeText(context, "Creating proposal block", Toast.LENGTH_SHORT).show()
        trustchain.createProposalBlock("publish_release", transaction, myPeer.publicKey.keyToBin())
    }


    /**
     * This is called when the chooseFile is completed
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (uri != null) {
            // This should be reached when the chooseFile intent is completed and the user selected
            // an audio file
            val localContext = context
            if (localContext != null) {
                val magnet = (activity as MusicService).trackLibrary.seedFile(localContext, uri)
                publishTrack(magnet)
            }
        }
    }

    /**
     * Select an audio file from local disk
     */
    private fun selectLocalTrackFile() {
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.type = "audio/*"
        val chooseFileActivity = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(chooseFileActivity, 1)
        val uri = chooseFileActivity.data
        if (uri != null) {
            println(uri.path)
        }
    }

    /**
     * Iteratively update and show torrent client connectivity
     */
    private fun iterateClientConnectivity() {
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    while (!this.isInterrupted) {
                        val text =
                            "UP: ${(activity as MusicService).trackLibrary.getUploadRate()} DOWN: ${(activity as MusicService).trackLibrary.getDownloadRate()} DHT: ${(activity as MusicService).trackLibrary.getDhtNodes()}"
                        torrentClientInfo.text = text
                        sleep(1000)
                    }
                } catch (e: Exception) {
                }
            }
        }
        thread.start()
    }


    /**
     * Once blocks on the trustchain arrive, which are audio release blocks, try to fetch and render
     * its metadata from its torrent file structure.
     */
    private fun registerBlockListener() {
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
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
