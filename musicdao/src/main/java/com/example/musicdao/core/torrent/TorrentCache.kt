package com.example.musicdao.core.torrent

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.example.musicdao.SpecialPath
import com.example.musicdao.core.util.MyResult
import com.frostwire.jlibtorrent.TorrentHandle
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.O)
class TorrentCache @Inject constructor() {

    @Inject
    lateinit var torrentEngine: TorrentEngine

    @Inject
    lateinit var specialPath: SpecialPath

    fun copyIntoCache(files: Path): MyResult<Path> {
        when (val infoHash = TorrentEngine.generateInfoHash(files)) {
            is MyResult.Failure -> return MyResult.Failure(infoHash.message)
            is MyResult.Success -> {
                val output =
                    Paths.get("${specialPath.getPath()}/torrents/${infoHash.value}/content")
                try {
                    files.toFile().copyRecursively(output.toFile(), overwrite = true)
                } catch (exception: Exception) {
                    return MyResult.Failure(message = "Could not copy files.")
                }
                return MyResult.Success(output)
            }
        }
    }

    fun seedStrategy(): List<TorrentHandle> {
        val infoHashes = getAllDownloadedInfoHashes()
        val handles = infoHashes.mapNotNull {
            when (val res = verifyAndSeed(it)) {
                is MyResult.Success -> res.value
                is MyResult.Failure -> null
            }
        }
        return handles
    }

    fun verifyAndSeed(realInfoHash: String): MyResult<TorrentHandle> {
        val folder = Paths.get("${specialPath.getPath()}/torrents/$realInfoHash/content")
        return torrentEngine.verifyAndSeed(folder, realInfoHash)
    }

    fun download(realInfoHash: String): MyResult<TorrentHandle> {
        val folder = Paths.get("${specialPath.getPath()}/torrents/$realInfoHash/content")
        return torrentEngine.download(folder, realInfoHash)
    }

    fun get(realInfoHash: String): MyResult<TorrentHandle> {
        return torrentEngine.getTorrentHandle(realInfoHash)
    }

    fun getAllDownloadedInfoHashes(): List<String> {
        val files = Paths.get("${specialPath.getPath()}/torrents")
            ?.toFile()
            ?.listFiles()

        if (files == null) return listOf()

        return files
            .filter {
                when (TorrentEngine.verify(Paths.get("$it/content"), it.name)) {
                    is MyResult.Failure -> false
                    is MyResult.Success -> true
                }
            }
            .map { it.name }
    }

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

    fun copyToTempFolder(context: Context, uris: List<Uri>): File {
        val contentResolver = context.contentResolver
        val randomInt = Random.nextInt(0, Int.MAX_VALUE)
        val parentDir = "${context.cacheDir}/temp/content"

        File(parentDir).deleteRecursively()

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
            val input =
                contentResolver.openInputStream(uri) ?: throw Resources.NotFoundException()
            val fileLocation = "$parentDir/$fileName"

            FileUtils.copyInputStreamToFile(input, File(fileLocation))
            fileList.add(File(fileLocation))
        }

        return File("${context.cacheDir}/temp/content")
    }
}
