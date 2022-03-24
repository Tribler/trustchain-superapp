package com.example.musicdao.core.torrent

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.musicdao.CachePath
import com.example.musicdao.core.cache.CacheDatabase
import com.example.musicdao.core.usecases.DownloadFinishUseCase
import com.example.musicdao.core.util.Util
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.*
import com.turn.ttorrent.client.SharedTorrent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.createDirectories

@DelicateCoroutinesApi
@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class TorrentEngine @Inject constructor(
    private val sessionManager: SessionManager,
    val cachePath: CachePath,
    private val database: CacheDatabase,
    private val downloadFinishUseCase: DownloadFinishUseCase
) {
    private val _activeTorrents: MutableStateFlow<List<String>> = MutableStateFlow(mutableListOf())
    private val activeTorrents: StateFlow<List<String>> = _activeTorrents

    private val torrentCacheFolder = Paths.get("${cachePath.getPath()}/torrents")

    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val jobs: MutableMap<String, Job> = mutableMapOf()

    init {
        setupListeners()
    }

    /**
     * The main seed function for any torrent. Magnet is only used for external
     * links.
     *
     * @param magnet magnet link
     * @param root root folder, not the root folder of torrent
     * @param initialSeed if torrent has never been seeded before
     * @return torrentHandle
     */
    fun seed(
        magnet: String,
        root: Path,
        initialSeed: Boolean = false
    ): TorrentHandle? {
        val contentFolder = rootToContentFolder(root) ?: return null
        Log.d(
            "MusicDao",
            "seed(): torrent seed with $magnet, $root, $contentFolder, $initialSeed"
        )
        if (!FileProcessor.folderExistsAndHasFiles(contentFolder.toFile())) {
            Log.d("MusicDao", "seed(): Folder $contentFolder is empty or does not exist")
            return null
        }

        // If initial seed, then "create" a torrent file and seed. Else, use the magnet link w/
        // any trackers included.
        if (initialSeed) {
            val torrentInfo = createTorrentInfo(contentFolder)
            sessionManager.download(torrentInfo, root.toFile())
        } else {
            sessionManager.download(magnet, root.toFile())
        }
        val handle = sessionManager.find(Sha1Hash(magnetToInfoHash(magnet)))

        handle.pause()
        handle.resume()

        return handle
    }

    /**
     * Downloads a torrent with the magnet link and trackers in it correctly.
     */
    fun download(
        magnet: String,
        root: Path,
    ): TorrentHandle? {

        val infoHash = magnetToInfoHash(magnet)!!

        Log.d(
            "MusicDao",
            "download(): torrent download with: $infoHash, $root"
        )

        Log.d(
            "MusicDao",
            "download(2): ${_activeTorrents.value.map { it }} $this"
        )

        if (_activeTorrents.value.contains(infoHash)) {
            Log.d(
                "MusicDao",
                "download(): torrent already started as a job"
            )
            downloadFinishUseCase.invoke(infoHash)
            return null
        }

        val job = GlobalScope.launch(dispatcher) {
            Log.d(
                "MusicDao",
                "download(): attempt to get torrentFile"
            )

            val torrentPath = Paths.get("${cachePath.getPath()}/torrents/$infoHash.torrent")
            val torrentFile = torrentPath.toFile()
            var torrentInfo: TorrentInfo? = null

            if (torrentFile.exists()) {
                Log.d(
                    "MusicDao",
                    "download(): torrentFile fetched locally"
                )
                torrentInfo = TorrentInfo(torrentFile)
            } else {
                while (isActive && torrentInfo == null) {
                    Log.d(
                        "MusicDao",
                        "download(): attempt to get torrentFile remotely for $infoHash - $magnet"
                    )
                    val bytes = sessionManager.fetchMagnet(magnet, 10)
                    if (bytes != null) {
                        Log.d(
                            "MusicDao",
                            "download(): found some bytes ${bytes.size}"
                        )
                        torrentInfo = TorrentInfo.bdecode(bytes)
                    }
                }

                if (!torrentCacheFolder.toFile().exists()) {
                    torrentCacheFolder.createDirectories()
                }
                Log.d(
                    "MusicDao",
                    "download(): torrent file found, writing to disk"
                )
                torrentFile.writeBytes(torrentInfo!!.bencode())
            }

            sessionManager.download(torrentInfo, root.toFile())
            val infoHash = magnetToInfoHash(magnet)
            val handle = sessionManager.find(Sha1Hash(infoHash))

            // Opt-out of the auto-managed queue system of lib-torrent
            handle.unsetFlags(TorrentFlags.AUTO_MANAGED)
            handle.resume()
            Log.d(
                "MusicDao",
                "download(): download started successfully for $infoHash!"
            )
        }

        jobs[infoHash] = job
        return null
    }

    fun getAllTorrents(): StateFlow<List<String>> {
        return activeTorrents
    }

    fun get(infoHash: String): TorrentHandle? {
        val handle = sessionManager.find(Sha1Hash(infoHash))
        return if (handle != null) {
            handle
        } else {
            null
        }
    }

    suspend fun seedStrategy(): List<TorrentHandle> {
        val downloadedReleases = database.dao.getAll().filter { it.isDownloaded }
        Log.d(
            "MusicDao",
            "SeedStrategy: attempting to seed (${downloadedReleases.size}): ${downloadedReleases.map { "[${it.magnet} - $it]" }}"
        )

        val result = downloadedReleases.mapNotNull {
            it.root?.let { root ->
                download(magnet = it.magnet, Paths.get(root))
            }
        }

        Log.d(
            "MusicDao",
            "SeedStrategy: result to seed (${result.size}): ${result.map { it.infoHash() }}"
        )

        return result
    }

    fun download(magnet: String): TorrentHandle? {
        val infoHash = magnetToInfoHash(magnet = magnet)!!
        return download(magnet, Paths.get("$torrentCacheFolder/$infoHash"))
    }

    /**
     * Simulates a download and puts content in cache/torrents/releaseId
     * @return root folder of release in cache
     */
    fun simulateDownload(
        context: Context,
        uris: List<Uri>,
    ): Pair<Path, TorrentInfo>? {
        copyReleaseToTempFolder(context, uris)

        val torrentInfo =
            createTorrentInfo(Paths.get("${cachePath.getPath()}/temp/$DEFAULT_DIR_NAME"))
        val infoHash = torrentInfo.infoHash().toString()
        val torrentPath = Paths.get("${cachePath.getPath()}/torrents/$infoHash.torrent")
        val torrentFile = torrentPath.toFile()

        if (!torrentCacheFolder.toFile().exists()) {
            torrentCacheFolder.createDirectories()
        }

        Log.d(
            "MusicDao",
            "simulateDownload(): attempting to download to $torrentPath"
        )
        torrentFile.writeBytes(torrentInfo.bencode())

        val folder = Paths.get("${cachePath.getPath()}/temp")
        Log.d(
            "MusicDao",
            "copyIntoCache: attempting to copy files $folder into torrent cache $torrentCacheFolder"
        )
        try {
            folder.toFile().copyRecursively(
                Paths.get("$torrentCacheFolder/${torrentInfo.infoHash()}").toFile(),
                overwrite = true
            )
        } catch (exception: Exception) {
            Log.d("MusicDao", "copyIntoCache: could not copy files")
            return null
        }
//        copyIntoCache(Paths.get("${cachePath.getPath()}/temp"))
        return Pair(Paths.get("$torrentCacheFolder/${torrentInfo.infoHash()}"), torrentInfo)
    }

    /**
     * Copies the file URIs given to the temp folder of the release.
     *
     * @param context android context
     * @param uris uris of files
     * @return root folder
     */
    private fun copyReleaseToTempFolder(
        context: Context,
        uris: List<Uri>,
    ): File {
        val parentDir = Paths.get("${cachePath.getPath()}/temp/$DEFAULT_DIR_NAME")
        return copyToTempFolder(context, uris, parentDir)
    }

    private fun copyToTempFolder(context: Context, uris: List<Uri>, parentDir: Path): File {
        Log.d(
            "MusicDao",
            "copyToTempFolder: attempting to copy files ${uris.map { it.toString() }} into temp folder $parentDir"
        )
        val contentResolver = context.contentResolver

        File("${context.cacheDir}/temp").deleteRecursively()

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

            FileUtils.copyInputStreamToFile(input, File(fileLocation))
            fileList.add(File(fileLocation))
        }

        return parentDir.toFile()
    }

    /**
     * Copy whole folder contents into cache
     *
     * @param folder all the contents of this folder will be copied into cache
     * @return if successful or not
     */
    private fun copyIntoCache(folder: Path): Boolean {
        Log.d(
            "MusicDao",
            "copyIntoCache: attempting to copy files $folder into torrent cache $torrentCacheFolder"
        )
        try {
            folder.toFile().copyRecursively(torrentCacheFolder.toFile(), overwrite = true)
        } catch (exception: Exception) {
            Log.d("MusicDao", "copyIntoCache: could not copy files")
            return false
        }
        return true
    }

    private fun setupListeners() {
        sessionManager.addListener(object : AlertListener {
            override fun types(): IntArray? {
                return null
            }

            override fun alert(alert: Alert<*>) {
                when (alert.type()) {
                    AlertType.ADD_TORRENT -> {
                        val a: AddTorrentAlert = alert as AddTorrentAlert
                        Log.d(
                            "MusicDao",
                            "Torrent: Torrent added ${a.handle().infoHash()} with \n${
                            a.handle().makeMagnetUri()
                            }"
                        )
                        alert.handle().resume()
                        _activeTorrents.value =
                            _activeTorrents.value + alert.handle().infoHash().toString()
                        Log.d(
                            "MusicDao",
                            "Torrent: Torrent added 2 ${_activeTorrents.value.map { it }} ${this@TorrentEngine}"
                        )
                    }
                    AlertType.TORRENT_REMOVED -> {
                        val a: TorrentRemovedAlert = alert as TorrentRemovedAlert
                        Log.d(
                            "MusicDao",
                            "Torrent: Torrent removed ${a.handle().infoHash()} with \n${
                            a.handle().makeMagnetUri()
                            }"
                        )
                        _activeTorrents.value =
                            _activeTorrents.value - alert.handle().infoHash().toString()
                        Log.d(
                            "MusicDao",
                            "Torrent: Torrent removed 2 ${_activeTorrents.value.map { it }}"
                        )
                    }
                    AlertType.TORRENT_CHECKED -> {
                        val a: TorrentCheckedAlert = alert as TorrentCheckedAlert
                        Log.d(
                            "MusicDao",
                            "Torrent: Torrent checked ${a.handle().infoHash()} with \n${
                            a.handle().makeMagnetUri()
                            }"
                        )
                        Util.setTorrentPriorities(alert.handle())
                    }
                    AlertType.BLOCK_FINISHED -> {
                        val a: BlockFinishedAlert = alert as BlockFinishedAlert
                        val p = (a.handle().status().progress() * 100).toInt()
                        Log.d(
                            "MusicDao",
                            "Torrent: Progress: " + p + " for torrent name: " + a.torrentName()
                        )
                    }
                    AlertType.TORRENT_FINISHED -> {
                        val a: TorrentFinishedAlert = alert as TorrentFinishedAlert
                        Log.d(
                            "MusicDao",
                            "Torrent: Torrent finished ${a.handle().infoHash()} with \n${
                            a.handle().makeMagnetUri()
                            }"
                        )
                        downloadFinishUseCase.invoke(a.handle().infoHash().toString())
                    }
                }
            }
        })
    }

    companion object {
        fun generateInfoHash(path: Path): String? {
            val folder = path.toFile()
            if (!FileProcessor.folderExistsAndHasFiles(folder)) {
                Log.d("MusicDao", "generateInfoHash: could not calculate info-hash for $folder")
                return null
            }

            val torrent = SharedTorrent.create(
                folder,
                folder.listFiles()?.toList()?.sorted() ?: listOf(),
                65535,
                listOf(),
                ""
            )

            return torrent.hexInfoHash
        }

        fun createTorrentInfo(folder: Path): TorrentInfo {
            val torrent = SharedTorrent.create(
                folder.toFile(),
                folder.toFile().listFiles()?.toList()?.sorted() ?: listOf(),
                65535,
                listOf(),
                ""
            )
            return TorrentInfo(torrent.encoded)
        }

        fun magnetToInfoHash(magnet: String): String? {
            val mark = "magnet:?xt=urn:btih:"
            val start = magnet.indexOf(mark) + mark.length
            if (start == -1) return null
            return magnet.substring(20, start + 40)
        }

        fun infoHashToMagnet(infoHash: String): String {
            Log.d("MusicDao", "infoHashToMagnet: $infoHash")

            return "magnet:?xt=urn:btih:$infoHash"
        }

        /**
         * Heuristically find the main folder of the torrent.
         * TODO: support torrents which do not have a main folder
         *
         * @param root
         * @return
         */
        fun rootToContentFolder(root: Path): Path? {
            // 1. Default folder, if created by our protocol.
            val contentFolder = Paths.get("$root/content").toFile()
            if (contentFolder.exists()) {
                return contentFolder.toPath()
            }

            // 2. First folder in root.
            val potentialFolders = root.toFile().listFiles()
            if (potentialFolders == null) return null

            val folder = potentialFolders.toList().find { it.isDirectory }
            if (folder != null) {
                return folder.toPath()
            }

            return null
        }

        const val DEFAULT_DIR_NAME = "content"
    }
}
