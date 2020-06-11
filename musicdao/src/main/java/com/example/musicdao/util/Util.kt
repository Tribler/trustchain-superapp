package com.example.musicdao.util

import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.TorrentInfo
import com.github.se_bastiaan.torrentstream.Torrent

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

    /**
     * Prefer the first pieces to be downloaded of the selected audio file over the other pieces,
     * so that the first seconds of the track can be buffered as soon as possible
     */
    fun setSequentialPriorities(torrent: Torrent) {
        val piecePriorities: Array<Priority> =
            torrent.torrentHandle.piecePriorities()
        for ((index, piecePriority) in piecePriorities.withIndex()) {
            if (piecePriority == Priority.SEVEN) {
                torrent.torrentHandle.piecePriority(index, Priority.NORMAL)
            }
//            if (piecePriority == Priority.NORMAL) {
//                torrent.torrentHandle.piecePriority(index, Priority.SIX)
//            }
//            if (piecePriority == Priority.IGNORE) {
//                torrent.torrentHandle.piecePriority(index, Priority.NORMAL)
//            }
        }
        for (i in torrent.interestedPieceIndex until torrent.interestedPieceIndex + torrent.piecesToPrepare) {
            torrent.torrentHandle.piecePriority(i, Priority.SEVEN)
        }
    }
}
