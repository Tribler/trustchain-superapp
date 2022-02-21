package com.example.musicdao.core.usecases.torrents

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.torrent.TorrentCache
import com.example.musicdao.core.util.MyResult
import com.frostwire.jlibtorrent.TorrentHandle
import javax.inject.Inject

class DownloadIntentUseCase @Inject constructor(private val torrentCache: TorrentCache) {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(id: String): MyResult<TorrentHandle> {
        return torrentCache.download(id)
    }

}
