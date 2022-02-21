package com.example.musicdao.core.torrent

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.SpecialPath
import com.example.musicdao.core.util.MyResult
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject

class FilesHelper @Inject constructor(val specialPath: SpecialPath) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFiles(realInfoHash: String): List<File>? {
        val folder = Paths.get("${specialPath.getPath()}/torrents/$realInfoHash/content").toFile()
        val verify = TorrentEngine.verify(folder.toPath(), folder.parentFile.name)

        when (verify) {
            is MyResult.Failure -> return null
            is MyResult.Success -> {
                val files = folder.listFiles() ?: return null
                return files.toList().filter {
                    it.extension == "mp3"
                }
            }
        }
    }

}
