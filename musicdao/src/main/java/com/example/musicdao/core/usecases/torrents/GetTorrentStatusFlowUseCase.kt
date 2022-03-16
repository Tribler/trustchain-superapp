package com.example.musicdao.core.usecases.torrents

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.musicdao.core.torrent.TorrentEngine
import com.example.musicdao.core.torrent.api.DownloadingTrack
import com.example.musicdao.core.torrent.api.TorrentHandleStatus
import com.example.musicdao.core.util.Util
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

class GetTorrentStatusFlowUseCase @Inject constructor(private val torrentEngine: TorrentEngine) {
    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(infoHash: String): Flow<TorrentHandleStatus>? {
        val handle = torrentEngine.get(infoHash = infoHash) ?: return null
        return flow {
            while (true) {
                emit(mapTorrentHandle(handle, torrentEngine.cachePath.getPath()!!.toFile()))
                delay(REFRESH_DELAY)
            }
        }
    }
    companion object {
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

        private fun downloadingTracks(handle: TorrentHandle, directory: File): List<DownloadingTrack>? {
            val files = handle.torrentFile()?.files() ?: return null
            val fileProgress = handle.fileProgress()

            val downloadingTracks = (0..(files.numFiles() - 1)).mapNotNull {
                val file = File("$directory/torrents/${handle.infoHash()}/${files.filePath(it)}")
                if (file.exists()) {
                    DownloadingTrack(
                        title = (
                            Util.checkAndSanitizeTrackNames(files.fileName(it))
                                ?: files.fileName(it)
                            ),
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
        private const val REFRESH_DELAY = 1000L
    }
}
