package com.example.musicdao.core.torrent.api

data class TorrentHandleStatus(
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
)
