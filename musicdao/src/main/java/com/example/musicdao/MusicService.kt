package com.example.musicdao

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.example.musicdao.ipv8.MusicCommunity
import com.example.musicdao.util.Util
import com.example.musicdao.wallet.WalletService
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.turn.ttorrent.client.SharedTorrent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.BaseActivity
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

open class MusicService : BaseActivity() {
    lateinit var torrentStream: TorrentStream
    lateinit var walletService: WalletService
    override val navigationGraph = R.navigation.musicdao_navgraph
    var contentSeeder: ContentSeeder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread {
            startup()
        }.start()
        handleIntent(intent)
    }

    private fun startup() {
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
        if (!::walletService.isInitialized) {
            walletService =
                WalletService.getInstance(this)
        }
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

    fun getStatsOverview(): String {
        val sessionManager = torrentStream.sessionManager ?: return ""
        return "up: ${Util.readableBytes(sessionManager.uploadRate())}, down: ${
            Util.readableBytes(
                sessionManager.downloadRate()
            )
        }, dht nodes: ${sessionManager.dhtNodes()}, magnet peers: ${sessionManager.magnetPeers()?.length}"
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
        val fileList = mutableListOf<File>()
        val contentResolver = context.contentResolver
        val projection =
            arrayOf<String>(MediaStore.MediaColumns.DISPLAY_NAME)

        val randomInt = Random.nextInt(0, Int.MAX_VALUE)
        val parentDir = "${context.cacheDir}/$randomInt"

        for (uri in uris) {
            val cursor = contentResolver.query(uri, projection, null, null, null)
            var fileName = ""
            if (cursor != null) {
                try {
                    // TODO convert this cursor to support multifile sharing
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(0)
                    }
                } finally {
                    cursor.close()
                }
            }

            if (fileName == "") throw Error("Source file name for creating torrent not found")
            val input = contentResolver.openInputStream(uri) ?: throw Resources.NotFoundException()
            val tempFileLocation = "$parentDir/$fileName"

            // TODO currently creates temp copies before seeding, but should not be necessary
            FileUtils.copyInputStreamToFile(input, File(tempFileLocation))
            fileList.add(File(tempFileLocation))
        }

        val torrent = SharedTorrent.create(File(parentDir), fileList, 65535, listOf(), "")
        val torrentFile = "$parentDir.torrent"
        torrent.save(FileOutputStream(torrentFile))
        return File(torrentFile)
    }

    fun showToast(text: String, length: Int) {
        runOnUiThread {
            Toast.makeText(baseContext, text, length).show()
        }
    }

    companion object {
        private const val PREF_PRIVATE_KEY = "private_key"
    }
}
