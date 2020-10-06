package com.example.musicdao.util

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReleaseFactoryTest {
    private val mockUri = mockk<Uri>()

    @Test
    fun uriListFromLocalFiles() {
        val selectFilesIntent = Intent(Intent.ACTION_GET_CONTENT)
        selectFilesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        selectFilesIntent.data = Uri.parse("http://test/path")
        val list = ReleaseFactory.uriListFromLocalFiles(selectFilesIntent)
        Assert.assertEquals(1, list.size)

        val intent2 = Intent()
        intent2.clipData = ClipData.newRawUri("a", Uri.parse("http://test/path"))
        val list2 = ReleaseFactory.uriListFromLocalFiles(intent2)
        Assert.assertEquals(1, list2.size)
    }
}
