package com.example.musicdao.util

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import org.junit.Assert
import org.junit.Test
import java.io.File

class ReleaseFactoryTest {
    @Test
    fun uriListFromLocalFiles() {
        val selectFilesIntent = Intent(Intent.ACTION_GET_CONTENT)
        selectFilesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        selectFilesIntent.data = Uri.fromFile(File("./src/res/test/Acoustic.mp3"))
        val list = ReleaseFactory.uriListFromLocalFiles(selectFilesIntent)
        Assert.assertEquals(0, list.size)

        val intent2 = Intent()
        intent2.clipData = ClipData.newHtmlText("a", "b", "c")
        val list2 = ReleaseFactory.uriListFromLocalFiles(intent2)
        Assert.assertEquals(0, list2.size)
    }
}
