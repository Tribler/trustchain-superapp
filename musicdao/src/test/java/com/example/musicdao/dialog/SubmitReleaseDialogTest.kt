package com.example.musicdao.dialog

import com.example.musicdao.catalog.ReleaseOverviewFragment
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class SubmitReleaseDialogTest {
    @Test
    fun validateReleaseBlock() {
        val musicService = mockk<ReleaseOverviewFragment>()
        val dialog = SubmitReleaseDialog(musicService)
        val magnetLink = "magnet:?xt=urn:btih:45E4170514EE0CE20ABACF1FE256F9C73F95EF47&dn=Royalty%20Free%20Background%20Music%20Pack&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2F9.rarbg.to%3A2920%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=udp%3A%2F%2Ftracker.internetwarriors.net%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.pirateparty.gr%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.cyberia.is%3A6969%2Fannounce"
        val valid = dialog.validateReleaseBlock("a", "b", "c", magnetLink)
        Assert.assertNotNull(valid)
        // Release blocks require a magnet link to be published
        val invalid = dialog.validateReleaseBlock("a", "b", "c", "")
        Assert.assertNull(invalid)
        // Release blocks require metadata to be published
        val invalid2 = dialog.validateReleaseBlock("", "", "", magnetLink)
        Assert.assertNull(invalid2)
    }
}
