package com.example.musicdao.repositories

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.musicdao.ipv8.SwarmHealth
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.*
import com.turn.ttorrent.client.SharedTorrent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

class TorrentRepository(
    val sessionManager: SessionManager,
    val directory: File,
) {
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val maxTorrentThreads = 10

    val seedingTorrents: MutableMap<String, String> = mutableMapOf()

    var swarmHealthMap: MutableMap<Sha1Hash, SwarmHealth> = mutableMapOf<Sha1Hash, SwarmHealth>()

    /**
     * Scan the saveDir directory for torrent files, and see if we can seed them. If all files have
     * been downloaded previously, we start seeding the respective torrent; otherwise we leave it
     */
    fun startSeeding(): Int {
        // Set-up session manager.
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

        // Set-up initial torrent seeding.
        val files = directory.listFiles()

        if (!directory.isDirectory) {
            throw Error("Content seeder active in non-directory")
        }
        if (files !is Array<File>) {
            return 0
        }

        // A maximum of maxTorrentThreads torrents are seeded, with LIFO ordering.
        val torrentFiles = files
            .filter { file -> file.name.endsWith(".torrent") }
            .sortedBy { file -> file.lastModified() }
            .take(maxTorrentThreads)

        val validTorrentInfos = torrentFiles
            .map { file -> TorrentInfo(file) }
            .filter { torrentInfo -> torrentInfo.isValid }
            .filter { torrentInfo -> Util.isTorrentCompleted(torrentInfo, directory) }


        // 'Downloading' the torrent file also starts seeding it after download has
        // already been completed.
        // We only seed torrents that have previously already been fully downloaded.
        validTorrentInfos.forEach { torrentInfo ->
            if (Util.isTorrentCompleted(torrentInfo, directory)) {
                downloadAndSeed(torrentInfo)

            }
        }

        return validTorrentInfos.size
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
//            TorrentRepository.addTrackers(torrentInfo)
            sessionManager.download(torrentInfo, directory)
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
        val path = "$directory/$torrentInfoName.torrent"
        val torrentFile = File(path)
        if (torrentInfo.isValid) {
            if (!torrentFile.isFile) {
                FileUtils.copyInputStreamToFile(torrentInfo.bencode().inputStream(), torrentFile)
            }
            return true
        }
        return false
    }

    fun isDownloaded(torrentInfoName: String): Boolean {
        val torrentInfo = getTorrentInfoFromDisk(torrentInfoName) ?: return false
        val res = Util.isTorrentCompleted(torrentInfo = torrentInfo, directory)

        return res
    }

    fun isDownloading(torrentInfoName: String): Boolean {
        val torrentInfo = getTorrentInfoFromDisk(torrentInfoName) ?: return false
        val handle = sessionManager.find(torrentInfo.infoHash())

        return handle != null
    }

    fun getHandle(torrentInfoName: String): TorrentHandle? {
        val torrentInfo = getTorrentInfoFromDisk(torrentInfoName) ?: return null
        val handle = sessionManager.find(torrentInfo.infoHash())
        return handle
    }

    fun getFiles(torrentInfoName: String): List<File> {
        val torrentInfo = getTorrentInfoFromDisk(torrentInfoName) ?: return listOf()
        val folder = File("$directory/$torrentInfoName")
        val files = folder.listFiles().toList().filter {
            it.extension == "mp3"
        }
        return files
    }


    fun generateTorrent(context: Context, uris: List<Uri>): File {
        val contentResolver = context.contentResolver
        val randomInt = Random.nextInt(0, Int.MAX_VALUE)
        val parentDir = "${context.cacheDir}/$randomInt"

        val fileList = mutableListOf<File>()
        val projection =
            arrayOf<String>(MediaStore.MediaColumns.DISPLAY_NAME)
        for (uri in uris) {

            var fileName = ""
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(0)
                }
            }

            if (fileName == "") throw Error("Source file name for creating torrent not found")
            val input =
                contentResolver.openInputStream(uri) ?: throw Resources.NotFoundException()
            val fileLocation = "$parentDir/$fileName"

            Log.d("Brian", "Generate torrent: ${fileName}")
            Log.d("Brian", "Generate torrent: ${fileLocation}")

            FileUtils.copyInputStreamToFile(input, File(fileLocation))
            fileList.add(File(fileLocation))
            Log.d("Brian", "Generate torrent: ${File(fileLocation)}")
        }

        val torrent = SharedTorrent.create(File(parentDir), fileList, 65535, listOf(), "")
        val torrentFile = "$parentDir.torrent"
        torrent.save(FileOutputStream(torrentFile))
        return File(torrentFile)
    }

    suspend fun startDownload(
        torrentInfoName: String,
        magnet: String,
        callback: () -> Unit
    ): TorrentHandle? {
        // Attempt to get torrent file on disk
        var torrentInfo = getTorrentInfoFromDisk(torrentInfoName)
        if (torrentInfo == null) {
            torrentInfo = attemptToGetTorrentInfoFileAndSaveIt(torrentInfoName, magnet)

        }
        if (torrentInfo == null) {
            return null
        }

        // TODO: Check if download is already happening

        addTrackers(torrentInfo)
        sessionManager.download(torrentInfo, directory)
        val torrentHandle = sessionManager.find(torrentInfo.infoHash())
        Util.setTorrentPriorities(torrentHandle, false, 0, 0)
        torrentHandle.pause()
        torrentHandle.resume()

        val listener = object : AlertListener {
            override fun types(): IntArray {
                return intArrayOf(
                    AlertType.PIECE_FINISHED.swig(),
                    AlertType.TORRENT_FINISHED.swig()
                )
            }

            override fun alert(alert: Alert<*>) {
                when (alert.type()) {
                    AlertType.PIECE_FINISHED -> {
                        val handle = (alert as PieceFinishedAlert).handle()
                        if (handle.infoHash() != torrentHandle?.infoHash()) return

                        callback()
                    }
                    AlertType.TORRENT_FINISHED -> {
                        val handle = (alert as TorrentFinishedAlert).handle()
                        if (handle.infoHash() != torrentHandle?.infoHash()) return
                        callback()
                    }
                    else -> {
                    }
                }
            }
        }

        sessionManager.addListener(listener)
        return torrentHandle
    }


    private suspend fun attemptToGetTorrentInfoFileAndSaveIt(
        torrentInfoName: String,
        magnet: String
    ): TorrentInfo? {
        val torrentInfo = getTorrentInfoFromNetwork(magnet = magnet)

        if (torrentInfo != null) {
            saveTorrentInfoToDisk(torrentInfo, torrentInfoName = torrentInfoName)
            return getTorrentInfoFromDisk(torrentInfoName)
        }
        return null
    }

    private fun saveTorrentInfoToDisk(torrentInfo: TorrentInfo, torrentInfoName: String): File? {
        val path = "${directory}/$torrentInfoName.torrent"
        val torrentFile = File(path)
        if (torrentInfo.isValid) {
            if (!torrentFile.isFile) {
                FileUtils.copyInputStreamToFile(torrentInfo.bencode().inputStream(), torrentFile)
                return torrentFile
            }
            return null
        }
        return null
    }

    private suspend fun getTorrentInfoFromNetwork(magnet: String): TorrentInfo? {
        return withContext(dispatcher) {
            val magnetWithTrackers = Util.addTrackersToMagnet(magnet)
            val torrentData = sessionManager.fetchMagnet(magnetWithTrackers, 10)
            if (torrentData != null) {
                TorrentInfo.bdecode(torrentData)
            } else {
                null
            }
        }
    }

    private fun getTorrentInfoFromDisk(torrentInfoName: String): TorrentInfo? {
        val file = File("${directory}/$torrentInfoName.torrent")
        return if (file.isFile) {
            return TorrentInfo(file)
        } else {
            null
        }
    }

    fun isSeeding(torrentInfoName: String): Boolean {
        val handle = getHandle(torrentInfoName) ?: return false
        return handle.status().isSeeding
    }

    companion object {
        fun addTrackers(torrentInfo: TorrentInfo) {
            torrentInfo.addTracker("udp://open.tracker.cl:1337/announce")
            torrentInfo.addTracker("udp://tracker.opentrackr.org:1337/announce")
            torrentInfo.addTracker("udp://9.rarbg.com:2810/announce")
            torrentInfo.addTracker("udp://tracker.openbittorrent.com:6969/announce")
            torrentInfo.addTracker("udp://exodus.desync.com:6969/announce")
            torrentInfo.addTracker("http://tracker.openbittorrent.com:80/announce")
            torrentInfo.addTracker("udp://www.torrent.eu.org:451/announce")
            torrentInfo.addTracker("udp://tracker.zerobytes.xyz:1337/announce")
            torrentInfo.addTracker("udp://tracker.torrent.eu.org:451/announce")
            torrentInfo.addTracker("udp://tracker.tiny-vps.com:6969/announce")
            torrentInfo.addTracker("udp://tracker.pomf.se:80/announce")
            torrentInfo.addTracker("udp://tracker.moeking.me:6969/announce")
            torrentInfo.addTracker("udp://tracker.bitsearch.to:1337/announce")
            torrentInfo.addTracker("udp://tracker-udp.gbitt.info:80/announce")
            torrentInfo.addTracker("udp://retracker.netbynet.ru:2710/announce")
            torrentInfo.addTracker("udp://opentor.org:2710/announce")
            torrentInfo.addTracker("udp://open.stealth.si:80/announce")
            torrentInfo.addTracker("udp://movies.zsw.ca:6969/announce")
            torrentInfo.addTracker("udp://explodie.org:6969/announce")
            torrentInfo.addTracker("udp://bt2.archive.org:6969/announce")
        }
    }


}
