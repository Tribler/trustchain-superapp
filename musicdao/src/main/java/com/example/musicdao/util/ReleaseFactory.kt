package com.example.musicdao.util

import android.content.ContentResolver
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.provider.MediaStore
import com.turn.ttorrent.client.SharedTorrent
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream

class ReleaseFactory {
    companion object {
        fun uriListFromLocalFiles(intent: Intent): List<Uri> {
            // This should be reached when the chooseFile intent is completed and the user selected
            // an audio file
            val uriList = mutableListOf<Uri>()
            val singleFileUri = intent.data
            if (singleFileUri != null) {
                // Only one file is selected
                uriList.add(singleFileUri)
            }
            val clipData = intent.clipData
            if (clipData != null) {
                // Multiple files are selected
                val count = clipData.itemCount
                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i).uri
                    uriList.add(uri)
                }
            }
            return uriList
        }

        /**
         * Generates a a .torrent File from local files
         * @param uris the list of Uris pointing to local audio source files to publish
         */
        fun generateTorrent(parentDirPath: String, uris: List<Uri>, contentResolver: ContentResolver, ioInteraction: Boolean = true): File {
            val fileList = mutableListOf<File>()
            val projection =
                arrayOf<String>(MediaStore.MediaColumns.DISPLAY_NAME)
            for (uri in uris) {
                var fileName = ""
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(0)
                    }
                }

                if (fileName == "") throw Error("Source file name for creating torrent not found")
                val input = contentResolver.openInputStream(uri) ?: throw Resources.NotFoundException()
                val fileLocation = "$parentDirPath/$fileName"

                if (ioInteraction) FileUtils.copyInputStreamToFile(input, File(fileLocation))
                fileList.add(File(fileLocation))
            }

            val torrent = SharedTorrent.create(File(parentDirPath), fileList, 65535, listOf(), "")
            val torrentFile = "$parentDirPath.torrent"
            if (ioInteraction) torrent.save(FileOutputStream(torrentFile))
            return File(torrentFile)
        }
    }
}
