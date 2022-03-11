package com.example.musicdao.core.usecases

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.musicdao.CachePath
import com.example.musicdao.core.database.CacheDatabase
import com.example.musicdao.core.database.entities.SongEntity
import com.example.musicdao.core.torrent.ReleaseProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Paths

@RequiresApi(Build.VERSION_CODES.O)
class DownloadFinishUseCase constructor(
    private val database: CacheDatabase,
    private val cachePath: CachePath
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    operator fun invoke(infoHash: String) {
        coroutineScope.launch {
            Log.d("MusicDao", "DownloadFinishUseCase: $infoHash")

            // TODO: fix, multiple releases can potentially have some info-hash, will break
            val albumEntity = database.dao.getFromInfoHash(infoHash)
            val releaseId = albumEntity.id
            val root = Paths.get("${cachePath.getPath()}/torrents/$releaseId")

            val mp3Files = ReleaseProcessor.getMP3Files(root)
            Log.d("MusicDao", "DownloadFinishUseCase: mp3 files in $root: $mp3Files")

            val songs = mp3Files?.map {
                SongEntity(
                    file = it.filename,
                    title = FileProcessor.getTitle(it),
                    name = FileProcessor.getTitle(it),
                    artist = albumEntity.artist
                )
            } ?: listOf()

            val cover = FileProcessor.getCoverArt(root)
            val updatedAlbumEntity = albumEntity.copy(
                songs = songs,
                cover = cover?.absolutePath,
                root = root.toString(),
                isDownloaded = true
            )

            Log.d("MusicDao", "DownloadFinishUseCase: updated album with $updatedAlbumEntity")
            database.dao.update(updatedAlbumEntity)
        }
    }
}
