package com.example.musicdao.domain.usecases

import com.example.musicdao.repositories.TorrentRepository
import com.example.musicdao.util.MyResult
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.coroutines.flow.MutableStateFlow

class GetTorrentUseCase(
    private val torrentRepository: TorrentRepository
) {

    operator fun invoke(id: String): MutableStateFlow<MyResult<TorrentHandle>> {
        return torrentRepository.get(id)
    }

}
