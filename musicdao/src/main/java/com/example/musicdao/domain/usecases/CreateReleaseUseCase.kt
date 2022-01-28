package com.example.musicdao.domain.usecases

import android.content.Context
import android.net.Uri
import com.example.musicdao.net.ContentSeeder
import com.example.musicdao.repositories.ReleaseRepository
import com.example.musicdao.repositories.TorrentRepository
import com.frostwire.jlibtorrent.TorrentInfo

class CreateReleaseUseCase(
    private val releaseTorrentRepository: TorrentRepository,
    private val releaseRepository: ReleaseRepository,
    private val contentSeeder: ContentSeeder
) {

    operator fun invoke(
        artist: String,
        title: String,
        releaseDate: String,
        publisher: String,
        uris: List<Uri>,
        context: Context
    ) {
        val torrentFile = releaseTorrentRepository.generateTorrent(context, uris)
        val torrentInfo = TorrentInfo(torrentFile)

        val validate = releaseRepository.validateReleaseBlock(
            title,
            artist,
            releaseDate,
            torrentInfo.makeMagnetUri(),
            torrentInfo
        )

        if (validate != null) {
            releaseRepository.publishRelease(
                torrentInfo.makeMagnetUri(),
                title,
                artists = artist,
                releaseDate,
                validate
            )
            contentSeeder.start()
        }
    }
}
