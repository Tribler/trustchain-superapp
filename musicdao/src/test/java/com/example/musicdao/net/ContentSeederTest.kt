package com.example.musicdao.net

import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

class ContentSeederTest {
    private lateinit var contentSeeder: ContentSeeder

    @Before
    fun init() {
        val sessionManager = SessionManager()
        val saveDir = File("./src/test/resources")
        Assert.assertTrue(saveDir.isDirectory)
        // Test whether it reads all local torrent files correctly
        contentSeeder = ContentSeeder(saveDir, sessionManager)
        Assert.assertNotNull(contentSeeder)
    }

    @Test
    fun start() {
        val count = contentSeeder.start()
        Assert.assertEquals(0, count)
    }

    @Test
    fun addValidTorrent() {
        val torrentInfoName = "RFBMP"
        // Adding an existing and valid torrent file
        val torrentFile = File("./src/test/resources/RFBMP.torrent")
        val validTorrentInfo =
            contentSeeder.saveTorrentInfoToFile(TorrentInfo(torrentFile), torrentInfoName)
        Assert.assertTrue(validTorrentInfo)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addInvalidTorrent() {
        val torrentInfoName = "RFBMP"
        // Adding a nonexisting torrent file
        val torrentFile = File("./somenonexistingfile.torrent")
        contentSeeder.saveTorrentInfoToFile(TorrentInfo(torrentFile), torrentInfoName)
    }

    @Test
    fun updateSwarmHealth() {
        val torrentHandle = mockk<TorrentHandle>()
        every { torrentHandle.status().numPeers() } returns 1
        every { torrentHandle.status().numSeeds() } returns 1
        every { torrentHandle.infoHash() } returns Sha1Hash.max()
        contentSeeder.updateSwarmHealth(torrentHandle)
        Assert.assertEquals(contentSeeder.swarmHealthMap.size, 1)
        contentSeeder.updateSwarmHealth(torrentHandle)
        Assert.assertEquals(contentSeeder.swarmHealthMap.size, 1)
    }
}
