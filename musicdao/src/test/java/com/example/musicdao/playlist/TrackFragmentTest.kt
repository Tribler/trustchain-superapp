package com.example.musicdao.playlist

import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class TrackFragmentTest {
    @Test
    fun getDownloadProgress() {
        val expectedPercentage = 10
        val releaseFragment = mockk<ReleaseFragment>()

        val track = TrackFragment("name", 0, releaseFragment, "1Mb", 10)
        val percentage = track.getProgress()
        Assert.assertEquals(expectedPercentage, percentage)
    }
}
