package com.example.musicdao

import io.mockk.every
import io.mockk.spyk
import org.junit.Assert
import org.junit.Test
import java.io.File

class MusicServiceTest {
    @Test
    fun startup() {
        val musicService = spyk(MusicService())
        every {
            musicService.applicationContext.cacheDir
        } returns File("./")
        Assert.assertEquals("Starting torrent client...", musicService.getStatsOverview())
    }
}
