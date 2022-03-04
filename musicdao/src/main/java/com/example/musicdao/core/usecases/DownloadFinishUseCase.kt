package com.example.musicdao.core.usecases

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.database.CacheDatabase
import com.example.musicdao.core.database.entities.SongEntity
import com.example.musicdao.core.torrent.FilesHelper
import com.example.musicdao.core.util.Util
import com.mpatric.mp3agic.Mp3File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Paths

class DownloadFinishUseCase constructor(
    val database: CacheDatabase,
    private val filesHelper: FilesHelper
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(id: String) {

        coroutineScope.launch {
            val albumEntity = database.dao.get(id)
            val files = filesHelper.getFiles(id)

            val songs = files?.mapNotNull {
                try {
                    val mp3 = Mp3File(it)
                    SongEntity(
                        file = it.absolutePath,
                        title = Util.getTitle(mp3) ?: (
                            Util.checkAndSanitizeTrackNames(it.name)
                                ?: it.name
                            ),
                        name = "name",
                        artist = albumEntity.artist
                    )
                } catch (e: Exception) {
                    null
                }
            }

            var cover: File? = null
            if (files != null && files.isNotEmpty()) {
                cover = Util.findCoverArt(files.get(0).parentFile.parentFile)
            }

            val root =
                Paths.get("${filesHelper.specialPath.getPath()!!}/torrents/$id/content").toFile()
            val updatedAlbumEntity = albumEntity.copy(
                songs = songs ?: listOf(),
                cover = cover?.absolutePath,
                root = root.absolutePath
            )

            database.dao.update(updatedAlbumEntity)
        }
    }
}
