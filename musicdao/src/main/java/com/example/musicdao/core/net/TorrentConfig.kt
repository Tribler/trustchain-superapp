package com.example.musicdao.core.net

import com.frostwire.jlibtorrent.TcpEndpoint

object TorrentConfig {
    val bootstrapPeers: MutableList<TcpEndpoint> = mutableListOf()

    init {
        // Cheat node: Seedbox on TU Delft
        bootstrapPeers.add(TcpEndpoint("130.161.119.207", 51413))
    }
}
