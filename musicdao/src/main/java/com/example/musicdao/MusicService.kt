package com.example.musicdao

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
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
import com.example.musicdao.util.ReleaseFactory
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.Sha1Hash
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import java.io.File
import java.util.*
import kotlin.random.Random

/**
 * This maintains the interactions between the UI and seeding/trustchain
 */
class MusicService : AppCompatActivity() {
    lateinit var torrentStream: TorrentStream
    private val navigationGraph: Int = R.navigation.musicdao_navgraph

    // Popularity measurement by swarm health
    var swarmHealthMap = mutableMapOf<Sha1Hash, SwarmHealth>()
    private val popularityGossipInterval: Long = 5000
    private val gossipTopTorrents = 5 // The amount of most popular torrents we use to gossip its
    // swarm health with neighbours
    private val gossipRandomTorrents = 5 // The amount of torrents we know its swarm health of
    // to share with neighbours

    private val navController by lazy {
        findNavController(R.id.navHostFragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_base)
        navController.setGraph(navigationGraph)

        Thread {
            startup()
        }.start()
        handleIntent(intent)
    }

    fun initTorrentStream() {
        torrentStream = try {
            TorrentStream.getInstance()
        } catch (e: Exception) {
            TorrentStream.init(
                TorrentOptions.Builder()
                    .saveLocation(applicationContext.cacheDir)
                    .removeFilesAfterStop(false)
                    .autoDownload(false)
                    .build()
            )
        }
    }

    private fun startup() {
        initTorrentStream()
        registerBlockSigner()
        iterativelyCrawlTrustChains()
        iterativelyUpdateConnectivityStats()

        ContentSeeder.getInstance(
            applicationContext.cacheDir,
            applicationContext
        ).start()
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
                findNavController(R.id.navHostFragment).navigate(R.id.playlistsOverviewFragment, args)
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
            R.id.action_search -> {
                onSearchRequested()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * This is a very simplistic way to crawl all chains from the peers you know
     */
    private fun iterativelyCrawlTrustChains() {
        val musicCommunity = IPv8Android.getInstance().getOverlay<MusicCommunity>()!!
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                for (peer in musicCommunity.getPeers()) {
                    musicCommunity.crawlChain(peer)
                    delay(1000)
                }
                delay(3000)
            }
        }
    }

    private fun iterativelyUpdateConnectivityStats() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                delay(popularityGossipInterval)
                swarmHealthMap = updateLocalSwarmHealthList()
                // Pick 5 of the most popular torrents and 5 random torrents, and send those stats to any neighbour
                // First, we sort the map based on swarm health
                val sortedMap = swarmHealthMap.toList()
                    .sortedBy { (_, value) -> value }
                    .toMap()
                gossipSwarmHealth(sortedMap, gossipTopTorrents)
                gossipSwarmHealth(swarmHealthMap, gossipRandomTorrents)
            }
        }
    }

    private fun gossipSwarmHealth(map: Map<Sha1Hash, SwarmHealth>, maxInterations: Int) {
        val musicCommunity = IPv8Android.getInstance().getOverlay<MusicCommunity>()!!
        var count = 0
        for (entry in map.entries) {
            count += 1
            if (count > maxInterations) break
            musicCommunity.sendSwarmHealthMessage(entry.value)
        }
    }

    private fun updateLocalSwarmHealthList(): MutableMap<Sha1Hash, SwarmHealth> {
        val map =
            ContentSeeder.getInstance(cacheDir, applicationContext).swarmHealthMap
        val musicCommunity = IPv8Android.getInstance().getOverlay<MusicCommunity>()!!
        val map2 = musicCommunity.swarmHealthMap
        map += map2
        // Remove outdated swarm health data: if the data is 1 day old or older, we throw it away
        for ((infoHash, swarmHealth) in map) {
            val timestamp = Date(swarmHealth.timestamp.toLong())
            if (timestamp.before(Date(swarmHealth.timestamp.toLong() - 3600 * 24 * 1000))) {
                map.remove(infoHash)
            }
        }
        return map
    }

    /**
     * Show libtorrent connectivity stats
     */
    fun getStatsOverview(): String {
        val sessionManager = ContentSeeder.getInstance(cacheDir, applicationContext).torrentSession
        if (!sessionManager.isRunning) return "Starting up torrent client..."
        return "up: ${Util.readableBytes(sessionManager.uploadRate)}, down: ${
            Util.readableBytes(sessionManager.downloadRate)
        }"
    }

    /**
     * On discovering a half block, with tag publish_release, agree it immediately (for now). In the
     * future there will be logic added here to determine whether an upload was done by the correct
     * artist/label (artist passport).
     */
    private fun registerBlockSigner() {
        val musicCommunity = IPv8Android.getInstance().getOverlay<MusicCommunity>()!!
        musicCommunity.registerBlockSigner("publish_release", object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                musicCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        })
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
