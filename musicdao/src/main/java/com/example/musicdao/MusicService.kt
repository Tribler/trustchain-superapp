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
import com.example.musicdao.net.ContentSeeder
import com.example.musicdao.util.ReleaseFactory
import com.example.musicdao.util.Util
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
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
    lateinit var torrentStream: TorrentStream
    private val navigationGraph: Int = R.navigation.musicdao_navgraph

    var contentSeeder: ContentSeeder? = null

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

        lifecycleScope.launchWhenStarted {
            while (isActive) {
                if (torrentStream.sessionManager != null) {
                    val seeder =
                        ContentSeeder.getInstance(
                            torrentStream.sessionManager,
                            applicationContext.cacheDir
                        )
                    seeder.start()
                    contentSeeder = seeder
                    return@launchWhenStarted
                }
                delay(3000)
            }
        }
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
                findNavController(R.id.navHostFragment).navigate(R.id.releaseOverviewFragment, args)
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

    /**
     * Show libtorrent connectivity stats
     */
    fun getStatsOverview(): String {
        val startingMessage = "Starting libtorrent session..."
        if (!::torrentStream.isInitialized) return startingMessage
        val sessionManager = torrentStream.sessionManager ?: return startingMessage
        return "up: ${Util.readableBytes(sessionManager.uploadRate())}, down: ${
            Util.readableBytes(
                sessionManager.downloadRate()
            )
        }, dht nodes: ${sessionManager.dhtNodes()}, magnet peers: ${sessionManager.magnetPeers()?.length}"
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
