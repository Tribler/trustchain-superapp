package com.example.musicdao

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.NavUtils
import androidx.preference.PreferenceManager
import com.example.musicdao.ui.SubmitReleaseDialog
import com.frostwire.jlibtorrent.FileStorage
import kotlinx.android.synthetic.main.music_app_main.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.BaseActivity
import java.io.File

const val PREPARE_SIZE_KB: Long = 10 * 512L

class MusicService : BaseActivity() {
    private var currentMagnetLoading: String? = null
    private val defaultTorrent =
        "magnet:?xt=urn:btih:2803173609ad794d2789da6a6852fc1dbda7b7bf&dn=tru1992-07-23"
    private val trackLibrary: TrackLibrary =
        TrackLibrary(this)

    override val navigationGraph = R.navigation.musicdao_navgraph

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearCache()
        setContentView(R.layout.music_app_main)
        supportActionBar?.title = "Music app"

        registerBlockListener()
        registerBlockSigner()

//        val audioPlayer = AudioPlayer.getInstance(this)
//        if (audioPlayer.parent == null) {
//            mainLinearLayout.addView(AudioPlayer.getInstance(this), 0)
//        }

        torrentButton.setOnClickListener {
            createDefaultBlock()
        }

        shareTrackButton.setOnClickListener {
            selectLocalTrackFile()
        }

        Thread(Runnable {
            trackLibrary.startDHT()
        }).start()
        iterateClientConnectivity()
    }

    fun prepareNextTrack() {
//        val trackPlayerFragment: AudioPlayer? = supportFragmentManager.findFragmentById(R.id.track_player_fragment)
//        trackPlayerFragment?.prepareNextTrack()
        val audioPlayer = supportFragmentManager.findFragmentById(R.id.track_player_fragment) as AudioPlayer
        audioPlayer.prepareNextTrack()
    }

    /**
     * Clear cache on every run (for testing, and audio files may be large 15MB+). May be removed
     * in the future
     */
    private fun clearCache() {
        if (cacheDir.isDirectory && cacheDir.listFiles() != null) {
            val files = cacheDir.listFiles()
            files?.forEach {
                it.deleteRecursively()
            }
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
                        sleep(1000)
                        runOnUiThread {
                            val text =
                                "UP: ${trackLibrary.getUploadRate()} DOWN: ${trackLibrary.getDownloadRate()} DHT NODES: ${trackLibrary.getDhtNodes()}"
                            torrentClientInfo.text = text
                        }
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
     * This is called when the chooseFile is completed
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (uri != null) {
            // This should be reached when the chooseFile intent is completed and the user selected
            // an audio file
            val magnet = trackLibrary.seedFile(applicationContext, uri)
            publishTrack(magnet)
        }
    }

    /**
     * Once a magnet link to publish is chosen, show an alert dialog which asks to add metadata for
     * the Release (album title, release date etc)
     */
    private fun publishTrack(magnet: String) {
        this.currentMagnetLoading = magnet
        SubmitReleaseDialog(this)
            .show(supportFragmentManager, "Submit metadata")
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
        Toast.makeText(applicationContext, "Creating proposal block", Toast.LENGTH_SHORT).show()
        trustchain.createProposalBlock("publish_release", transaction, myPeer.publicKey.keyToBin())
    }

    private fun registerBlockSigner() {
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        trustchain.registerBlockSigner("publish_release", object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                Toast.makeText(
                    applicationContext,
                    "Signing block ${block.blockId}",
                    Toast.LENGTH_LONG
                ).show()
                trustchain.createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        })
    }

    /**
     * Once blocks on the trustchain arrive, which are audio release blocks, try to fetch and render
     * its metadata from its torrent file structure.
     */
    private fun registerBlockListener() {
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        val musicService = this
        trustchain.addListener("publish_release", object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Toast.makeText(
                    applicationContext,
                    "Discovered signed block ${block.blockId}",
                    Toast.LENGTH_LONG
                ).show()
                val magnet = block.transaction["magnet"]
                if (magnet != null && magnet is String) {
                    trackListLinearLayout.addView(
                        Release(
                            applicationContext,
                            magnet,
                            trackLibrary,
                            musicService,
                            block.transaction
                        ), 0
                    )
                }
                Log.d("TrustChainDemo", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }

    /**
     * Creates a trustchain block which uses the example creative commons release magnet
     * This is useful for testing the download speed of tracks over libtorrent
     */
    private fun createDefaultBlock() {
        publishTrack(defaultTorrent)
    }

    companion object {
        private const val PREF_PRIVATE_KEY = "private_key"
    }

    private fun getPrivateKey(): PrivateKey {
        // Load a key from the shared preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val privateKey = prefs.getString(PREF_PRIVATE_KEY, null)
        return if (privateKey == null) {
            // Generate a new key on the first launch
            val newKey = AndroidCryptoProvider.generateKey()
            prefs.edit()
                .putString(PREF_PRIVATE_KEY, newKey.keyToBin().toHex())
                .apply()
            newKey
        } else {
            AndroidCryptoProvider.keyFromPrivateBin(privateKey.hexToBytes())
        }
    }

    fun setSongArtistText(s: String) {
        val audioPlayer = supportFragmentManager.findFragmentById(R.id.track_player_fragment) as AudioPlayer
        audioPlayer.setSongArtistText(s)
    }

    fun startPlaying(file: File, index: Int, files: FileStorage) {
        val audioPlayer = supportFragmentManager.findFragmentById(R.id.track_player_fragment) as AudioPlayer
        audioPlayer.setAudioResource(file, index, files)
    }
}
