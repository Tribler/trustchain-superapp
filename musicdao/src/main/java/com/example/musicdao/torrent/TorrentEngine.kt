import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.musicdao.util.MyResult
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.nio.file.Path

/**
 * Cl
 */
@RequiresApi(Build.VERSION_CODES.O)
class TorrentEngine(private val sessionManager: SessionManager) {

    private val _activeTorrents: MutableStateFlow<List<String>> = MutableStateFlow(
        mutableListOf()
    )
    val activeTorrents: StateFlow<List<String>> = _activeTorrents

    fun getAllTorrents(): StateFlow<List<String>> {
        return activeTorrents
    }

    init {
        sessionManager.addListener(object : AlertListener {
            override fun types(): IntArray? {
                return null
            }

            override fun alert(alert: Alert<*>) {
                val type: AlertType = alert.type()
                when (type) {
                    AlertType.ADD_TORRENT -> {
                        val a: AddTorrentAlert = alert as AddTorrentAlert
                        Log.d(
                            "MusicDAOTorrent",
                            "ALERT: Torrent added ${a.handle().infoHash()} with \n${
                                a.handle().makeMagnetUri()
                            }"
                        )
                        alert.handle().resume()
                        _activeTorrents.value =
                            _activeTorrents.value + alert.handle().infoHash().toString()

                    }
                    AlertType.TORRENT_REMOVED -> {
                        val a: TorrentRemovedAlert = alert as TorrentRemovedAlert
                        Log.d(
                            "MusicDAOTorrent",
                            "ALERT: Torrent removed ${a.handle().infoHash()} with \n${
                                a.handle().makeMagnetUri()
                            }"
                        )
                        _activeTorrents.value =
                            _activeTorrents.value - alert.handle().infoHash().toString()
                    }
                    AlertType.BLOCK_FINISHED -> {
                        val a: BlockFinishedAlert = alert as BlockFinishedAlert
                        val p = (a.handle().status().progress() * 100).toInt()
                        Log.d(
                            "MusicDAOTorrent",
                            "ALERT: Progress: " + p + " for torrent name: " + a.torrentName()
                        )
                    }
                    AlertType.TORRENT_FINISHED -> {
                        val a: TorrentFinishedAlert = alert as TorrentFinishedAlert
                        Log.d(
                            "MusicDAOTorrent",
                            "ALERT: Torrent finished ${a.handle().infoHash()} with \n${
                                a.handle().makeMagnetUri()
                            }"
                        )
                    }
                }
            }
        })
    }

    fun getTorrentHandle(realInfoHash: String): MyResult<TorrentHandle> {
        val handle = sessionManager.find(Sha1Hash(realInfoHash))
        if (handle == null) {
            return MyResult.Failure("No handle.")
        } else {
            return MyResult.Success(handle)
        }
    }

    fun verifyAndSeed(folder: Path, realInfoHash: String): MyResult<TorrentHandle> {
        return when (val res = verify(folder, realInfoHash)) {
            is MyResult.Failure -> MyResult.Failure(res.message)
            is MyResult.Success -> seed(folder)
        }
    }

    fun seed(
        folder: Path
    ): MyResult<TorrentHandle> {
        val files = File(folder.toUri())
        if (!files.exists() || files.listFiles().isEmpty()) {
            return MyResult.Failure("Folder $folder is empty or does not exist.")
        }

        val builder = TorrentBuilder()
        builder.path(files)
        val bytes = builder.generate().entry().bencode()

        val torrentInfo = TorrentInfo(bytes)
        sessionManager.download(torrentInfo, files.parentFile)
        val handle = sessionManager.find(torrentInfo.infoHash())
        handle.pause()
        handle.resume()

        return MyResult.Success(handle)
    }

    fun download(
        folder: Path,
        realInfoHash: String
    ): MyResult<TorrentHandle> {
        sessionManager.download(
            "magnet:?xt=urn:btih:$realInfoHash",
            folder.toFile().parentFile
        )
        val handle = sessionManager.find(Sha1Hash(realInfoHash))

        // Opt-out of the auto-managed queue system of libtorrent
        handle.unsetFlags(TorrentFlags.AUTO_MANAGED)
        handle.resume()

        return if (handle == null) {
            MyResult.Failure("Did not get the torrent.")
        } else {
            MyResult.Success(handle)
        }
    }

    companion object {
        /**
         * @param folder the folder is included in the torrent
         * file and resulting info-hash
         */
        fun generateInfoHash(folder: Path): MyResult<String> {
            val files = File(folder.toUri())
            if (!files.exists() || files.listFiles().isEmpty()) {
                return MyResult.Failure("Folder $folder is empty or does not exist.")
            }

            val builder = TorrentBuilder()
            builder.path(files)
            val bytes = builder.generate().entry().bencode()
            val torrentInfo = TorrentInfo(bytes)

            return MyResult.Success(torrentInfo.infoHash().toString())
        }

        /**
         * @param folder the folder is included in the torrent
         * file and resulting info-hash
         */
        fun verify(folder: Path, realInfoHash: String): MyResult<Boolean> {
            return when (val infoHash = generateInfoHash(folder)) {
                is MyResult.Failure -> MyResult.Failure(infoHash.message)
                is MyResult.Success -> {
                    if (infoHash.value == realInfoHash) {
                        MyResult.Success(true)
                    } else {
                        MyResult.Failure("Info-hash not the same: ${infoHash.value} and $realInfoHash")
                    }
                }
            }
        }
    }
}
