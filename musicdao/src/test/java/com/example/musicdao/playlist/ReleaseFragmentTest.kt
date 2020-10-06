package com.example.musicdao.playlist

import io.mockk.every
import io.mockk.spyk
import org.junit.Assert
import org.junit.Test
import java.io.File

class ReleaseFragmentTest {
    @Test
    fun resolveTorrentUrl() {
        val releaseFragment = spyk(ReleaseFragment(
            "magnet", "artists", "title", "10-10-2010",
            "publisherKey", "RFBMP"
        ))
        val resources = File("./src/test/resources")
        Assert.assertTrue(resources.isDirectory)
        every {
            releaseFragment.context?.cacheDir
        } returns resources
        Assert.assertEquals(
            resources.toURI().toURL().toString() + "RFBMP.torrent",
            releaseFragment.resolveTorrentUrl("RFBMP")
        )
    }
}
