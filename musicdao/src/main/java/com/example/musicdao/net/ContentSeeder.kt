package com.example.musicdao.net

import android.util.Log
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
class ContentSeeder(private val saveDir: File, private val sessionManager: SessionManager) {
    private val maxTorrentThreads = 10

    var swarmHealthMap: MutableMap<Sha1Hash, SwarmHealth> = mutableMapOf<Sha1Hash, SwarmHealth>()

    /**
     * Scan the saveDir directory for torrent files, and see if we can seed them. If all files have
     * been downloaded previously, we start seeding the respective torrent; otherwise we leave it
     */
    fun start(): Int {
        var count = 0
        if (!saveDir.isDirectory) throw Error("Content seeder active in non-directory")
        val fileList = saveDir.listFiles()
        if (fileList !is Array<File>) return count
        // A maximum of maxTorrentThreads torrents are seeded, with LIFO ordering
        Arrays.sort(fileList) { a, b -> a.lastModified().compareTo(b.lastModified()) }

        sessionManager.addListener(
            object : AlertListener {
                override fun types(): IntArray {
                    return intArrayOf(AlertType.PEER_CONNECT.swig())
                }

                override fun alert(alert: Alert<*>) {
                    when (alert.type()) {
                        AlertType.PEER_CONNECT -> {
                            val handle = (alert as PeerConnectAlert).handle()
                            updateSwarmHealth(handle)
                        }
                        else -> {
                        }
                    }
                }
            }
        )

        if (!sessionManager.isRunning) {
            sessionManager.start()
        }

        saveDir.listFiles()?.forEachIndexed { index, file ->
            if (index >= maxTorrentThreads) return count
            if (file.name.endsWith(".torrent")) {
                val torrentInfo = TorrentInfo(file)
                if (torrentInfo.isValid) {
                    // 'Downloading' the torrent file also starts seeding it after download has
                    // already been completed
                    // We only seed torrents that have previously already been fully downloaded
                    if (Util.isTorrentCompleted(torrentInfo, saveDir)) {
                        downloadAndSeed(torrentInfo)
                        count += 1
                    }
                }
            }
        }
        return count
    }

    /**
     * Keep track of connected peers/seeders per torrent by using a local swarmHealthMap
     */
    fun updateSwarmHealth(handle: TorrentHandle) {
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

    private fun downloadAndSeed(torrentInfo: TorrentInfo) {
        if (torrentInfo.isValid) {
            torrentInfo.addTracker("udp://130.161.119.207:8000/announce")
            torrentInfo.addTracker("http://130.161.119.207:8000/announce")
            torrentInfo.addTracker("udp://130.161.119.207:8000")
            torrentInfo.addTracker("http://130.161.119.207:8000")
            sessionManager.download(torrentInfo, saveDir)
            val torrentHandle = sessionManager.find(torrentInfo.infoHash()) ?: return
            torrentHandle.setFlags(torrentHandle.flags().and_(TorrentFlags.SEED_MODE))
            torrentHandle.pause()
            torrentHandle.resume() // This is a fix/hack that forces SEED_MODE to be available, for
            // an unsolved issue: seeding local torrents often result in an endless "CHECKING_FILES"
            // state
            // Start by setting swarm connectivity to 1 as the current device is now 1 seeder
            updateSwarmHealth(torrentHandle)
        }
    }

    /**
     * Create and save a torrent file with TorrentInfo
     * @param torrentInfoName the torrentinfo.name() parameter
     */
    fun saveTorrentInfoToFile(torrentInfo: TorrentInfo, torrentInfoName: String): Boolean {
        val path = "$saveDir/$torrentInfoName.torrent"
        val torrentFile = File(path)
        if (torrentInfo.isValid) {
            if (!torrentFile.isFile) {
                FileUtils.copyInputStreamToFile(torrentInfo.bencode().inputStream(), torrentFile)
            }
            return true
        }
        return false
    }

    companion object {
        fun getInstance(cacheDir: File, sessionManager: SessionManager): ContentSeeder {
            if (!::contentSeederInstance.isInitialized) {
                contentSeederInstance = ContentSeeder(cacheDir, sessionManager)
            }
            return contentSeederInstance
        }
    }
}
