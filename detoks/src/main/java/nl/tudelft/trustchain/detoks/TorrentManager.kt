package nl.tudelft.trustchain.detoks

import android.annotation.SuppressLint
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.content.Context
import android.os.Build
import android.os.FileUtils
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import com.frostwire.jlibtorrent.swig.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.detoks.fragments.DeToksFragment
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


/**
 * This class manages the torrent files and the video pool.
 * It is responsible for downloading the torrent files and caching the videos.
 * It also provides the videos to the video adapter.
 */
class TorrentManager constructor (
    private val cacheDir: File,
    private val torrentDir: File,
    private val cachingAmount: Int = 1,
) {
    private val sessionManager = SessionManager()
    private val logger = KotlinLogging.logger {}

    val torrentFiles = mutableListOf<TorrentHandler>()

    // maps torrent to videos acquired from them. Can help with preventing duplicate torrents.
    private val torrentsList = HashMap<String, String>()
    private var currentIndex = 0

    /**
     * If we want to seed torrents we cannot clear the cache
     */
    init {
//        clearMediaCache()
        initializeSessionManager()
        buildTorrentIndex()
        initializeVideoPool()
    }

    /**
     * ONLY CREATE NEW TORRENT MANAGERS WITH THIS GET INSTANCE!!
     */
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
//        Log.i("DeToks", "Increasing index ... ${(currentIndex + 1) % getNumberOfTorrents()}")
        notifyChange((currentIndex + 1) % getNumberOfTorrents(), loopedToFront = true)

    }

    fun notifyDecrease() {
//        Log.i("DeToks", "Decreasing index ... ${(currentIndex - 1) % getNumberOfTorrents()}")
        notifyChange((currentIndex - 1) % getNumberOfTorrents())
    }

    /**
     * This function provides the video at the given index.
     * If the video is not downloaded yet, it will wait for it to be downloaded.
     * If the video is not downloaded after the timeout, it will return the video anyway.
     */
    suspend fun provideContent(index: Int = currentIndex, timeout: Long = 10000): TorrentMediaInfo {
        val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
        val comparator = compareByDescending<TrustChainBlock>
        {
            community.getLikes(
                it.transaction["video"] as String,
                it.transaction["torrent"] as String
            ).size
        }.thenByDescending {
            community.getEarliestDate(
                it.transaction["video"] as String,
                it.transaction["torrent"] as String
            )
        }
        val a = community.database.getBlocksWithType(LIKE_BLOCK).sortedWith(comparator)
        var i = 0
        // we only recommend videos that are not watched yet
        while(i < a.size) {
            val b = a[i].transaction
            i += 1
            val c = torrentFiles.filter { !it.watched }
            for (t in c) {
                if (t.fileName == b["video"] as String && t.torrentName == b["torrent"] as String) {
                    return try {
                        withTimeout(timeout) {
                            Log.i("DeToks", "Waiting for content ... $index")
                            while (!t.isDownloaded()) {
                                delay(100)
                            }
                        }
                        t.asMediaInfo()
                    } catch (e: TimeoutCancellationException) {
                        Log.i("DeToks", "Timeout for content ... $index")
                        t.asMediaInfo()
                    }
                }
            }
        }
        // default implementation when all videos are watched
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
            torrentFiles.sort()
            torrentFiles.gett(newIndex + cachingAmount).downloadFile()
        } else {
            torrentFiles.gett(currentIndex + cachingAmount).deleteFile()
            torrentFiles.sort()
            torrentFiles.gett(newIndex - cachingAmount).downloadFile()

        }
        currentIndex = newIndex
    }

    private fun initializeVideoPool() {
        if (torrentFiles.size < cachingAmount * 2) {
            Log.d("DeToks", "Smaller than cache")
            logger.error("Not enough torrents to initialize video pool")
            return
        }
        for (i in (currentIndex - cachingAmount)..(currentIndex + cachingAmount)) {
            torrentFiles.sort()
            val torrent = torrentFiles.gett(i)
            if (i == currentIndex) {
                torrent.downloadWithMaxPriority()
            } else {
                torrent.downloadFile()
            }
        }
    }

    /**
     * Adds a torrent to the torrent handler list.
     * If the torrent is not being seeded (fetching magnet returns null) we do not add it.
     * @param magnet - magnet link in string format
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun addMagnet(magnet: String){
        Log.wtf("Detoks", "in add magnet for $magnet")
        val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
        val res = sessionManager.fetchMagnet(magnet, 1000) ?: return

        val torrentInfo = TorrentInfo(res)
        if(torrentsList.get(torrentInfo.infoHash().toString()) != null) return
        val par = torrentDir.absolutePath
        val torrentPath = Paths.get("$par/${torrentInfo.infoHash()}.torrent")
        val torrentFile = torrentPath.toFile()

        torrentFile.writeBytes(res)
        sessionManager.download(torrentInfo, cacheDir)
        val handle = sessionManager.find(torrentInfo.infoHash())

        handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
        val priorities = Array(torrentInfo.numFiles()) { Priority.IGNORE }
        handle.prioritizeFiles(priorities)
        handle.pause()

        val author = community.getAuthorOfMagnet(magnet)
        for (it in 0 until torrentInfo.numFiles()) {
            val fileName = torrentInfo.files().fileName(it)
            if (fileName.endsWith(".mp4")) {
                torrentsList.put(torrentInfo.infoHash().toString(), fileName)
                Log.wtf("Detoks", "addMagnet: author = $author, filename=$fileName")
                torrentFiles.add(
                    TorrentHandler(
                        cacheDir,
                        handle,
                        torrentInfo.name(),
                        fileName,
                        it,
                        author,
                        torrentInfo.makeMagnetUri(),
                        false
                    )
                )
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
                    sessionManager.download(torrentInfo, cacheDir )
                    val res = sessionManager.fetchMagnet(torrentInfo.makeMagnetUri(), 10)
                    if (res == null) {
                        Log.d("DeToks", "NO DATA :(")
                        continue
                    }
                    val handle = sessionManager.find(torrentInfo.infoHash())

                    handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
                    val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
                    val myPublicKey = community.myPeer.publicKey.toString()

                    if (torrentInfo.creator() == myPublicKey){
//                        Log.wtf("Detoks", "Creator is me: ${torrentInfo.creator()}")
                        val priorities = Array(torrentInfo.numFiles()) { Priority.SEVEN }
                        handle.prioritizeFiles(priorities)
                        handle.pause()
                        handle.resume()
                    }else{
//                        Log.wtf("Detoks", "Creator: ${torrentInfo.creator()} is not me: $myPublicKey")
                        val priorities = Array(torrentInfo.numFiles()) { Priority.NORMAL }
                        handle.prioritizeFiles(priorities)
                        handle.pause()
                    }
                    for (it in 0 until torrentInfo.numFiles()) {
                        val fileName = torrentInfo.files().fileName(it)
//                        Log.d("DeToks", "file ${fileName} in $it")
                        val author = community.getAuthorOfMagnet(torrentInfo.makeMagnetUri())
                        Log.wtf("Detoks", "Author of $fileName is $author")
                        if (fileName.endsWith(".mp4")) {
                            torrentsList.put(torrentInfo.infoHash().toString(), fileName)
                            torrentFiles.add(
                                TorrentHandler(
                                    cacheDir,
                                    handle,
                                    torrentInfo.name(),
                                    fileName,
                                    it,
                                    author,
                                    torrentInfo.makeMagnetUri(),
                                    false
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a new torrent and start seeding it (also adds it to the torrent handler list)
     * @param collection - the uri of the file for which a torrent is to be generated (in the format of media:numbers usually)
     * @param context - the current context of execution (needed for path creation)
     * @return A pair with a path to the torrent file and a Torrent info object
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createTorrentInfo(collection: Uri, context: Context): Pair<Path, TorrentInfo>? {
        val parentDir = Paths.get(cacheDir.getPath()+"/"+collection.hashCode().toString())
        val out = copyToTempFolder(context, listOf(collection), parentDir)

        Log.d("DeToks", collection.toString())
        Log.d("DeToks", "VALID: ${out.second.is_valid}" )

        val tb = TorrentBuilder()
        tb.creator(IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!.myPeer.publicKey.toString())
        tb.path(File(cacheDir.getPath()+"/"+collection.hashCode().toString()))

        tb.addTracker("http://tracker.openbittorrent.com:80/announce", 0)
        tb.addTracker("http://open.acgnxtracker.com:80/announce", 1)

        tb.setPrivate(false)

        val torrentInfo = TorrentInfo(tb.generate().entry().bencode())
        val infoHash = torrentInfo.infoHash().toString()
        val par = torrentDir.absolutePath
        val torrentPath = Paths.get("$par/$infoHash.torrent")
        val torrentFile = torrentPath.toFile()

        torrentFile.writeBytes(tb.generate().entry().bencode())

        Log.d("DeToks", "Making magnet")
        Log.d("DeToks", torrentInfo.makeMagnetUri())
        sessionManager.download(torrentInfo, cacheDir )
        val res = sessionManager.fetchMagnet(torrentInfo.makeMagnetUri(), 20)
        if (res == null) Log.d("DeToks", "NO DATA :(")
        val handle = sessionManager.find(torrentInfo.infoHash())

        handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
        val priorities = Array(torrentInfo.numFiles()) { Priority.SEVEN }
        handle.prioritizeFiles(priorities)
        handle.pause()
        handle.resume()
        val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
        val magnUri = torrentInfo.makeMagnetUri()
        Log.wtf("Detoks", "createTorrentInfo, creator: ${torrentInfo.creator()}")
        Log.d("DeToks", "THIS HAS ${torrentInfo.numFiles()} : ${torrentInfo.creator()}  from ${torrentInfo.name()}")
        for (it in 0..torrentInfo.numFiles()-1) {
            val fileName = torrentInfo.files().fileName(it)
            Log.d("DeToks", "file ${fileName} in $it")
            if (fileName.endsWith(".mp4")) {
                torrentsList.put(torrentInfo.infoHash().toString(), fileName)
                Log.d("Detoks", "Liking video uploaded by self: ${torrentInfo.creator()}")
                community.broadcastLike(fileName, torrentInfo.name(), torrentInfo.creator(), magnUri)
                torrentFiles.add(
                    TorrentHandler(
                        cacheDir,
                        handle,
                        torrentInfo.name(),
                        fileName,
                        it,
                        torrentInfo.creator(),
                        magnUri,
                        false
                    )
                )
            }
        }
        return Pair(torrentPath, torrentInfo)
    }

    /**
     * Copies a list of files to a temp folder
     * @param context - current context
     * @param uris - a list of media uris to be copied
     * @param parentDir - Path to the parent directory where files will be copied
     * @return A pair of file object (representing the parent folder) and a file_storage (used for torrent creation) of the parent folder
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun copyToTempFolder(context: Context, uris: List<Uri>, parentDir: Path): Pair<File, file_storage> {
        Log.d(
            "DeToks",
            "copyToTempFolder: attempting to copy files ${uris.map { it.toString() }} into temp folder $parentDir"
        )
        val contentResolver = context.contentResolver

        File(parentDir.toUri()).deleteRecursively()
        var fs = file_storage()

        val fileList = mutableListOf<File>()
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
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

            Files.createDirectories(parentDir)
            if(Files.notExists(File(fileLocation).toPath())) {
                Files.createFile(File(fileLocation).toPath()).toFile()
            }

            val output = contentResolver.openOutputStream(Uri.fromFile(File(fileLocation)))!!

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                FileUtils.copy(input, output)
            }
            output.close()
            fileList.add(File(fileLocation))
            libtorrent.add_files(fs, "$parentDir/$fileName")
        }

        return Pair(parentDir.toFile(), fs)
    }

    /**
     * Given an android media uri it will convert it to a concrete path
     * @param uri - the media uri of the video
     * @param context - current execution context
     * @return A pair of string (the file path) a
     */
    @SuppressLint("Range")
    fun getVideoFilePath(uri: Uri, context: Context):  Pair<String?, Long> {
        val cursor: Cursor = context.getContentResolver().query(uri, null, null, null, null)!!
        cursor.moveToFirst()
        var f_id = cursor.getString(0)

        f_id = f_id.split(":")[1]
        cursor.close()

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf( MediaStore.Video.Media.DATA, OpenableColumns.SIZE),
            "_id=?",
            arrayOf(f_id),
            null
        )?.use { c2 ->
            Log.d("AndroidRuntime", f_id)
            Log.d("AndroidRuntime", uri.toString())
            Log.d("AndroidRuntime",c2.count.toString())
            Log.d("AndroidRuntime", "====")
            c2.moveToFirst()
            var path = c2.getString(0)
            val size = c2.getString(1).toLong()

            c2.close()
            return Pair(path, size )
        }

        return Pair("",0)
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

    fun addTorrent(magnet: String) {
        val torrentInfo = getInfoFromMagnet(magnet)?:return
        val hash = torrentInfo.infoHash()

        if(sessionManager.find(hash) != null) return
//        Log.d("DeToksCommunity","Is a new torrent: ${torrentInfo.name()}")

        sessionManager.download(torrentInfo, cacheDir)
        val handle = sessionManager.find(hash)
        handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
        handle.prioritizeFiles(arrayOf(Priority.IGNORE))
        handle.pause()
        val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
        val author = community.getAuthorOfMagnet(magnet)
        Log.wtf("Detoks", "addTorrent: author = $author, creator = ${torrentInfo.creator()}")

        for (it in 0 until torrentInfo.numFiles()) {
            val fileName = torrentInfo.files().fileName(it)
            if (fileName.endsWith(".mp4")) {
                torrentFiles.add(
                    TorrentHandler(
                        cacheDir,
                        handle,
                        torrentInfo.name(),
                        fileName,
                        it,
                        author,
                        magnet,
                        false
                    )
                )
            }
        }
    }

    private fun getInfoFromMagnet(magnet: String): TorrentInfo? {
        val bytes = sessionManager.fetchMagnet(magnet, 10)?:return null
        return TorrentInfo.bdecode(bytes)
    }

    fun getListOfTorrents(): List<TorrentHandle> {
        return torrentFiles.map {it.handle}.distinct()
    }

    class TorrentHandler(
        private val cacheDir: File,
        val handle: TorrentHandle,
        val torrentName: String,
        val fileName: String,
        val fileIndex: Int,
        val creator: String,
        val torrentMagnet: String,
        var watched: Boolean
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

        fun downloadWithMaxPriority() {
            downloadFile()
            setMaximumPriority()
        }

        fun deleteFile() {
            return
//            handle.filePriority(fileIndex, Priority.IGNORE)
//            val file = File("$cacheDir/$torrentName/$fileName")
//            if (file.exists()) {
//                file.delete()
//            }
//            isDownloading = false
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
            return TorrentMediaInfo(torrentName, fileName, getPath(), creator, torrentMagnet)
        }

    }
    private fun List<TorrentHandler>.sort(){
        val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
        val comparator = compareByDescending <TorrentHandler>
        { community.getLikes(it.fileName,it.torrentName).size }.
        thenByDescending{ community.getEarliestDate(it.fileName,it.torrentName)}

        this.sortedWith(comparator)
    }
    // Extension functions to loop around the index of a lists.
    private fun <E> List<E>.gett(index: Int): E  {
        if(size!=0) return this[index.mod(size)]
        return this[0]
    }
    private fun <E> List<E>.gettIndex(index: Int): Int = index.mod(size)
}

class TorrentMediaInfo(
    val torrentName: String,
    val fileName: String,
    val fileURI: String,
    val creator: String,
    val torrentMagnet: String
)
