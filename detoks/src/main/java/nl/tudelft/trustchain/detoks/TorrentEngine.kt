package nl.tudelft.trustchain.detoks

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toFile
import com.frostwire.jlibtorrent.Pair
import com.frostwire.jlibtorrent.TorrentInfo
import com.turn.ttorrent.client.SharedTorrent
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

class TorrentEngine {
//    @RequiresApi(Build.VERSION_CODES.O)
//    val torrentCacheFolder = Paths.get("cache/torrents")




//    /**
//     * Simulates a download and puts content in cache/torrents/releaseId
//     * @return root folder of release in cache
//     */
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun simulateDownload(
//        context: Context,
//        uris: List<Uri>
//    ): Pair<Path, TorrentInfo>? {
//        copyReleaseToTempFolder(context, uris)
//
//        val torrentInfo =
//            createTorrentInfo(Paths.get("cache/temp/"))
//        val infoHash = torrentInfo.infoHash().toString()
//        val torrentPath = Paths.get("cache/torrents/$infoHash.torrent")
//        val torrentFile = torrentPath.toFile()
//
//        if (!torrentCacheFolder.toFile().exists()) {
//            torrentCacheFolder.createDirectories()
//        }
//
//        Log.d(
//            "MusicDao",
//            "simulateDownload(): attempting to download to $torrentPath"
//        )
//        torrentFile.writeBytes(torrentInfo.bencode())
//
//        val folder = Paths.get("cache/temp")
//        Log.d(
//            "MusicDao",
//            "copyIntoCache: attempting to copy files $folder into torrent cache $torrentCacheFolder"
//        )
//        try {
//            folder.toFile().copyRecursively(
//                Paths.get("$torrentCacheFolder/${torrentInfo.infoHash()}").toFile(),
//                overwrite = true
//            )
//        } catch (exception: Exception) {
//            Log.d("MusicDao", "copyIntoCache: could not copy files")
//            return null
//        }
////        copyIntoCache(Paths.get("${cachePath.getPath()}/temp"))
//        return Pair(Paths.get("$torrentCacheFolder/${torrentInfo.infoHash()}"), torrentInfo)
//    }
}
