package com.example.musicdao.util

import com.frostwire.jlibtorrent.TorrentInfo

object Util {
    fun calculatePieceIndex(fileIndex: Int, torrentInfo: TorrentInfo): Int {
        var pieceIndex = 0
        for (i in 0..fileIndex) {
            var fullSize = torrentInfo.files().fileSize(i)
            while (fullSize > 1) {
                fullSize -= torrentInfo.pieceSize(pieceIndex)
                pieceIndex += 1
            }
        }
        return pieceIndex
    }

    fun readableBytes(bytes: Long): String {
        if (bytes > 1024 && bytes <= (1024 * 1024)) return "${(bytes / 1024)}Kb"
        if (bytes > (1024 * 1024)) return "${(bytes / (1024 * 1024))}Mb"
        return "${bytes}B"
    }
}
