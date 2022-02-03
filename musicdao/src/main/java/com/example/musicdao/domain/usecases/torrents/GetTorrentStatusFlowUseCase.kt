package com.example.musicdao.domain.usecases.torrents

import DownloadingTrack
import TorrentCache
import TorrentHandleStatus
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.util.MyResult
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

fun mapTorrentHandle(torrentHandle: TorrentHandle, directory: File): TorrentHandleStatus {
    return TorrentHandleStatus(
        id = torrentHandle.status().name().toString(),
        magnet = torrentHandle.makeMagnetUri(),
        finishedDownloading = torrentHandle.status().isFinished.toString(),
        pieces = "${torrentHandle.status().numPieces()}/${torrentHandle.status().pieces().count()}",
        files = torrentHandle.torrentFile()?.files().toString(),
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

    val downloadingTracks = (0..(files.numFiles() - 1)).map {
        DownloadingTrack(
            title = files.fileName(it),
            artist = "Artist Name",
            progress = Util.calculateDownloadProgress(
                fileProgress.get(it),
                files.fileSize(it)
            ),
            file = File("${directory}/${files.filePath(it)}"),
            fileIndex = it
        )
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
