package nl.tudelft.trustchain.musicdao.core.torrent.status

import nl.tudelft.trustchain.musicdao.core.util.Util
import com.frostwire.jlibtorrent.TorrentHandle
import java.io.File

data class TorrentStatus(
    val id: String,
    val infoHash: String,
    val magnet: String,
    val finishedDownloading: String,
    val pieces: String,
    val files: String?,
    val seeding: String,
    val peers: String,
    val seeders: String,
    val uploadedBytes: String,
    val downloadedBytes: String,
    val downloadingTracks: List<DownloadingTrack>?
) {
    companion object {
        fun mapTorrentHandle(torrentHandle: TorrentHandle, directory: File): TorrentStatus {
            val files = torrentHandle.torrentFile()?.files()

            val formatted = if (files != null) {
                (0 until files.numFiles()).map {
                    files.filePath(it)
                }.joinToString("\n")
            } else {
                ""
            }

            return TorrentStatus(
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

            val downloadingTracks = (0 until files.numFiles()).mapNotNull {
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

        fun torrentHandleFiles(handle: TorrentHandle, cacheDir: File): List<FileWithIndex>? {
            val memoryFiles = handle.torrentFile()?.files() ?: return null

            return (0 until memoryFiles.numFiles()).mapNotNull {
                val file =
                    File("$cacheDir/torrents/${handle.infoHash()}/${memoryFiles.filePath(it)}")

                if (file.exists()) {
                    FileWithIndex(file, it)
                } else {
                    null
                }
            }
        }

        data class FileWithIndex(
            val file: File,
            val index: Int
        )
    }
}
