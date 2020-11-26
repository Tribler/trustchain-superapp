package com.example.musicdao.net

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.example.musicdao.ipv8.SwarmHealth
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.masterwok.simpletorrentandroid.TorrentSession
import com.masterwok.simpletorrentandroid.TorrentSessionOptions
import com.masterwok.simpletorrentandroid.contracts.TorrentSessionListener
import com.masterwok.simpletorrentandroid.models.TorrentSessionStatus
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*


lateinit var contentSeederInstance: ContentSeeder

/**
 * This maintains an implementation of a strategy of libtorrent seeding.
 * Currently, all files in the cache directory are being seeded.
 */
class ContentSeeder(private val saveDir: File, private val context: Context) {
    private val maxTorrentThreads = 10
    private var started = false
    private val torrentSessionOptions = TorrentSessionOptions(
        downloadLocation = saveDir,
        onlyDownloadLargestFile = false,
        enableLogging = false,
        shouldStream = false
    )

    val torrentSession = TorrentSession(torrentSessionOptions)

    var swarmHealthMap: MutableMap<Sha1Hash, SwarmHealth> = mutableMapOf<Sha1Hash, SwarmHealth>()

    fun start(): Int {
        var count = 0
        if (started) return count
        started = true
        if (!saveDir.isDirectory) throw Error("Content seeder active in non-directory")
        val fileList = saveDir.listFiles()
        if (fileList !is Array<File>) return count
        Arrays.sort(fileList) { a, b -> a.lastModified().compareTo(b.lastModified()) }

//        sessionManager.addListener(object : AlertListener {
//            private var forceChecked = false
//            override fun types(): IntArray? {
//                return null
//            }
//
//            override fun alert(alert: Alert<*>) {
//                when (alert.type()) {
////                    AlertType.ADD_TORRENT -> {
////                        val handle = (alert as AddTorrentAlert).params()
////                        handle.stor
////                    }
//                    AlertType.PIECE_FINISHED -> {
//                        val progress = ((alert as PieceFinishedAlert).handle().status()
//                            .progress() * 100).toInt()
//                        // this number represents the current progress of
//                        // the current status (downloading or checking)
//                        if (progress > 2 && !forceChecked) {
//                            forceChecked = true
//                            alert.handle().forceRecheck()
//                        }
//                    }
//                    AlertType.PEER_CONNECT -> {
//                        val th = (alert as PeerConnectAlert).handle()
//                        val numPeers = th.status().numPeers()
//                        val numSeeds = th.status().numSeeds()
//                        // Add to local database to keep track of connectivity of each item
//                        Log.d(
//                            "ContentSeeder",
//                            "Peer connected: $numPeers peers," +
//                                " $numSeeds seeds for ${th.infoHash()}"
//                        )
//
//                        swarmHealthMap[th.infoHash()] = SwarmHealth(
//                            th.infoHash().toString(),
//                            numPeers.toUInt(),
//                            numSeeds.toUInt()
//                        )
//                    }
//                    else -> {
//
//                    }
//                }
//            }
//        })
//
//        sessionManager.start()

        torrentSession.listener = object : TorrentSessionListener {
            override fun onAddTorrent(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
            }

            override fun onBlockUploaded(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
            }

            override fun onMetadataFailed(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {

            }

            override fun onMetadataReceived(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
            }

            override fun onPieceFinished(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
            }

            override fun onTorrentDeleteFailed(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
            }

            override fun onTorrentDeleted(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
            }

            override fun onTorrentError(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
            }

            override fun onTorrentFinished(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
                val numPeers = torrentHandle.status().numPeers()
                val numSeeds = torrentHandle.status().numSeeds() + 1 // TODO is this correct?
                // Add to local database to keep track of connectivity of each item
                Log.d(
                    "ContentSeeder",
                    "Peer connected: $numPeers peers," +
                        " $numSeeds seeds for ${torrentHandle.infoHash()}"
                )

                swarmHealthMap[torrentHandle.infoHash()] = SwarmHealth(
                    torrentHandle.infoHash().toString(),
                    numPeers.toUInt(),
                    numSeeds.toUInt()
                )
            }

            override fun onTorrentPaused(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
            }

            override fun onTorrentRemoved(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
            }

            override fun onTorrentResumed(
                torrentHandle: TorrentHandle,
                torrentSessionStatus: TorrentSessionStatus
            ) {
            }
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
                        val torrentUri = file.toUri()

                        torrentSession.start(context, torrentUri)
                        // enable bootstrapping from public routers
//                        val dhtModule = DHTModule(DHTConfig())
//
//                        val config = Config()
//                        val client = BtClientBuilder()
//                            .config(config)
//                            .storage(FileSystemStorage(saveDir.toPath()))
//                            .torrent(file.toURI().toURL())
//                            .autoLoadModules()
//                            .build()
//
//                        client.startAsync()
//                        sessionManager.download(torrentInfo, saveDir)
//                        val torrentHandle = sessionManager.find(torrentInfo.infoHash())
//                        torrentHandle.setFlags(
//                            torrentHandle.flags().and_(TorrentFlags.SEED_MODE)
//                                .and_(TorrentFlags.UPLOAD_MODE)
//                        )
//                        if (torrentHandle != null) {
//                            var flags = torrentHandle.flags()
//                            flags = flags.and_(TorrentFlags.AUTO_MANAGED.inv())
//                            flags = flags.and_(TorrentFlags.SEED_MODE.inv())
//                            torrentHandle.setFlags(flags)
//                        }
//                        println(torrentHandle.flags().toString())
//                        sessionManager.download(torrentInfo, saveDir)
//                        sessionManager.download(torrentInfo, saveDir)
//                        val torrentHandle = sessionManager.find(torrentInfo.infoHash())
                        //                        val activeTorrent = sessionManager.find(torrentInfo.infoHash())
                        // This is a hack to override file priorities, so that the sessionManager
                        // immediately starts seeding, as we already know that all files have been
                        // completed previously (tested with the Util.isTorrentCompleted call)
//                        sessionManager.dhtAnnounce(torrentInfo.infoHash()) // TODO does this do anything?
                    }
                }
            }
        }
        return count
    }

    private fun overrideFilePriorities(torrentHandle: TorrentHandle, priority: Priority) {
        for ((i, _) in torrentHandle.filePriorities().withIndex()) {
            torrentHandle.filePriority(i, priority)
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
            torrentSession.start(context, torrentFile.toUri())
            return true
        }
        return false
    }

    companion object {
        fun getInstance(cacheDir: File, context: Context): ContentSeeder {
            if (!::contentSeederInstance.isInitialized) {
                contentSeederInstance = ContentSeeder(cacheDir, context)
            }
            return contentSeederInstance
        }
    }
}
