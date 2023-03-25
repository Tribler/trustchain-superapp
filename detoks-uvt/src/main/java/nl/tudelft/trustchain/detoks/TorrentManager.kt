package nl.tudelft.trustchain.detoks

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.AddTorrentParams
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import nl.tudelft.trustchain.detoks.recommendation.Recommender
import nl.tudelft.trustchain.detoks.util.MagnetUtils
import java.io.File
import java.util.ArrayList
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
    private val postVideosDir: File,
    private val cachingAmount: Int = 1,
) {
    private val sessionManager = SessionManager()
    private val logger = KotlinLogging.logger {}
    private val torrentFiles = mutableListOf<TorrentHandler>()
    private var currentIndex = 0
    private val torrentHandlesBeingSeeded = mutableMapOf<String, kotlin.Pair<TorrentHandle, String>>()
    internal var signal = CountDownLatch(0)
    var sessionActive = false
    init {
        clearMediaCache()
        initializeSessionManager()
        buildTorrentIndex()
        initializeVideoPool()
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

    fun notifyIncrease() {
        Log.i("DeToks", "Increasing index ... ${(currentIndex + 1) % getNumberOfTorrents()}")
        notifyChange((currentIndex + 1) % getNumberOfTorrents(), loopedToFront = true)

        val recommendedVideo: String? = Recommender.getNextRecommendation()
        if (recommendedVideo != null) {
            Log.i("DeToks", "Recommended video ID: $recommendedVideo")
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
            content.asMediaInfo()
        } catch (e: TimeoutCancellationException) {
            Log.i("DeToks", "Timeout for content ... $index")
            content.asMediaInfo()
        }
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
            torrentFiles.gett(currentIndex - cachingAmount).deleteFile()
            torrentFiles.gett(newIndex + cachingAmount).downloadFile()
        } else {
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

    /**
     * This function seeds a video.
     * To be used when a peer likes a video or when a peer posts a video
     * the returned string is the magnet link
     */
    fun seedVideo(videoFileName: String): String? {
        val fileToSeed = postVideosDir
            .listFiles()
            ?.toList()
            ?.firstOrNull { it.name == videoFileName }
        if (fileToSeed != null) {
            downloadAndSeed(TorrentInfo(fileToSeed), videoFileName)
        }
        val pair = torrentHandlesBeingSeeded.getOrDefault(videoFileName, null)
        return if (pair != null) {
            return pair.second
        } else {
            null
        }
    }

    fun addTorrent(magnet: String) {
        val torrentInfo = getInfoFromMagnet(magnet)?:return
        val hash = torrentInfo.infoHash()

        if(sessionManager.find(hash) != null) return
        logger.info {  "Adding new torrent: ${torrentInfo.name()}"}

        sessionManager.download(torrentInfo, cacheDir)
        val handle = sessionManager.find(hash)
        handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
        handle.prioritizeFiles(arrayOf(Priority.IGNORE))
        handle.pause()

        for (it in 0 until torrentInfo.numFiles()) {
            val fileName = torrentInfo.files().fileName(it)
            if (fileName.endsWith(".mp4")) {
                torrentFiles.add(
                    TorrentHandler(
                        cacheDir,
                        handle,
                        torrentInfo.name(),
                        fileName,
                        it
                    )
                )
            }
        }
    }

    private fun getInfoFromMagnet(magnet: String): TorrentInfo? {
        val bytes = sessionManager.fetchMagnet(magnet, 10)?:return null
        return TorrentInfo.bdecode(bytes)
    }

    fun downLoadMagnetLink(magnetLink: String){
        val sp = SettingsPack()
        sp.seedingOutgoingConnections(true)
        val params =
            SessionParams(sp)
        sessionManager.start(params)

        logger.info { "Fetching the magnet uri, please wait..." }
        val data: ByteArray
        try {
            data = sessionManager.fetchMagnet(magnetLink, 30)
        } catch (e: Exception) {
            logger.info { "Failed to retrieve the magnet" }
//            activity.runOnUiThread { printToast("Failed to fetch magnet info for $torrentName! error:$e") }
//            onTorrentDownloadFailure(torrentName, magnetInfoHash, peer)
            return
        }
        if (data != null) {

            val torrentInfo = TorrentInfo.bdecode(data)
//            sessionActive = true
//            signal = CountDownLatch(1)

            sessionManager.download(torrentInfo, cacheDir)
            val handle = sessionManager.find(torrentInfo.infoHash())
            handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
            val priorities = Array(torrentInfo.numFiles()) { Priority.IGNORE }
            handle.prioritizeFiles(priorities)
            handle.pause()
            for (it in 0..torrentInfo.numFiles()) {
                val fileName = torrentInfo.files().fileName(it)
                if (fileName.endsWith(".mp4")) {
                    torrentFiles.add(
                        TorrentHandler(
                            cacheDir,
                            handle,
                            torrentInfo.name(),
                            fileName,
                            it
                        )
                    )
                }
            }
//            activity.runOnUiThread { printToast("Managed to fetch torrent info for $torrentName, trying to download it via torrent!") }
            signal.await(1, TimeUnit.MINUTES)

            if (signal.count.toInt() == 1) {
//                activity.runOnUiThread { printToast("Attempt to download timed out for $torrentName!") }
                signal = CountDownLatch(0)
                sessionManager.find(torrentInfo.infoHash())?.let { torrentHandle ->
                    sessionManager.remove(torrentHandle)
                }
//                onTorrentDownloadFailure(torrentName, magnetInfoHash, peer)
            } else {
//                onDownloadSuccess(magnetName)
            }
            sessionActive = false
        } else {
            logger.info { "Failed to retrieve the magnet" }
//            activity.runOnUiThread { printToast("Failed to retrieve magnet for $torrentName!") }
//            onTorrentDownloadFailure(torrentName, magnetInfoHash, peer)
        }
    }



    /**
     * Downloads and seeds a torrent
     * This method is from AppGossiper.kt which is in package nl.tudelft.trustchain.FOC
     */
    private fun downloadAndSeed(torrentInfo: TorrentInfo, fileName: String) {
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

//                    torrentHandle.makeMagnetUri()
                    val magnetLink = MagnetUtils.constructMagnetLink(torrentInfo.infoHash(), torrentInfo.name())
//                    focCommunity.informAboutTorrent(magnetLink)
//                    torrentHandles.add(torrentHandle)
//                    torrentHandlesBeingSeeded.put(fileName, Pair(torrentHandle, magnetLink))
                    torrentHandlesBeingSeeded[fileName] = Pair(torrentHandle, magnetLink)
                }
            }
        }
    }

    /**
     * Retrieves the names of the mp4 videos in the res folder the user can post
     */
    fun getMP4videos(): List<String> {
//        MagnetUtils.constructMagnetLink()
        val files  = postVideosDir.listFiles()
        if (files != null) {
            return files
                .toList()
                .filter{ it.extension== "torrent"}
                .map{ it.name}
        }
        return emptyList()
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
//                    logger.info {"The magnet URI created by .makeMagnetUri() function is ${handle.makeMagnetUri()}"}
//                    logger.info {"The magnet URI create by constructMagnetLink() is ${MagnetUtils.constructMagnetLink(torrentInfo.infoHash(), torrentInfo.name())}" }
            for (it in 0..torrentInfo.numFiles()) {
                val fileName = torrentInfo.files().fileName(it)
                if (fileName.endsWith(".mp4")) {
                    torrentFiles.add(
                        TorrentHandler(
                            cacheDir,
                            handle,
                            torrentInfo.name(),
                            fileName,
                            it
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
