package com.example.musicdao.domain.usecases.torrents

import TorrentCache
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.util.MyResult
import com.frostwire.jlibtorrent.TorrentHandle

class DownloadIntentUseCase(private val torrentCache: TorrentCache) {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(id: String): MyResult<TorrentHandle> {
        return torrentCache.download(id)
    }

}
