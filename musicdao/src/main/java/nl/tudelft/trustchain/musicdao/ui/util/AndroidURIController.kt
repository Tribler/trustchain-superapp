package nl.tudelft.trustchain.musicdao.ui.util

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import nl.tudelft.trustchain.musicdao.CachePath
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class AndroidURIController @Inject constructor(cacheDir: CachePath) {

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
    }
}
