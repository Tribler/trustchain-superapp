package com.example.musicdao.playlist

import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class TrackFragmentTest {
    @Test
    fun getDownloadProgress() {
        val fileProgress = 1_000L
        val fullSize = 10_000L
        val expectedPercentage = 10
        val releaseFragment = mockk<ReleaseFragment>()

        val track = TrackFragment("name", 0, releaseFragment, "1Mb")
        val percentage = track.getDownloadProgress(fileProgress, fullSize)
        Assert.assertEquals(expectedPercentage, percentage)
    }
}
