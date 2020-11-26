package com.example.musicdao.util

import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.TorrentInfo
import com.github.se_bastiaan.torrentstream.Torrent
import io.mockk.every
import io.mockk.mockk
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

    @Test
    fun extractInfoHashFromMagnet() {
        val magnet = "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c&dn=displayname"
        val name = Util.extractInfoHash(magnet)
        Assert.assertEquals(name, Sha1Hash("a83cc13bf4a07e85b938dcf06aa707955687ca7c"))
        Assert.assertEquals(name.toString(), Sha1Hash("a83cc13bf4a07e85b938dcf06aa707955687ca7c").toString())
        Assert.assertNotEquals(name, "somethingelse")
    }

    @Test
    fun extractNameFromMagnet() {
        val magnet = "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c&dn=displayname"
        val name = Util.extractNameFromMagnet(magnet)
        Assert.assertEquals(name, "displayname")
        Assert.assertNotEquals(name, "somethingelse")
    }

    @Test
    fun readableBytes() {
        val eightMbs: Long = 1024 * 1024 * 8
        val eightKbs: Long = 1024 * 8
        val eightBytes: Long = 8
        Assert.assertEquals(Util.readableBytes(eightMbs), "8Mb")
        Assert.assertEquals(Util.readableBytes(eightKbs), "8Kb")
        Assert.assertEquals(Util.readableBytes(eightBytes), "8B")
    }

    @Test
    fun checkAndSanitizeTrackNames() {
        val fileName = "Valid_File-Name.mp3"
        val fileName2 = "invalid-File.txt"
        Assert.assertEquals(
            "Valid File-Name",
            Util.checkAndSanitizeTrackNames(fileName)
        )
        Assert.assertNull(Util.checkAndSanitizeTrackNames(fileName2))
    }

    @Test
    fun setSequentialPriorities() {
        val priorities: Array<Priority> = arrayOf(
            Priority.SEVEN,
            Priority.SEVEN,
            Priority.SEVEN,
            Priority.NORMAL,
            Priority.IGNORE
        )
        val torrent = mockk<Torrent>()
        every { torrent.torrentHandle.piecePriorities() } returns priorities
        every { torrent.interestedPieceIndex } returns 1
        every { torrent.piecesToPrepare } returns 2
        val expectedPriorites: Array<Priority> = arrayOf(
            Priority.SIX,
            Priority.SEVEN,
            Priority.SEVEN,
            Priority.FIVE,
            Priority.NORMAL
        )
        val answer = Util.setSequentialPriorities(torrent, onlyCalculating = true)
        Assert.assertArrayEquals(expectedPriorites, answer)
    }
}
