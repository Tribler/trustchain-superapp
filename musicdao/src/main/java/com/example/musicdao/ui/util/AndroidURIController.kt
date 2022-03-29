package com.example.musicdao.ui.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.musicdao.CachePath
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class AndroidURIController @Inject constructor(val cacheDir: CachePath) {

    val cachePath = cacheDir.getPath()!!

    fun copyIntoCache(uri: Uri, context: Context, file: Path): File? {
        val stream = uriToStream(uri, context) ?: return null
        FileUtils.copyInputStreamToFile(stream, file.toFile())
        return file.toFile()
    }

    companion object {
        fun uriToStream(uri: Uri, context: Context): InputStream? {
            return context.contentResolver.openInputStream(uri)
        }

        @SuppressLint("Range")
        fun uriToFileName(uri: Uri, context: Context): String? {
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                return if (it.moveToFirst()) {
                    val filePath = it.getString(it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                    Log.d("MusicDao", "uriToFileName: $filePath")
                    filePath
                } else {
                    null
                }
            }
            return null
        }
    }
}
