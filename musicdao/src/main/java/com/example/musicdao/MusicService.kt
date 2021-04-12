package com.example.musicdao

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.musicdao.ipv8.MusicCommunity
import com.example.musicdao.ipv8.SwarmHealth
import com.example.musicdao.net.ContentSeeder
import com.example.musicdao.player.AudioPlayer
import com.example.musicdao.util.ReleaseFactory
import com.example.musicdao.util.Util
import com.example.musicdao.wallet.WalletService
import com.frostwire.jlibtorrent.*
import kotlinx.android.synthetic.main.dialog_tip_artist.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import java.io.File
import kotlin.random.Random

/**
 * This maintains the interactions between the UI and seeding/trustchain
 */
class MusicService : AppCompatActivity() {
    // Popularity measurement by swarm health
    var swarmHealthMap = mutableMapOf<Sha1Hash, SwarmHealth>()
    private lateinit var musicGossipingService: MusicGossipingService
    private var mBound = false // Whether the musicGossipingService is bound to the current activity
    private val navigationGraph: Int = R.navigation.musicdao_navgraph

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MusicGossipingService.LocalBinder
            musicGossipingService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    var sessionManager: SessionManager? = null

    private val navController by lazy {
        findNavController(R.id.navHostFragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_base)
        navController.setGraph(navigationGraph)
        handleIntent(intent)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        startup()
    }

    override fun onDestroy() {
        AudioPlayer.getInstance()?.exoPlayer?.stop()
        sessionManager?.stop()
        sessionManager = null
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()

        Intent(this, MusicGossipingService::class.java).also { intent ->
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()

        if (mBound) {
            unbindService(connection)
            mBound = false
        }
    }

    private fun startup() {
        val ses = SessionManager()
        ses.start()
        registerBlockSigner()
        iterativelyUpdateSwarmHealth()

        // Start ContentSeeder service: for serving music torrents to other devices
        ContentSeeder.getInstance(
            applicationContext.cacheDir,
            ses
        ).start()

        // Start WalletService, for maintaining and sending coins
        WalletService.getInstance(applicationContext.cacheDir, this@MusicService)
            .start()

        sessionManager = ses
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                val args = Bundle()
                args.putString("filter", query)
                findNavController(R.id.navHostFragment).navigate(
                    R.id.playlistsOverviewFragment,
                    args
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menu == null) return false
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_searchable, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_search -> {
                onSearchRequested()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Keep track of Swarm Health for all torrents being monitored
     */
    private fun iterativelyUpdateSwarmHealth() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                swarmHealthMap = filterSwarmHealthMap()

                if (mBound) {
                    musicGossipingService.setSwarmHealthMap(swarmHealthMap)
                }
                delay(3000)
            }
        }
    }

    /**
     * Merge local and remote swarm health map and remove outdated data
     */
    private fun filterSwarmHealthMap(): MutableMap<Sha1Hash, SwarmHealth> {
        val localMap = updateLocalSwarmHealthMap()
        val musicCommunity = IPv8Android.getInstance().getOverlay<MusicCommunity>()
        val communityMap = musicCommunity?.swarmHealthMap ?: mutableMapOf()
        // Keep the highest numPeers/numSeeds count of all items in both maps
        // This map contains all the combined data, where local and community map data are merged;
        // the highest connectivity count for each item is saved in a gloal map for the MusicService
        val map: MutableMap<Sha1Hash, SwarmHealth> = mutableMapOf<Sha1Hash, SwarmHealth>()
        val allKeys = localMap.keys + communityMap.keys
        for (infoHash in allKeys) {
            val shLocal = localMap[infoHash]
            val shRemote = communityMap[infoHash]

            val bestSwarmHealth = SwarmHealth.pickBest(shLocal, shRemote)
            if (bestSwarmHealth != null) {
                map[infoHash] = bestSwarmHealth
            }
        }
        // Remove outdated swarm health data: if the data is outdated, we throw it away
        return map.filterValues { it.isUpToDate() }.toMutableMap()
    }

    /**
     * Go through all the torrents that we are currently seeding and mark its connectivity to peers
     */
    private fun updateLocalSwarmHealthMap(): MutableMap<Sha1Hash, SwarmHealth> {
        val sessionManager = sessionManager ?: return mutableMapOf()
        val contentSeeder =
            ContentSeeder.getInstance(cacheDir, sessionManager)
        val localMap = contentSeeder.swarmHealthMap
        for (infoHash in localMap.keys) {
            // Update all connectivity stats of the torrents that we are currently seeding
            if (sessionManager.isRunning) {
                val handle = sessionManager.find(infoHash) ?: continue
                val newSwarmHealth = SwarmHealth(
                    infoHash.toString(),
                    handle.status().numPeers().toUInt(),
                    handle.status().numSeeds().toUInt()
                )
                // Never go below 1, because we know we are at least 1 seeder of our local files
                if (newSwarmHealth.numSeeds.toInt() < 1) continue
                localMap[infoHash] = newSwarmHealth
            }
        }
        return localMap
    }

    /**
     * Show libtorrent connectivity stats
     */
    fun getStatsOverview(): String {
        val sessionManager = sessionManager ?: return "Starting torrent client..."
        if (!sessionManager.isRunning) return "Starting torrent client..."
        return "up: ${Util.readableBytes(sessionManager.uploadRate())}, down: ${
        Util.readableBytes(sessionManager.downloadRate())
        }, dht nodes (torrent): ${sessionManager.dhtNodes()}"
    }

    /**
     * On discovering a half block, with tag publish_release, agree it immediately (for now). In the
     * future there will be logic added here to determine whether an upload was done by the correct
     * artist/label (artist passport).
     */
    private fun registerBlockSigner() {
        val musicCommunity = IPv8Android.getInstance().getOverlay<MusicCommunity>()
        musicCommunity?.registerBlockSigner(
            "publish_release",
            object : BlockSigner {
                override fun onSignatureRequest(block: TrustChainBlock) {
                    musicCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
                }
            }
        )
    }

    /**
     * Generates a a .torrent File from local files
     * @param uris the list of Uris pointing to local audio source files to publish
     */
    @Throws(Resources.NotFoundException::class)
    fun generateTorrent(context: Context, uris: List<Uri>): File {
        val contentResolver = context.contentResolver
        val randomInt = Random.nextInt(0, Int.MAX_VALUE)
        val parentDir = "${context.cacheDir}/$randomInt"

        return ReleaseFactory.generateTorrent(parentDir, uris, contentResolver)
    }

    fun showToast(text: String, length: Int) {
        runOnUiThread {
            Toast.makeText(baseContext, text, length).show()
        }
    }
}
