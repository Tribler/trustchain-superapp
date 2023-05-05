package nl.tudelft.trustchain.detoks

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.ipv8.android.IPv8Android
import java.io.File


/**
 * This class manages the torrent files and the video pool.
 * It is responsible for downloading the torrent files and caching the videos.
 * It also provides the videos to the video adapter.
 */
class TorrentManager private constructor(
    private val cacheDir: File,
    private val torrentDir: File,
    private val cachingAmount: Int = 1,
)  {
    private val sessionManager = SessionManager()
    private val logger = KotlinLogging.logger {}
    val torrentFiles = mutableListOf<TorrentHandler>()


    var seedingTorrents = mutableListOf<TorrentHandler>()
        private set

    var profitMap = HashMap<String, Float>()

    var balance = 0.0f
    val community: DeToksCommunity = IPv8Android.getInstance().getOverlay()!!

    val profile = Profile(HashMap())
    val strategies = Strategy()

    private var lastTimeStamp: Long
    private var currentIndex = 0
    private var unwatchedIndex = 0

    private var seedingStrategyJobs = mutableListOf<Job>()

    init {
        clearMediaCache()
        initializeSessionManager()
        buildTorrentIndex()
        initializeVideoPool()
        lastTimeStamp = System.currentTimeMillis()
        balance = community.getBalance()
    }

    companion object {
        const val SEEDING_LOOP_TIME : Long = 1800000 // Half hour

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

        fun createKey(hash: Sha1Hash, index: Int): String {
            return "$hash?index=$index"
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
        Log.i("DeToks", "Providing content ... $index")
        val content = torrentFiles.gett(index)

        return try {
            withTimeout(timeout) {
                Log.i("DeToks", "Waiting for content ... $index")
                while (!content.isDownloaded()) {
                    delay(100)
                }
                profile.updateEntryDuration(
                    createKey(content.handle.infoHash(), content.fileIndex),
                    content.getVideoDuration())
            }
            content.asMediaInfo()
        } catch (e: TimeoutCancellationException) {
            content.asMediaInfo()
        }
    }

    private fun getNumberOfTorrents(): Int {
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

        if (loopedToFront && newIndex == 0) unwatchedIndex = torrentFiles.size
        else if (unwatchedIndex < newIndex) unwatchedIndex = newIndex

        if (cachingAmount * 2 + 1 >= getNumberOfTorrents()) {
            notifyChangeUpdate()
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
        notifyChangeUpdate()
        currentIndex = newIndex
    }

    private fun notifyChangeUpdate() {
        val torrent = torrentFiles.gett(currentIndex)
        val key = createKey(torrent.handle.infoHash(), torrent.fileIndex)
        profile.updateEntryDuration(key, torrent.getVideoDuration())
        profile.updateEntryWatchTime(key, updateTime(), true)
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
                            getInfoFromMagnet(torrent.handle.makeMagnetUri())?.let { it2 ->
                                profile.updateEntryUploadDate(
                                    createKey(torrent.handle.infoHash(), it),
                                    it2
                                )
                            }
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
                    AlertType.LISTEN_SUCCEEDED -> {
                        val listenSucceededAlert = alert as ListenSucceededAlert
                        val ipAddress = listenSucceededAlert.address().toString().substring(1)
                        val port = listenSucceededAlert.port()
                        Log.d(DeToksCommunity.LOGGING_TAG,"IP: $ipAddress, Port: $port")
                        val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
                        community.saveLibTorrentPort(port.toString())
                    }
                    AlertType.PIECE_FINISHED -> {
                        val a = alert as PieceFinishedAlert
                        val torrentHandle = a.handle()
                        val allPeers = torrentHandle.peerInfo().filter { peer ->
                            peer.upSpeed() > 0 }

                        profitMap[torrentHandle.name()] = 0.0f - (a.handle().status().allTimeDownload()/1000000)
                        for (peer in allPeers) {
                            val ip = peer.ip().split(":")[0]
                            val foundPeers = community.findPeerByIps(ip)
                            for (seederPeer in foundPeers) {
                                if (seederPeer != null) {
                                    Log.d(DeToksCommunity.LOGGING_TAG, "Found seeder from Detoks Community: ${seederPeer.mid}")
                                    community.sendTokens(1.0f, seederPeer.mid)


                                }
                            }
                        }
                    }
                    AlertType.BLOCK_UPLOADED -> {
                        val a = alert as BlockUploadedAlert
                        val torrentHandle = a.handle()

                        val newWallet = community.getBalance()
                        profitMap[torrentHandle.name()] = newWallet - balance + profitMap[torrentHandle.name()]!!
                        balance = newWallet
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
        val torrentInfo = getInfoFromMagnet(magnet)?:return
        if (sessionManager.find(hash) != null) {
            for (it in 0 until torrentInfo.numFiles()) {
                if (!torrentInfo.files().fileName(it).endsWith(".mp4")) continue
                profile.incrementTimesSeen(createKey(torrentInfo.infoHash(), it))
            }
            return
        }

        Log.d(DeToksCommunity.LOGGING_TAG,"Adding new torrent: ${torrentInfo.name()}")

        sessionManager.download(torrentInfo, cacheDir)
        val handle = sessionManager.find(hash)
        handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
        handle.prioritizeFiles(arrayOf(Priority.IGNORE))
        handle.pause()

        var insertIndex = -1
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

                if (insertIndex == -1) {
                    insertIndex = if (unwatchedIndex == torrentFiles.size) torrentFiles.size
                    else {
                        strategies.findLeechingIndex(
                            torrentFiles,
                            profile.profiles,
                            torrent,
                            unwatchedIndex
                        )
                    }
                }

                torrentFiles.add(insertIndex, torrent)
                getInfoFromMagnet(magnet)?.let { it2 ->
                    profile.updateEntryUploadDate(
                        createKey(hash, it),
                        it2
                    )
                }
            }
        }
    }

    fun updateLeechingStrategy(strategyId: Int) {
        if (strategies.leechingStrategy == strategyId) return
        strategies.leechingStrategy = strategyId

        val sortedTorrents: MutableList<TorrentHandler>

        if (unwatchedIndex == torrentFiles.size) {
            currentIndex = 0
            sortedTorrents = strategies.applyStrategy(
                strategyId,
                torrentFiles,
                profile.profiles
            )
        } else {
            sortedTorrents = strategies.applyStrategy(
                strategyId,
                torrentFiles.subList(unwatchedIndex, torrentFiles.size),
                profile.profiles
            )
        }

        // Preserve cached if again in cache
        val cacheEnd = currentIndex + cachingAmount
        val newCache = sortedTorrents.subList(0,
            cachingAmount.coerceAtMost(sortedTorrents.size - 1)
        )

        for (i in currentIndex .. cacheEnd) {
            if (!newCache.contains(torrentFiles.gett(i)))
                torrentFiles.gett(i).deleteFile()
            torrentFiles[i.mod(torrentFiles.size)] = sortedTorrents.gett(i - currentIndex)
        }

        for (i in cacheEnd + 1 until torrentFiles.size) {
            torrentFiles.gett(i).deleteFile()
            torrentFiles[i.mod(torrentFiles.size)] = sortedTorrents.gett(i - currentIndex)
        }

        initializeVideoPool()
    }

    fun updateSeedingStrategy(
        strategyId: Int = strategies.seedingStrategy,
        storageLimit: Int = strategies.storageLimit,
        isSeeding: Boolean = strategies.isSeeding
    ) {
        if (!isSeeding) return

        seedingStrategyJobs.forEach{ it.cancel() }

        strategies.seedingStrategy = strategyId
        strategies.storageLimit = storageLimit

        val seedingTorrentsSorted = strategies.applyStrategy(
            strategyId,
            torrentFiles,
            profile.profiles
        ).distinctBy { it.handle }
        var storage: Long = 0

        val toStopSeeding = getAndClearSeedingTorrents()
        val jobs = mutableListOf<Job>()

        for (i in seedingTorrentsSorted.indices) {
            seedingTorrentsSorted[i].handle.scrapeTracker()
            val status = seedingTorrentsSorted[i].handle.status()
            val seeders = status.numSeeds()
            val leechers = status.numPeers() - seeders
            if (leechers < seeders) continue

            val size = status.total() / 1000000 // Bytes to MB conversion

            if (storage + size > strategies.storageLimit) continue
            storage += size

            if (toStopSeeding.contains(seedingTorrentsSorted[i])) {
                if (status.lastUpload() - System.currentTimeMillis() > SEEDING_LOOP_TIME) continue
                toStopSeeding.remove(seedingTorrentsSorted[i])
                addSeedingTorrent(seedingTorrentsSorted[i])
                continue
            }

            jobs.add(CoroutineScope(Job() + Dispatchers.Default).launch {
                if (downloadAndSeed(seedingTorrentsSorted[i])) {
                    yield()
                    addSeedingTorrent(seedingTorrentsSorted[i])
                }
            })
        }
        seedingStrategyJobs = jobs

        CoroutineScope(Job() + Dispatchers.Default).launch {
            toStopSeeding.forEach { stopSeedingTorrent(it) }
            jobs.forEach { it.join() }

            delay(SEEDING_LOOP_TIME)

            if (strategies.seedingStrategy == strategyId
                && strategies.storageLimit == storageLimit)
                updateSeedingStrategy(strategyId, storageLimit)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startMonitoringUploaders(handler: TorrentHandler) {
        val previousUploadMap = mutableMapOf<String, Long>()

        GlobalScope.launch(Dispatchers.IO) {
            while (handler.handle.isValid) {
                val peersInfo = handler.handle.peerInfo()
                val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!

                for (peerInfo in peersInfo) {
                    val ip = peerInfo.ip().split(":")[0]
                    val currentTotalUpload = peerInfo.totalUpload()
                    val previousTotalUpload = previousUploadMap[ip] ?: 0

                    if (currentTotalUpload > previousTotalUpload) {
                        val uploadedBytes = currentTotalUpload - previousTotalUpload
                        val tokens = uploadedBytes / 1048576
                        previousUploadMap[ip] = currentTotalUpload
                        Log.d(DeToksCommunity.LOGGING_TAG, "Increased token based on money received")
                        community.increaseTokens(tokens.toFloat())
                        profitMap[handler.handle.name()] = profitMap[handler.handle.name()]!! + tokens.toFloat()

                    }
                }

                delay(2000)
            }
        }
    }



    @Synchronized private fun addSeedingTorrent(seedTorrent: TorrentHandler) {
        seedingTorrents.add(seedTorrent)
    }

    @Synchronized private fun getAndClearSeedingTorrents(): MutableList<TorrentHandler> {
        val toStop = seedingTorrents.toMutableList()
        seedingTorrents.clear()
        return toStop
    }

    private suspend fun downloadAndSeed(handler: TorrentHandler, timeout: Long = 400000) : Boolean {
        if (!handler.handle.isValid) return false
        handler.downloadFile()

        try {
            withTimeout(timeout) {
                while (!handler.isDownloaded()) {
                    Log.d(DeToksCommunity.LOGGING_TAG, "Trying to download... ${handler.handle.status().totalWantedDone()} / ${handler.handle.status().totalWanted()}")

                    delay(1000)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.d(DeToksCommunity.LOGGING_TAG, "Timeout for download ... ${handler.torrentName}")
            handler.deleteFile()
            return false
        }

        handler.handle.setFlags(handler.handle.flags().and_(TorrentFlags.SHARE_MODE))
        handler.handle.pause()
        handler.handle.resume()
        startMonitoringUploaders(handler)

        return true
    }

    fun stopSeeding() {
        Log.d(DeToksCommunity.LOGGING_TAG, "Stopping all seeding")
        seedingTorrents.forEach {
            stopSeedingTorrent(it)
        }
    }

    private fun stopSeedingTorrent(handler: TorrentHandler) {
        seedingTorrents.remove(handler)
        handler.handle.unsetFlags(TorrentFlags.SHARE_MODE)
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

    fun getInfoFromMagnet(magnet: String): TorrentInfo? {
        val bytes = sessionManager.fetchMagnet(magnet, 10)?:return null
        return TorrentInfo.bdecode(bytes)
    }

    fun getListOfTorrents(): List<TorrentHandle> {
        return torrentFiles.map {it.handle}.distinct()
    }

    fun getListOfSeedingTorrents(): List<TorrentHandle> {
        return seedingTorrents.map {it.handle}.distinct()
    }

    fun getCurrentIndex(): Int {
        return currentIndex
    }

    fun getTorrentHandler(position: Int): TorrentHandler {
        return torrentFiles.gett(position)
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
        fun peerInfo(): MutableList<PeerInfo>? {
            return handle.peerInfo()
        }
        fun getVideoDuration() : Long {
            if(!isDownloaded()) return 0
            val retriever = MediaMetadataRetriever()
            if(!File(getPath()).exists())
                return 0
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
            handle.status()
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

        private fun setMaximumPriority() {
            handle.resume()
            handle.filePriority(fileIndex, Priority.SEVEN)
            handle.pause()
            handle.resume()
        }

        fun asMediaInfo(): TorrentMediaInfo {
            return TorrentMediaInfo(torrentName, fileName, getPath())
        }

        fun getFileSize(): Long {
            return handle.torrentFile().files().fileSize(fileIndex)
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
