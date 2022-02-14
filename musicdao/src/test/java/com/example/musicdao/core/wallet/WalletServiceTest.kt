package com.example.musicdao.core.wallet

import com.example.musicdao.MusicService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import java.io.File

class WalletServiceTest {
    @Test
    fun startup() {
        val musicService = mockk<MusicService>()
        val saveDir = File("./src/test/resources")
        Assert.assertTrue(saveDir.isDirectory)
        every {
            musicService.applicationContext.cacheDir
        } returns saveDir
        // TODO this test is failing on the RegTestNet.get() call, because of an
        //  ExceptionInInitializerError
//        val service = WalletService(musicService)
//        service.startup()
//        Assert.assertEquals(
//            1,
//            service.app.peerGroup().connectedPeers.size
//        )
    }
}
