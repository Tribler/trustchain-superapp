package nl.tudelft.trustchain.musicdao.core.torrent.status

import java.io.File

data class DownloadingTrack(
    val title: String,
    val artist: String,
    val progress: Int,
    val file: File,
    val fileIndex: Int
)
