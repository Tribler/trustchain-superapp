package com.example.musicdao.net

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.example.musicdao.ipv8.SwarmHealth
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.PeerConnectAlert
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*


lateinit var contentSeederInstance: ContentSeeder

/**
 * This maintains an implementation of a strategy of libtorrent seeding.
 * Currently, a max. of 10 torrents, in LIFO ordering, from the cache directory, are being seeded.
 */
class ContentSeeder(private val saveDir: File, private val context: Context, private val sessionManager: SessionManager) {
    private val maxTorrentThreads = 10
    private var started = false

//    val sessionManager: SessionManager = TorrentStream.getInstance().sessionManager // Manages seeding torrents
    var swarmHealthMap: MutableMap<Sha1Hash, SwarmHealth> = mutableMapOf<Sha1Hash, SwarmHealth>()

    /**
     * Scan the saveDir directory for torrent files, and see if we can seed them. If all files have
     * been downloaded previously, we start seeding the respective torrent; otherwise we leave it
     */
    fun start(): Int {
        var count = 0
//        if (started) return count
//        started = true TODO should this code stay?
        if (!saveDir.isDirectory) throw Error("Content seeder active in non-directory")
        val fileList = saveDir.listFiles()
        if (fileList !is Array<File>) return count
        // A maximum of maxTorrentThreads torrents are seeded, with LIFO ordering
        Arrays.sort(fileList) { a, b -> a.lastModified().compareTo(b.lastModified()) }

        sessionManager.addListener(object : AlertListener {
            override fun types(): IntArray {
                return intArrayOf(AlertType.PEER_CONNECT.swig())
            }

            override fun alert(alert: Alert<*>) {
                when (alert.type()) {
                    AlertType.PEER_CONNECT -> {
                        val handle = (alert as PeerConnectAlert).handle()
                        updateSwarmhealth(handle)
                    }
                    else -> {}
                }
            }
        })

        if (!sessionManager.isRunning) {
            sessionManager.start()
        }

        saveDir.listFiles()?.forEachIndexed { index, file ->
            if (index >= maxTorrentThreads) return count
            if (file.name.endsWith(".torrent")) {
                val torrentInfo = TorrentInfo(file)
                if (torrentInfo.isValid) {
                    count += 1
                    // 'Downloading' the torrent file also starts seeding it after download has
                    // already been completed
                    // We only seed torrents that have previously already been fully downloaded,
                    // so that this does not clash with the TorrentStream library
                    if (Util.isTorrentCompleted(torrentInfo, saveDir)) {
                        downloadAndSeed(file)
                    }
                }
            }
        }
        return count
    }

    /**
     * Keep track of connected peers/seeders per torrent by using a local swarmHealthMap
     */
    private fun updateSwarmhealth(handle: TorrentHandle) {
        val numPeers = handle.status().numPeers()
        val numSeeds = handle.status().numSeeds()
        // Add to local database to keep track of connectivity of each item
        Log.d(
            "ContentSeeder",
            "Peer connected: $numPeers peers," +
                " $numSeeds seeds for ${handle.infoHash()}"
        )
        // We assume that the amount of seeders does not include ourself; so we add 1 to numSeeds
        swarmHealthMap[handle.infoHash()] = SwarmHealth(
            handle.infoHash().toString(),
            numPeers.toUInt(),
            1.toUInt()
        )
    }

    private fun downloadAndSeed(torrentFile: File) {
        val torrentUri = torrentFile.toUri()
        val bytes = context.contentResolver.openInputStream(torrentUri)?.readBytes()
        val torrentInfo = TorrentInfo.bdecode(bytes)
        if (bytes != null) {
            sessionManager.download(torrentInfo, saveDir)
            val torrentHandle = sessionManager.find(torrentInfo.infoHash())
            torrentHandle.setFlags(torrentHandle.flags().and_(TorrentFlags.SEED_MODE))
            torrentHandle.pause()
            torrentHandle.resume() // This is a fix/hack that forces SEED_MODE to be available, for
            // an unsolved issue: seeding local torrents often result in an endless "CHECKING_FILES"
            // state
            // Start by setting swarm connectivity to 1 as the current device is now 1 seeder
            updateSwarmhealth(torrentHandle)
        }
    }

    /**
     * Create, save and seed a torrent file, based on a TorrentInfo object
     */
    fun add(torrentInfo: TorrentInfo, torrentInfoName: String): Boolean {
        val torrentFile = File("$saveDir/$torrentInfoName.torrent")
        if (torrentInfo.isValid) {
            if (!torrentFile.isFile) {
                FileUtils.copyInputStreamToFile(torrentInfo.bencode().inputStream(), torrentFile)
            }
            downloadAndSeed(torrentFile)
            return true
        }
        return false
    }

    companion object {
        fun getInstance(cacheDir: File, context: Context, sessionManager: SessionManager): ContentSeeder {
            if (!::contentSeederInstance.isInitialized) {
                contentSeederInstance = ContentSeeder(cacheDir, context, sessionManager)
            }
            return contentSeederInstance
        }
    }
}
