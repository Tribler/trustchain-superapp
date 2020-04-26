package com.example.musicdao.net

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicdao.R
import com.example.musicdao.databinding.MusicAppMainBinding
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.squareup.sqldelight.android.AndroidSqliteDriver
import kotlinx.android.synthetic.main.music_app_main.*
import nl.tudelft.ipv8.IPv8Configuration
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.OverlayConfiguration
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.BaseActivity
import java.util.*


class MusicService : BaseActivity() {
    private lateinit var binding: MusicAppMainBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var items: MutableList<String> = mutableListOf()
    private val trackLibrary: TrackLibrary = TrackLibrary()

    private var peers: List<Peer> = listOf()
    override val navigationGraph = R.navigation.musicdao_navgraph
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.music_app_main)
        supportActionBar?.title = "Music app"

        binding = MusicAppMainBinding.inflate(layoutInflater)

        viewManager = LinearLayoutManager(this)
        items.add("Listening for peers...")

        viewAdapter = MusicListAdapter(items)

        viewAdapter.notifyDataSetChanged()

        recyclerView = findViewById<RecyclerView>(R.id.recycler_view_items).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(false)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

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

        items.add("I am " + IPv8Android.getInstance().myPeer.mid)
        viewAdapter.notifyDataSetChanged()

        registerBlockListener()
        registerBlockSigner()

        torrentButton.setOnClickListener {
            startStreamingTorrent("magnet:?xt=urn:btih:23176D77FC9227FA0811E8796B2B9ACCC02C01BF&dn=VA+-+NOW+Reggae+Classics+%282020%29+Mp3+320kbps+%5BPMEDIA%5D+%E2%AD%90%EF%B8%8F&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce&tr=udp%3A%2F%2Fexodus.desync.com%3A6969%2Fannounce&tr=udp%3A%2F%2Fretracker.netbynet.ru%3A2710%2Fannounce&tr=udp%3A%2F%2Ftracker.nyaa.uk%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2Fopentor.org%3A2710%2Fannounce&tr=udp%3A%2F%2F9.rarbg.me%3A2750%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.cyberia.is%3A6969%2Fannounce&tr=udp%3A%2F%2Fp4p.arenabg.com%3A1337%2Fannounce&tr=http%3A%2F%2Fseed.datascene.net%2F5e9d12bf35cad25402848253da9adf2a%2Fannounce&tr=udp%3A%2F%2Ftracker.zer0day.to%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce&tr=udp%3A%2F%2Fcoppersurfer.tk%3A6969%2Fannounce")
        }

        shareTrackButton.setOnClickListener {
            selectLocalTrackFile()
        }

        Thread(Runnable {
            trackLibrary.startDHT()
        }).start()
        updateTorrentClientInfo(1000)
    }

    private fun updateTorrentClientInfo(period: Long) {
        Thread(Runnable {
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    val text =
                        "Torrent client info: UP: ${trackLibrary.getUploadRate()}MB DOWN: ${trackLibrary.getDownloadRate()}MB DHT NODES: ${trackLibrary.getDhtNodes()}"
                    torrentClientInfo.text = text
                }
            }, 0, period)
        }).start()
    }

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

    //TODO this function should be removed completely and decoupled into separate functions
    private fun publishTrack(magnet: String) {
        val trackId = Random().nextInt()
        val myPeer = IPv8Android.getInstance().myPeer

        val transaction = mapOf(
            "trackId" to trackId,
            "author" to myPeer.mid,
            "magnet" to magnet
        )
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        items.add("Creating proposal block")
        viewAdapter.notifyDataSetChanged()
        trustchain.createProposalBlock("publish_track", transaction, myPeer.publicKey.keyToBin())
    }

    private fun registerBlockSigner() {
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        trustchain.registerBlockSigner("publish_track", object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                items.add("Signing block ${block.blockId}")
                viewAdapter.notifyDataSetChanged()
                trustchain.createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        })
    }

    private fun registerBlockListener() {
        val trustchain = IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!
        trustchain.addListener("publish_track", object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                items.add("Self-signed block on trustchain: ${block.blockId}")
                items.add("Song metadata: ${block.transaction}")
                val magnet = block.transaction["magnet"]
                if (magnet != null && magnet is String) startStreamingTorrent("magnet:?xt=urn:btih:23176D77FC9227FA0811E8796B2B9ACCC02C01BF&dn=VA+-+NOW+Reggae+Classics+%282020%29+Mp3+320kbps+%5BPMEDIA%5D+%E2%AD%90%EF%B8%8F&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce&tr=udp%3A%2F%2Fexodus.desync.com%3A6969%2Fannounce&tr=udp%3A%2F%2Fretracker.netbynet.ru%3A2710%2Fannounce&tr=udp%3A%2F%2Ftracker.nyaa.uk%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2Fopentor.org%3A2710%2Fannounce&tr=udp%3A%2F%2F9.rarbg.me%3A2750%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.cyberia.is%3A6969%2Fannounce&tr=udp%3A%2F%2Fp4p.arenabg.com%3A1337%2Fannounce&tr=http%3A%2F%2Fseed.datascene.net%2F5e9d12bf35cad25402848253da9adf2a%2Fannounce&tr=udp%3A%2F%2Ftracker.zer0day.to%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce&tr=udp%3A%2F%2Fcoppersurfer.tk%3A6969%2Fannounce")
                viewAdapter.notifyDataSetChanged()
                Log.d("TrustChainDemo", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }

    private fun startStreamingTorrent(magnet: String) {
        val torrentOptions: TorrentOptions = TorrentOptions.Builder()
            .saveLocation(applicationContext.cacheDir)
            .autoDownload(false)
            .removeFilesAfterStop(true)
            .build()

        val torrentStream: TorrentStream = TorrentStream.init(torrentOptions)
        torrentStream.startStream(magnet)
        torrentStream.addListener(AudioTorrentStreamHandler(progressBar, applicationContext, bufferInfo, playButton))
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(SongMessage.Deserializer)
        items.add("Song from " + peer.mid + " : " + payload.message)
    }

    companion object {
        private const val PREF_PRIVATE_KEY = "private_key"
        private const val MESSAGE_ID = 1
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
