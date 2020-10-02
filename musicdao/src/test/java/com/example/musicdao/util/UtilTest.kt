package com.example.musicdao.util

import com.frostwire.jlibtorrent.TorrentInfo
import org.junit.Assert
import org.junit.Test
import java.io.File

class UtilTest {
    @Test
    fun calculatePieceIndex() {
        val torrentFile = this.javaClass.getResource("/RFBMP.torrent")?.path
        Assert.assertNotNull(torrentFile)
        if (torrentFile == null) return
        Assert.assertNotNull(File(torrentFile))
        val fileIndex = 1
        val torrentInfo = TorrentInfo(File(torrentFile))
        val x = Util.calculatePieceIndex(fileIndex, torrentInfo)
        Assert.assertEquals(82, x)
    }
}
