package com.example.musicdao.core.torrent.api

import java.io.File

data class DownloadingTrack(
    val title: String,
    val artist: String,
    val progress: Int,
    val file: File,
    val fileIndex: Int
)
