package com.example.musicdao.core.torrent

import android.os.Build
import androidx.annotation.RequiresApi
import com.mpatric.mp3agic.Mp3File
import java.io.File
import java.nio.file.Path

@RequiresApi(Build.VERSION_CODES.O)
class ReleaseProcessor {

    companion object {
        private fun getFiles(path: Path): List<File>? {
            val folder = path.toFile()
            if (!folder.exists()) {
                return null
            }
            return getFiles(folder)
        }

        fun getFiles(folder: File): List<File>? {
            if (!folder.exists()) {
                return null
            }
            return folder.walkTopDown().map { it }.toList()
        }

        fun getMP3Files(path: Path): List<Mp3File>? {
            val files = getFiles(path) ?: return null
            val mp3Files = files.toList().filter {
                it.extension == "mp3"
            }

            return mp3Files.mapNotNull {
                try {
                    Mp3File(it)
                } catch (e: Exception) {
                    null
                }
            }
        }

        fun folderExistsAndHasFiles(folder: File): Boolean {
            return folder.exists() && getFiles(folder)?.isEmpty() == false
        }
    }
}
