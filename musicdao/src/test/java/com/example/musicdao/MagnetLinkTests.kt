package com.example.musicdao

import com.example.musicdao.core.util.Util
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MagnetLinkTests {
    @Test
    fun extractDisplayName() {
        val magnetLink =
            "magnet:?xt=urn:btih:45E4170514EE0CE20ABACF1FE256F9C73F95EF47&dn=Royalty%20Free%20Background%20Music%20Pack&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2F9.rarbg.to%3A2920%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=udp%3A%2F%2Ftracker.internetwarriors.net%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.pirateparty.gr%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.cyberia.is%3A6969%2Fannounce"
        assertEquals(
            "Royalty%20Free%20Background%20Music%20Pack",
            Util.extractNameFromMagnet(magnetLink)
        )
    }
}
