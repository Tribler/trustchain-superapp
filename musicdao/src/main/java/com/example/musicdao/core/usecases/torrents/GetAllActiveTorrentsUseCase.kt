package com.example.musicdao.core.usecases.torrents

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.torrent.TorrentEngine
import com.example.musicdao.core.torrent.api.TorrentHandleStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetAllActiveTorrentsUseCase @Inject constructor(
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
