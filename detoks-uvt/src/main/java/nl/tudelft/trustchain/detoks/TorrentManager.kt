package nl.tudelft.trustchain.detoks

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.detoks.recommendation.Recommender
import nl.tudelft.trustchain.detoks.util.MagnetUtils
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.Pair


/**
 * This class manages the torrent files and the video pool.
 * It is responsible for downloading the torrent files and caching the videos.
 * It also provides the videos to the video adapter.
 */
class TorrentManager(
    private val cacheDir: File,
    private val torrentDir: File,
    private val seedableTorrentsDir: File,
    private val cachingAmount: Int = 1,
) {
    private val sessionManager = SessionManager()
    private val logger = KotlinLogging.logger {}
    private val torrentFiles = mutableListOf<TorrentHandlerPlusUserInfo>()
    private var currentIndex = 0
    private val torrentHandleBeingSeeded = mutableListOf<TorrentHandle>()
    private val seedableTorrentInfo = mutableListOf<TorrentInfo>()
    init {
        clearMediaCache()
        initializeSessionManager()
        buildTorrentIndex()
        initializeVideoPool()
        initializeSeedableTorrentInfo()
    }

    fun getAllTorrents(): List<TorrentHandler> {
        return torrentFiles
    }

    fun addNewVideo(proposalBlockHash: String, videoPostedOn: String, videoID: String) {
        val lastTorrentHandler = torrentFiles.lastOrNull()!!
        torrentFiles.add(TorrentHandlerPlusUserInfo(cacheDir,
            lastTorrentHandler.handle,
            lastTorrentHandler.torrentName,
            lastTorrentHandler.fileName,
            lastTorrentHandler.fileIndex,
            proposalBlockHash,
            videoPostedOn,
            videoID))
    }

    fun getHashOfCurrentVideo(): String {
        return torrentFiles.gett(currentIndex % getNumberOfTorrents()).proposalBlockHash
    }

    fun notifyIncrease() {
        Log.i("DeToks", "Increasing index ... ${(currentIndex + 1) % getNumberOfTorrents()}")
        notifyChange((currentIndex + 1) % getNumberOfTorrents(), loopedToFront = true)

        val recommendedVideo: List<String>? = Recommender.getNextRecommendation()
        if (recommendedVideo != null && recommendedVideo.size == 4) {
            Log.i("DeToks", "Now adding this recommended video ID: $recommendedVideo to peers video feed")
            addTorrent(recommendedVideo[0], recommendedVideo[1], recommendedVideo[2], recommendedVideo[3])
        } else {
            Log.i("DeToks", "Could not get recommended video")
        }
    }

    fun notifyDecrease() {
        Log.i("DeToks", "Decreasing index ... ${(currentIndex - 1) % getNumberOfTorrents()}")
        notifyChange((currentIndex - 1) % getNumberOfTorrents())
    }

    /**
     * This function provides the video at the given index.
     * If the video is not downloaded yet, it will wait for it to be downloaded.
     * If the video is not downloaded after the timeout, it will return the video anyway.
     */
    suspend fun provideContent(index: Int = currentIndex, timeout: Long = 10000): TorrentMediaInfo {
        val numTorrents = getNumberOfTorrents()
        Log.i("DeToks", "Providing content ... $index, ${index % numTorrents}, $numTorrents")
        val content = torrentFiles.gett(index % getNumberOfTorrents())

        return try {
            withTimeout(timeout) {
                Log.i("DeToks", "Waiting for content ... $index")
                while (!content.isDownloaded()) {
                    delay(100)
                }
            }
            mvpSeeder(content)
            content.asMediaInfo()
        } catch (e: TimeoutCancellationException) {
            Log.i("DeToks", "Timeout for content ... $index")
            Log.i("DeToks", "Timeout for content ... ${content.asMediaInfo().fileName}")
            content.asMediaInfo()
        }
    }

    fun mvpSeeder(videoTorrentHandler: TorrentHandler): MutableList<Pair<String, String>> {
        val peerInfo = videoTorrentHandler.handle.peerInfo()
        // list of peer info objects -> you can get the
        // bit torrent client and ip address of the peers
        val downloadedFrom = mutableListOf<Pair<String, String>>()
        for (peer in peerInfo) {
            if (peer.totalDownload() > 0) {
                Log.i("Detoks", "---------------------------------------------------------")
                Log.i("Detoks", "downloaded from peer with ip adress ${peer.ip()}")
                Log.i("Detoks", "this peer uses the following client ${peer.client()}")
                Log.i("Detoks", "amount downloaded from this peer: ${peer.totalDownload()}")
                Log.i("Detoks", "-----------------------------------------------------------")
                downloadedFrom.add(Pair(peer.client(), peer.ip()))
            }
//            peer.ip()
//            peer.client()
        }
        return downloadedFrom
    }


    fun mvpSeeder(): MutableList<Triple<String, String, Long>> {
        val peerInfo = torrentFiles.gett(currentIndex % getNumberOfTorrents()).handle.peerInfo()
        // list of peer info objects -> you can get the
        // bit torrent client and ip address of the peers
        val downloadedFrom = mutableListOf<Triple<String, String, Long>>()
        for (peer in peerInfo) {
            if (peer.totalDownload() > 0) {
                Log.i("Detoks", "---------------------------------------------------------")
                Log.i("Detoks", "downloaded from peer with ip address ${peer.ip()}")
                Log.i("Detoks", "this peer uses the following client ${peer.client()}")
                Log.i("Detoks", "amount downloaded from this peer: ${peer.totalDownload()}")
                Log.i("Detoks", "-----------------------------------------------------------")
                downloadedFrom.add(Triple(peer.client(), peer.ip(), peer.totalDownload()))
            }
//            peer.ip()
//            peer.client()
        }
        return downloadedFrom
    }

    fun getNumberOfTorrents(): Int {
        return torrentFiles.size
    }

    /**
     * This functions updates the current index of the cache.
     */
    private fun notifyChange(
        newIndex: Int,
        loopedToFront: Boolean = false
    ) {
        if (newIndex == currentIndex) {
            return
        }
        if (cachingAmount * 2 + 1 >= getNumberOfTorrents()) {
            currentIndex = newIndex
            return
        }

        if (newIndex > currentIndex || loopedToFront) {
            //TODO make sure to not delete files that are being seeded
            torrentFiles.gett(currentIndex - cachingAmount).deleteFile()
            torrentFiles.gett(newIndex + cachingAmount).downloadFile()
        } else {
            //TODO make sure to not delete files that are being seeded
            torrentFiles.gett(currentIndex + cachingAmount).deleteFile()
            torrentFiles.gett(newIndex - cachingAmount).downloadFile()

        }
        currentIndex = newIndex
    }

    private fun initializeVideoPool() {
        if (torrentFiles.size < cachingAmount * 2) {
            logger.error("Not enough torrents to initialize video pool")
            return
        }
        for (i in (currentIndex - cachingAmount)..(currentIndex + cachingAmount)) {
            val torrent = torrentFiles.gett(i)
            if (i == currentIndex) {
                torrent.downloadWithMaxPriority()
            } else {
                torrent.downloadFile()
            }
        }
    }

    fun addTorrent(magnet: String, proposalBlockHash: String, videoPostedOn: String, videoID: String) {
        val torrentInfo = getInfoFromMagnet(magnet)?:return
        val hash = torrentInfo.infoHash()
        var handle = sessionManager.find(hash)

        if (sessionManager.find(hash) == null) {
            // no need to download again if it was already downloaded before: prevents warning and crash bugs
            Log.i("Detoks", "there is no torrenthandle yet for the video with this hash: $proposalBlockHash \n and this magnetlink: $magnet")
            sessionManager.download(torrentInfo, cacheDir)
            var handle = sessionManager.find(hash)
            handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
            handle.prioritizeFiles(arrayOf(Priority.IGNORE))
            handle.pause()
        }

        Log.i("Detoks", "Adding new torrent: ${torrentInfo.name()}")


        for (it in 0 until torrentInfo.numFiles()) {
            val fileName = torrentInfo.files().fileName(it)
            if (fileName.endsWith(".mp4")) {
                torrentFiles.add(
                    TorrentHandlerPlusUserInfo(
                        cacheDir,
                        handle,
                        torrentInfo.name(),
                        fileName,
                        it,
                        proposalBlockHash,
                        videoPostedOn,
                        videoID
                    )
                )
                Log.i("Detoks", "Received and added new video data: magnet link: $magnet, proposalBlockHash: $proposalBlockHash")
            }
        }
    }

    private fun getInfoFromMagnet(magnet: String): TorrentInfo? {
        val bytes = sessionManager.fetchMagnet(magnet, 10)
        if (bytes == null) {
            Log.e("Detoks", "Failed to fetch torrentInfo from following magnet link: $magnet")
            return null
        }
        return TorrentInfo.bdecode(bytes)
    }

    /**
     * Start seeding the video that is currently on the screen
     * To be called when the user double clicks, //TODO first check if the current video is created by someone else first
     * or else the user will be seeding a video that wasn't posted by anyone
     * @return the magnet link of that is being seeded or null if failed to seed
     */
    fun seedLikedVideo(): String? {
        val torrentHandle = torrentFiles.gett(currentIndex % getNumberOfTorrents()).handle
        return if (torrentHandle.isValid) {
            torrentHandle.setFlags(torrentHandle.flags().and_(TorrentFlags.SEED_MODE))
            torrentHandle.pause()
            torrentHandle.resume()
            // This is a fix/hack that forces SEED_MODE to be available, for
            // an unsolved issue: seeding local torrents often result in an endless "CHECKING_FILES"
            // state
            Log.i("Detoks", "Now seeding the video you double clicked, torrent with magnetLink: ${torrentHandle.makeMagnetUri()}")
            torrentHandleBeingSeeded.add(torrentHandle)
            torrentHandle.makeMagnetUri()
        } else {
            null
        }
    }

    /**
     * Downloads and seeds the torrent specified by @param torrentInfoName
     * @return null if downloading and seeding failed, the magnet link if downloading and seeding was successful
     */
    fun seedTorrent(torrentInfoName: String): String? {
        downloadAndSeed(torrentInfoName)
        return torrentHandleBeingSeeded.firstOrNull { it.torrentFile().name() == torrentInfoName }
            ?.makeMagnetUri()
    }

    fun seedTorrentFromMagnetLink(magnetLink: String): Boolean {
        val torrentInfo = getInfoFromMagnet(magnetLink)?:return false
        val hash = torrentInfo.infoHash()
        if(sessionManager.find(hash) != null) return true // return true if already seeding this torrent
        Log.i("Detoks", "Downloading and seeding new torrent: ${torrentInfo.name()}")
        if (torrentInfo.isValid) {
            sessionManager.download(torrentInfo, cacheDir)
        } else {
            return false
        }
        val torrentHandle = sessionManager.find(torrentInfo.infoHash())
        return if (torrentHandle.isValid) {
            torrentHandle.setFlags(torrentHandle.flags().and_(TorrentFlags.SEED_MODE))
            torrentHandle.pause()
            torrentHandle.resume()
            // This is a fix/hack that forces SEED_MODE to be available, for
            // an unsolved issue: seeding local torrents often result in an endless "CHECKING_FILES"
            // state
            Log.i("Detoks", "Now seeding torrent with magnetLink: ${torrentHandle.makeMagnetUri()}")
            torrentHandleBeingSeeded.add(torrentHandle)
            true
        } else {
            false
        }
    }

    /**
     * Downloads and seeds the torrent which has a name that matches @param torrentInfoName
     * This method is adapted from AppGossiper.kt which is in package nl.tudelft.trustchain.FOC
     */
    private fun downloadAndSeed(torrentInfoName: String) {
        val torrentInfo = seedableTorrentInfo.firstOrNull { it.name() == torrentInfoName }
        if (torrentInfo != null) {
            if (torrentInfo.isValid) {
                sessionManager.download(torrentInfo, cacheDir)
                sessionManager.find(torrentInfo.infoHash())?.let { torrentHandle ->
                    if (torrentHandle.isValid) {
                        torrentHandle.setFlags(torrentHandle.flags().and_(TorrentFlags.SEED_MODE))
                        torrentHandle.pause()
                        torrentHandle.resume()
                        // This is a fix/hack that forces SEED_MODE to be available, for
                        // an unsolved issue: seeding local torrents often result in an endless "CHECKING_FILES"
                        // state
                        Log.i("Detoks", "Now seeding torrent with magnetLink: ${torrentHandle.makeMagnetUri()}")
                        torrentHandleBeingSeeded.add(torrentHandle)
                    }
                }
            }
        } else {
            Log.e("Detoks" , "there is no torrent with name ${torrentInfoName}, as a result it won't be seeded")
        }
    }



    /**
     * Retrieves the names of the mp4 videos in the res folder the user can post
     */
    fun getSeedableTorrents(): List<String> {
        val alreadySeeding = torrentHandleBeingSeeded.map {it.name()}
        return seedableTorrentInfo.filter { !alreadySeeding.contains(it.name()) }
            .map { it.name() }
    }

    private fun initializeSeedableTorrentInfo() {
        val files = seedableTorrentsDir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.extension == "torrent") {
                    val torrentInfo = TorrentInfo(file)
                    seedableTorrentInfo.add(torrentInfo)
                }
            }
        }
    }

    /**
     * This function builds the torrent index. It adds all the torrent files in the torrent
     * directory to Libtorrent and selects all .mp4 files for download.
     */
    private fun buildTorrentIndex() {
        val files = torrentDir.listFiles() ?: return

        for (file in files) {
            if (file.extension != "torrent")
                continue

            val torrentInfo = TorrentInfo(file)
            sessionManager.download(torrentInfo, cacheDir)
            val handle = sessionManager.find(torrentInfo.infoHash())
            handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
            val priorities = Array(torrentInfo.numFiles()) { Priority.IGNORE }
            handle.prioritizeFiles(priorities)
            handle.pause()
            Log.i("Detoks", "name of the torrent file is: ${torrentInfo.name()}")
            Log.i("Detoks", "The magnet URI created by .makeMagnetUri() function is ${handle.makeMagnetUri()}")
            Log.i("Detoks", "The magnet URI create by constructMagnetLink() is ${MagnetUtils.constructMagnetLink(torrentInfo.infoHash(), torrentInfo.name())}")
            for (it in 0..torrentInfo.numFiles()) {
                val fileName = torrentInfo.files().fileName(it)
                if (fileName.endsWith(".mp4")) {
                    torrentFiles.add(
                        TorrentHandlerPlusUserInfo(
                            cacheDir,
                            handle,
                            torrentInfo.name(),
                            fileName,
                            it,
                            "", "", ""
                        )
                    )
                }
            }
        }
    }

    private fun initializeSessionManager() {
        sessionManager.addListener(object : AlertListener {
            override fun types(): IntArray? {
                return null
            }

            @RequiresApi(Build.VERSION_CODES.N)
            override fun alert(alert: Alert<*>) {
                when (alert.type()) {
                    AlertType.ADD_TORRENT -> {
                        val a = alert as AddTorrentAlert
                        println("Torrent added: ${a.torrentName()}")
                    }
                    AlertType.BLOCK_FINISHED -> {
                        val a = alert as BlockFinishedAlert
                        val p = (a.handle().status().progress() * 100).toInt()
                        logger.info { ("Progress: " + p + " for torrent name: " + a.torrentName()) }
                        logger.info { sessionManager.stats().totalDownload() }
                    }
                    AlertType.TORRENT_FINISHED -> {
                        logger.info { "Torrent finished" }
                    }
                    else -> {}
                }
            }
        })

        sessionManager.start()
    }

    private fun clearMediaCache() {
        deleteRecursive(cacheDir)
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) for (child in fileOrDirectory.listFiles()!!) deleteRecursive(
            child
        )
        fileOrDirectory.delete()
    }

    class TorrentHandlerPlusUserInfo(cacheDir: File,
                                     handle: TorrentHandle,
                                     torrentName: String,
                                     fileName: String,
                                     fileIndex: Int,
                                     val proposalBlockHash: String,
                                     val videoPostedOn: String,
                                     val videoID: String):
        TorrentHandler(cacheDir, handle, torrentName, fileName, fileIndex){
        override fun asMediaInfo(): TorrentMediaInfo {
            return TorrentMediaInfo(torrentName, fileName, getPath(), proposalBlockHash, videoPostedOn, videoID)
        }
    }


    open class TorrentHandler(
        private val cacheDir: File,
        val handle: TorrentHandle,
        val torrentName: String,
        val fileName: String,
        val fileIndex: Int
    ) {

        var isDownloading: Boolean = false

        fun getPath(): String {
            return "$cacheDir/$torrentName/$fileName"
        }

        fun isPlayable(): Boolean {
            return handle.fileProgress()[fileIndex] / handle.torrentFile().files()
                .fileSize(fileIndex) > 0.8
        }

        fun isDownloaded(): Boolean {
            return handle.fileProgress()[fileIndex] == handle.torrentFile().files()
                .fileSize(fileIndex)
        }

        fun deleteFile() {
            handle.filePriority(fileIndex, Priority.IGNORE)
            val file = File("$cacheDir/$torrentName/$fileName")
            if (file.exists()) {
                file.delete()
            }
            isDownloading = false
        }

        fun downloadWithMaxPriority() {
            downloadFile()
            setMaximumPriority()
        }

        fun downloadFile() {
            if (isDownloading) {
                return
            }
            isDownloading = true
            handle.resume()
            handle.forceRecheck()
            handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
            handle.filePriority(fileIndex, Priority.NORMAL)
            handle.pause()
            handle.resume()
        }

        fun setMaximumPriority() {
            handle.resume()
            handle.filePriority(fileIndex, Priority.SEVEN)
            handle.pause()
            handle.resume()
        }

        open fun asMediaInfo(): TorrentMediaInfo {
            return TorrentMediaInfo(torrentName, fileName, getPath(), "", "" ,"")
        }

    }

    // Extension functions to loop around the index of a lists.
    private fun <E> List<E>.gett(index: Int): E = this[index.mod(size)]

    private fun <E> List<E>.gettIndex(index: Int): Int = index.mod(size)
}

class TorrentMediaInfo(
    val torrentName: String,
    val fileName: String,
    val fileURI: String,
    val proposalBlockHash: String,
    val videoPostedOn: String,
    val videoID: String
)
