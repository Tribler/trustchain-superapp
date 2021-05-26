package com.example.musicdao.playlist

import com.frostwire.jlibtorrent.SessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert
import org.junit.Test
import java.io.File

class ReleaseFragmentTest {
    @Test
    fun resolveTorrentUrl() {
        val sessionManager = mockk<SessionManager>()
        val releaseFragment = spyk(
            ReleaseFragment(
                "magnet",
                "artists",
                "title",
                "10-10-2010",
                "publisherKey",
                "RFBMP",
                sessionManager
            )
        )
        val resources = File("./src/test/resources")
        Assert.assertTrue(resources.isDirectory)
        every {
            releaseFragment.context?.cacheDir
        } returns resources
    }
}
