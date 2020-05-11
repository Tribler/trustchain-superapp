package com.example.musicdao

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.util.Log
import android.widget.Toast
import com.example.musicdao.ipv8.MusicDemoCommunity
import com.example.musicdao.ui.SubmitReleaseDialog
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.squareup.sqldelight.android.AndroidSqliteDriver
import kotlinx.android.synthetic.main.music_app_main.*
import nl.tudelft.ipv8.IPv8Configuration
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.OverlayConfiguration
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.BaseActivity
import java.lang.Exception

const val PREPARE_SIZE_KB: Long = 10 * 512L

class MusicService : BaseActivity() {
    private var currentMagnetLoading: String? = null
    private val defaultTorrent =
        "magnet:?xt=urn:btih:88b17043ee77a31feef3b55a8ebe120b045fa577&dn=tru1991-05-27sbd"
    private val trackLibrary: TrackLibrary =
        TrackLibrary()
    var torrentStream: TorrentStream? = null

    override val navigationGraph = R.navigation.musicdao_navgraph
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearCache()
        torrentStream = initTorrentStreamService()
        setContentView(R.layout.music_app_main)
        supportActionBar?.title = "Music app"

        initTrustChain()

        Toast.makeText(applicationContext, "Initialized IPv8", Toast.LENGTH_SHORT).show()
        mid.text = ("My trustchain member ID: " + IPv8Android.getInstance().myPeer.mid)

        registerBlockListener()
        registerBlockSigner()

        mainLinearLayout.addView(AudioPlayer.getInstance(applicationContext, this), 0)

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

    private fun initTrustChain() {
        val community = OverlayConfiguration(
            Overlay.Factory(MusicDemoCommunity::class.java),
            listOf(RandomWalk.Factory())
        )

        val settings = TrustChainSettings()
        val driver = AndroidSqliteDriver(Database.Schema, this, "trustchain.db")
        val store = TrustChainSQLiteStore(Database(driver))
        val randomWalk = RandomWalk.Factory()
        val trustChainCommunity = OverlayConfiguration(
            TrustChainCommunity.Factory(settings, store),
            listOf(randomWalk)
        )

        val config = IPv8Configuration(overlays = listOf(community, trustChainCommunity))

        IPv8Android.Factory(application)
            .setConfiguration(config)
            .setPrivateKey(getPrivateKey())
            .init()
    }

    private fun initTorrentStreamService(): TorrentStream {
        val prepareSize: Long = PREPARE_SIZE_KB * 1024L
        val torrentOptions: TorrentOptions = TorrentOptions.Builder()
            .saveLocation(applicationContext.cacheDir)
            .autoDownload(false)
            //PrepareSize: Starts playing the song after PREPARE_SIZE_MB megabytes are buffered.
            //Requires testing and tweaking to find the best number
            .prepareSize(prepareSize)
            .removeFilesAfterStop(true)
            .build()

        return TorrentStream.init(torrentOptions)
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
                    while (!this.isInterrupted && torrentStream != null) {
                        sleep(1000)
                        runOnUiThread {
                            val text =
                                "Torrent client info: UP: ${trackLibrary.getUploadRate()}KB DOWN: ${trackLibrary.getDownloadRate()}KB DHT NODES: ${trackLibrary.getDhtNodes()}"
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
            //This should be reached when the chooseFile intent is completed and the user selected
            //an audio file
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

    fun fillProgressBar() {
        progressBar.progress = 100
        progressBar.secondaryProgress = 100
    }

    fun resetProgressBar() {
        progressBar.progress = 0
        progressBar.secondaryProgress = 0
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
}
