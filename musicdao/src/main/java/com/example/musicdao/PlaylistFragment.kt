package com.example.musicdao

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Toast
import com.example.musicdao.ui.SubmitReleaseDialog
import kotlinx.android.synthetic.main.music_app_main.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment

class PlaylistFragment : BaseFragment(R.layout.music_app_main) {
    private var currentMagnetLoading: String? = null
    private val defaultTorrent =
        "magnet:?xt=urn:btih:2803173609ad794d2789da6a6852fc1dbda7b7bf&dn=tru1992-07-23"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        torrentButton.setOnClickListener {
            createDefaultBlock()
        }

        shareTrackButton.setOnClickListener {
            selectLocalTrackFile()
        }

        iterateClientConnectivity()
    }

    /**
     * Iteratively update and show torrent client connectivity
     */
    private fun iterateClientConnectivity() {
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    while (!this.isInterrupted) {
                        sleep(1000)
                        val text =
                            "UP: ${(activity as MusicService).trackLibrary.getUploadRate()} DOWN: ${(activity as MusicService).trackLibrary.getDownloadRate()} DHT: ${(activity as MusicService).trackLibrary.getDhtNodes()}"
                        torrentClientInfo.text = text
                    }
                } catch (e: Exception) {
                }
            }
        }
        thread.start()
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
     * Creates a trustchain block which uses the example creative commons release magnet
     * This is useful for testing the download speed of tracks over libtorrent
     */
    private fun createDefaultBlock() {
        publishTrack(defaultTorrent)
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
     * Once a magnet link to publish is chosen, show an alert dialog which asks to add metadata for
     * the Release (album title, release date etc)
     */
    private fun publishTrack(magnet: String) {
        this.currentMagnetLoading = magnet
        SubmitReleaseDialog(this)
            .show(requireActivity().supportFragmentManager, "Submit metadata")
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
}
