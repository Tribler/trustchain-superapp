package nl.tudelft.trustchain.musicdao.core.repositories.album

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import nl.tudelft.trustchain.musicdao.CachePath
import nl.tudelft.trustchain.musicdao.core.repositories.AlbumRepository
import nl.tudelft.trustchain.musicdao.core.torrent.TorrentEngine
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.time.Instant
import java.util.*
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class BatchPublisher @Inject constructor(
    val cachePath: CachePath,
    val albumRepository: AlbumRepository
) {

    suspend fun publish(file: File) {
        val currentAlbums = albumRepository.getAlbums()

        if (!file.exists()) {
            Log.d("MusicDao", "BatchPublisher: file not found $file")
            return
        }
        Log.d("MusicDao", "BatchPublisher: file $file found, reading")

        // Reading and parsing.
        val bufferedReader = file.bufferedReader()
        val csvFormat = CSVFormat.Builder
            .create(CSVFormat.DEFAULT)
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build()

        // Dropping header from CSV file.
        val records = CSVParser(
            bufferedReader,
            csvFormat
        ).records.drop(1)

        // Go through all records and publish it on Trustchain.
        for (record in records) {
            val title = record.get(0)
            val artist = record.get(1)
            val magnet = record.get(2)

            val infoHash = TorrentEngine.magnetToInfoHash(magnet)

            // Only publish albums not published before.
            if (currentAlbums.find { TorrentEngine.magnetToInfoHash(it.magnet) == infoHash } == null) {
                Log.d("MusicDao", "Batchpublisher: $title, $artist, $infoHash")
                val id = UUID.randomUUID().toString()

                val result = albumRepository.createAlbum(
                    releaseId = id,
                    magnet = magnet,
                    title = title,
                    artist = artist,
                    releaseDate = Instant.now().toString()
                )

                Log.d("MusicDao", "Batchpublisher: $infoHash $result")
            }
        }
    }
}
