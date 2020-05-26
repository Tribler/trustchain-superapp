package com.example.musicdao.util

import com.frostwire.jlibtorrent.TorrentInfo

object TorrentUtil {
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
}
