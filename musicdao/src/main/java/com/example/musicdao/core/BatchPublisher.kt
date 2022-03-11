package com.example.musicdao.core

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.musicdao.CachePath
import com.example.musicdao.core.repositories.AlbumRepository
import com.example.musicdao.core.util.TorrentUtil
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import javax.inject.Inject

class BatchPublisher @Inject constructor(
    val cachePath: CachePath,
    val albumRepository: AlbumRepository
) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun run() {
        val file = Paths.get("${cachePath.getPath()}/output.csv").toFile()
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
            val infoHash = TorrentUtil.magnetToInfoHash(magnet)
            Log.d("MusicDao", "Batchpublisher: $title, $artist, $infoHash")
            val id = UUID.randomUUID().toString()

            val result = albumRepository.create(
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
