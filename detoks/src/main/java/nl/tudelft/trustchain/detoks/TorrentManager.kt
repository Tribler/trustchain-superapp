package nl.tudelft.trustchain.detoks

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File


/**
 * This class manages the torrent files and the video pool.
 * It is responsible for downloading the torrent files and caching the videos.
 * It also provides the videos to the video adapter.
 */
class TorrentManager private constructor (
    private val cacheDir: File,
    private val torrentDir: File,
    private val cachingAmount: Int = 1,
) {
    private val sessionManager = SessionManager()
    private val logger = KotlinLogging.logger {}
    private val torrentFiles = mutableListOf<TorrentHandler>()

    private var unwatchedVideos = mutableListOf<TorrentHandler>()
    var seedingTorrents = mutableListOf<TorrentHandler>()
        private set

    val profile = Profile(HashMap())
    val strategies = Strategy()

    private var lastTimeStamp: Long
    private var currentIndex = 0

    init {
        clearMediaCache()
        initializeSessionManager()
        buildTorrentIndex()
        initializeVideoPool()
        lastTimeStamp = System.currentTimeMillis()
        unwatchedVideos = torrentFiles
    }

    companion object {
        private lateinit var instance: TorrentManager
        fun getInstance(context: Context): TorrentManager {
            if (!::instance.isInitialized) {
                instance = TorrentManager(
                    File("${context.cacheDir.absolutePath}/media"),
                    File("${context.cacheDir.absolutePath}/torrent"),
                    DeToksFragment.DEFAULT_CACHING_AMOUNT
                )
            }
            return instance
        }
    }

    fun notifyIncrease() {
        Log.i("DeToks", "Increasing index ... ${(currentIndex + 1) % getNumberOfTorrents()}")
        notifyChange((currentIndex + 1) % getNumberOfTorrents(), loopedToFront = true)
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
        Log.i("DeToks", "Providing content ... $index, ${index % getNumberOfTorrents()}")
        val content = torrentFiles.gett(index % getNumberOfTorrents())

        return try {
            withTimeout(timeout) {
                Log.i("DeToks", "Waiting for content ... $index")
                while (!content.isDownloaded()) {
                    delay(100)
                }
                profile.updateEntryDuration(
                    MagnetLink.hashFromMagnet(content.handle.makeMagnetUri()),
                    content.getVideoDuration())
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
     * Update the time and return the difference
     */
    private fun updateTime() : Long {
        val oldTimeStamp = lastTimeStamp
        lastTimeStamp = System.currentTimeMillis()
        return lastTimeStamp - oldTimeStamp
    }

    /**
     * This function updates the current index of the cache.
     */
    private fun notifyChange(
        newIndex: Int,
        loopedToFront: Boolean = false
    ) {
        if (newIndex == currentIndex) {
            return
        }
        if (cachingAmount * 2 + 1 >= getNumberOfTorrents()) {
            // TODO: This could potentially lead to issues, since what happens if the user locks
            //        their screen or switches to another app for a while? Maybe this could be
            //        changed to a place in the video adapter as well, if we can detect maybe when
            //        a video is done playing and starts again, then update the duration if possible
            val uri = torrentFiles.gett(currentIndex).handle.makeMagnetUri()    // TODO: make torrentFiles into unwatched videos
            profile.updateEntryWatchTime(
                MagnetLink.hashFromMagnet(uri),
                updateTime(),
                true)
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
        val uri = torrentFiles.gett(currentIndex).handle.makeMagnetUri()
        profile.updateEntryWatchTime(
            MagnetLink.hashFromMagnet(uri),
            updateTime(),
        true)
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
     * This function builds the torrent index. It adds all the torrent files in the torrent
     * directory to Libtorrent and selects all .mp4 files for download.
     */
    private fun buildTorrentIndex() {
        val files = torrentDir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.extension == "torrent") {
                    val torrentInfo = TorrentInfo(file)
                    sessionManager.download(torrentInfo, cacheDir)
                    val handle = sessionManager.find(torrentInfo.infoHash())
                    handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
                    val priorities = Array(torrentInfo.numFiles()) { Priority.IGNORE }
                    handle.prioritizeFiles(priorities)
                    handle.pause()
                    for (it in 0 until torrentInfo.numFiles()) {
                        val fileName = torrentInfo.files().fileName(it)
                        if (fileName.endsWith(".mp4")) {
                            val torrent = TorrentHandler(
                                cacheDir,
                                handle,
                                torrentInfo.name(),
                                fileName,
                                it
                            )
                            torrentFiles.add(torrent)
                            addProfile(torrent.handle.makeMagnetUri())
                        }
                    }
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

    fun addTorrent(hash: Sha1Hash, magnet: String) {
        profile.incrementTimesSeen(MagnetLink.hashFromMagnet(magnet))
        if (sessionManager.find(hash) != null) return

        val torrentInfo = getInfoFromMagnet(magnet)?:return

        Log.d(DeToksCommunity.LOGGING_TAG,"Adding new torrent: ${torrentInfo.name()}")

        sessionManager.download(torrentInfo, cacheDir)
        val handle = sessionManager.find(hash)
        handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
        handle.prioritizeFiles(arrayOf(Priority.IGNORE))
        handle.pause()

        for (it in 0 until torrentInfo.numFiles()) {
            val fileName = torrentInfo.files().fileName(it)
            if (fileName.endsWith(".mp4")) {
                val torrent = TorrentHandler(
                    cacheDir,
                    handle,
                    torrentInfo.name(),
                    fileName,
                    it
                )
                torrentFiles.add(torrent)
                addProfile(torrent.handle.makeMagnetUri())
            }
        }
    }

    fun addProfile(magnet: String) {
        profile.profiles[magnet] = ProfileEntry(
            uploadDate = (getInfoFromMagnet(magnet)?:return).creationDate())
    }

    fun updateLeachingStrategy(strategyId: Int) {
        if (strategies.leachingStrategy == strategyId) return

        strategies.leachingStrategy = strategyId

        currentIndex = 0
        unwatchedVideos = strategies.applyStrategy(strategyId, unwatchedVideos, profile.profiles)
    }

    fun updateSeedingStrategy(
        strategyId: Int = strategies.seedingStrategy,
        storageLimit: Int = strategies.storageLimit,
        isSeeding: Boolean = strategies.isSeeding
    ) {
        if (!isSeeding) return

        strategies.seedingStrategy = strategyId
        strategies.storageLimit = storageLimit

        val seedingTorrentsSorted = strategies.applyStrategy(strategyId, torrentFiles, profile.profiles)
        var storage: Long = 0

        val jobs = mutableListOf<Job>()

        val toStopSeeding = seedingTorrents.toMutableList()
        seedingTorrents.clear()


        for (i in 0 until seedingTorrentsSorted.size) {
            val size = seedingTorrentsSorted[i].handle.status().total() / 1000000 //TODO: store as byte to avoid all conversions

            if (storage + size > strategies.storageLimit) continue

            if (toStopSeeding.contains(seedingTorrentsSorted[i])) {
                toStopSeeding.remove(seedingTorrentsSorted[i])
                seedingTorrents.add(seedingTorrentsSorted[i])
                storage += size
                continue
            }

            jobs.add(CoroutineScope(Job() + Dispatchers.Default).launch {
                if (downloadAndSeed(seedingTorrentsSorted[i])) {
                    seedingTorrents.add(seedingTorrentsSorted[i])
                    storage += size
                }
            })
        }

        CoroutineScope(Job() + Dispatchers.Default).launch {
            toStopSeeding.forEach { stopSeedingTorrent(it) }
            jobs.forEach { it.join() }
        }
    }

    private suspend fun downloadAndSeed(handler: TorrentHandler, timeout: Long = 100000) : Boolean {
        if (!handler.handle.isValid) return false
        handler.downloadFile()

        try {
            withTimeout(timeout) {
                Log.d(DeToksCommunity.LOGGING_TAG, "Waiting to download ${handler.torrentName}")
                while (!handler.isDownloaded()) {
                    Log.d(DeToksCommunity.LOGGING_TAG, "Trying to download... ${handler.handle.status().totalWantedDone()} / ${handler.handle.status().totalWanted()}")
                    delay(300)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.d(DeToksCommunity.LOGGING_TAG, "Timeout for download ... ${handler.torrentName}")
            return false
        }

        handler.handle.setFlags(handler.handle.flags().and_(TorrentFlags.SEED_MODE))
        handler.handle.pause()
        handler.handle.resume()

        return true
    }

    fun stopSeeding() {
        Log.d(DeToksCommunity.LOGGING_TAG, "Stopping all seeding")
        seedingTorrents.forEach {
            stopSeedingTorrent(it)
        }
    }

    private fun stopSeedingTorrent(handler: TorrentHandler) {
        Log.d(DeToksCommunity.LOGGING_TAG, "Stopping seeding for torrent ${handler.torrentName}")
        handler.handle.unsetFlags(TorrentFlags.SEED_MODE)
        handler.handle.pause()
        handler.handle.resume()
        for (i in 0 until cachingAmount) {
            if(torrentFiles[i] == handler)
                return
        }
        handler.deleteFile()
    }

    fun setUploadRateLimit(bandwidth: Int) {
        Log.d(DeToksCommunity.LOGGING_TAG, "Updated the upload limit")
        sessionManager.uploadRateLimit(bandwidth)
    }

    private fun getInfoFromMagnet(magnet: String): TorrentInfo? {
        val bytes = sessionManager.fetchMagnet(magnet, 10)?:return null
        return TorrentInfo.bdecode(bytes)
    }

    fun getListOfTorrents(): List<TorrentHandle> {
        return torrentFiles.map {it.handle}.distinct()
    }

    fun getWatchedTorrents(): List<String> {
        return (profile.profiles.keys).toList()
    }

    fun getUnwatchedTorrents(): List<String> {
        return (torrentFiles.map { it.handle.makeMagnetUri() } subtract profile.profiles.keys).toList()
    }

    class TorrentHandler(
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

        fun getVideoDuration() : Long {
            if(!isDownloaded()) return 0
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(getPath())
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
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

        fun asMediaInfo(): TorrentMediaInfo {
            return TorrentMediaInfo(torrentName, fileName, getPath())
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
)

class MagnetLink {
    companion object {
        fun hashFromMagnet(magnet: String) : String {
            return magnet
                .substringAfter("xt=urn:btih:")
                .substringBefore("&")
        }
    }
}
