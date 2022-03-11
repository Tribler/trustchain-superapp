package com.example.musicdao.core.usecases.torrents

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.torrent.TorrentEngine
import com.frostwire.jlibtorrent.TorrentHandle
import javax.inject.Inject

class DownloadIntentUseCase @Inject constructor(private val torrentEngine: TorrentEngine) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(releaseId: String): TorrentHandle? {
        return torrentEngine.download(releaseId)
    }
}
