package nl.tudelft.trustchain.musicdao.core.torrent.fileProcessing

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import nl.tudelft.trustchain.musicdao.CachePath
import nl.tudelft.trustchain.musicdao.core.cache.CacheDatabase
import nl.tudelft.trustchain.musicdao.core.cache.entities.SongEntity
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
            val albumEntities = database.dao.getFromInfoHash(infoHash)

            for (albumEntity in albumEntities) {
                if (albumEntity.isDownloaded) {
                    Log.d("MusicDao", "DownloadFinishUseCase: Skipping $infoHash of ${albumEntity.id}, already downloaded")
                }

                val root = Paths.get("${cachePath.getPath()}/torrents/$infoHash")

                val mp3Files = FileProcessor.getMP3Files(root)
                Log.d("MusicDao", "DownloadFinishUseCase: mp3 files in $root: $mp3Files")

                val songs = mp3Files?.map {
                    SongEntity(
                        file = it.filename,
                        title = FileProcessor.getTitle(it),
                        artist = albumEntity.artist
                    )
                } ?: listOf()

                val cover = FileProcessor.getCoverArt(root)
                val updatedAlbumEntity = albumEntity.copy(
                    songs = songs,
                    cover = cover?.absolutePath,
                    root = root.toString(),
                    isDownloaded = true,
                    torrentPath = Paths.get("${cachePath.getPath()}/torrents/$infoHash.torrent").toString()
                )

                Log.d("MusicDao", "DownloadFinishUseCase: updated album with $updatedAlbumEntity")
                database.dao.update(updatedAlbumEntity)
            }
        }
    }
}
