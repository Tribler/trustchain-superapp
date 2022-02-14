package com.example.musicdao.core.usecases.torrents

import TorrentEngine
import com.example.musicdao.core.torrent.api.TorrentHandleStatus
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class GetAllActiveTorrentsUseCase(
    private val getTorrentStatusFlowUseCase: GetTorrentStatusFlowUseCase,
    private val torrentEngine: TorrentEngine
) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(): Flow<List<Flow<TorrentHandleStatus>>> = flow {
        torrentEngine.getAllTorrents().collect { list ->
            emit(listOf())
            val result = list.mapNotNull {
                getTorrentStatusFlowUseCase.invoke(it)
            }
            emit(result)
        }
    }

}
