package com.example.musicdao.net

import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentInfo
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.rules.ExpectedException
import java.io.File
import kotlin.jvm.*

class ContentSeederTest {
    lateinit var contentSeeder: ContentSeeder

    @Before
    fun init() {
        val sessionManager = SessionManager(false)
        val saveDir = File("./src/test/resources")
        Assert.assertTrue(saveDir.isDirectory)
        // Test whether it reads all local torrent files correctly
        contentSeeder = ContentSeeder.getInstance(sessionManager, saveDir)
        Assert.assertNotNull(contentSeeder)
    }

    @Test
    fun start() {
        val count = contentSeeder.start()
        Assert.assertEquals(1, count)
    }

    @Test
    fun addValidTorrent() {
        val torrentInfoName = "RFBMP"
        // Adding an existing and valid torrent file
        val torrentFile = File("./src/test/resources/RFBMP.torrent")
        val validTorrentInfo = contentSeeder.add(TorrentInfo(torrentFile), torrentInfoName)
        Assert.assertTrue(validTorrentInfo)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addInvalidTorrent() {
        val torrentInfoName = "RFBMP"
        // Adding a nonexisting torrent file
        val torrentFile = File("./somenonexistingfile.torrent")
        contentSeeder.add(TorrentInfo(torrentFile), torrentInfoName)
    }
}
