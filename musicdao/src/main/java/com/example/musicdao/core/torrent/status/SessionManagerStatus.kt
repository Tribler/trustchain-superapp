package com.example.musicdao.core.torrent.status

data class SessionManagerStatus(
    val interfaces: String,
    val dhtNodes: Long,
    val uploadRate: Long,
    val downloadRate: Long
)
