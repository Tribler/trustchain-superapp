package com.example.musicdao.core.usecases.torrents

import com.example.musicdao.core.torrent.api.DownloadingTrack
import TorrentCache
import com.example.musicdao.core.torrent.api.TorrentHandleStatus
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.util.MyResult
import com.example.musicdao.core.util.Util
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

fun mapTorrentHandle(torrentHandle: TorrentHandle, directory: File): TorrentHandleStatus {
    val files = torrentHandle.torrentFile()?.files()
    val formatted = if (files != null) {
        (0..(files.numFiles() - 1)).map {
            files.filePath(it)
        }.joinToString("\n")
    } else {
        ""
    }

    return TorrentHandleStatus(
        id = torrentHandle.status().name().toString(),
        infoHash = torrentHandle.infoHash().toString(),
        magnet = torrentHandle.makeMagnetUri(),
        finishedDownloading = torrentHandle.status().isFinished.toString(),
        pieces = "${torrentHandle.status().pieces().count()}/${torrentHandle.status().numPieces()}",
        files = formatted,
        seeding = torrentHandle.status().isSeeding.toString(),
        peers = torrentHandle.status().numPeers().toString(),
        seeders = torrentHandle.status().numPeers().toString(),
        uploadedBytes = torrentHandle.status().allTimeUpload().toString(),
        downloadedBytes = torrentHandle.status().allTimeDownload().toString(),
        downloadingTracks = downloadingTracks(torrentHandle, directory),
    )
}

fun downloadingTracks(handle: TorrentHandle, directory: File): List<DownloadingTrack>? {
    val files = handle.torrentFile()?.files() ?: return null
    val fileProgress = handle.fileProgress()

    val downloadingTracks = (0..(files.numFiles() - 1)).mapNotNull {
        val file = File("${directory}/torrents/${handle.infoHash()}/${files.filePath(it)}")
        if (file.exists()) {
            DownloadingTrack(
                title = (Util.checkAndSanitizeTrackNames(files.fileName(it))
                    ?: files.fileName(it)),
                artist = "Artist",
                progress = Util.calculateDownloadProgress(
                    fileProgress.get(it),
                    files.fileSize(it)
                ),
                file = file,
                fileIndex = it
            )
        } else {
            null
        }
    }
    return downloadingTracks
}

class GetTorrentStatusFlowUseCase(private val torrentCache: TorrentCache) {

    private val REFRESH_DELAY = 1000L

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(id: String): Flow<TorrentHandleStatus>? {
        return when (val result = torrentCache.get(id)) {
            is MyResult.Failure -> null
            is MyResult.Success -> flow {
                while (true) {
                    val handle = result.value
                    emit(mapTorrentHandle(handle, torrentCache.path.toFile()))
                    delay(REFRESH_DELAY)
                }
            }
        }
    }
}
