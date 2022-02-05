package com.example.musicdao.ui.release

import TorrentHandleStatus
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicdao.AppContainer
import com.example.musicdao.domain.usecases.torrents.DownloadIntentUseCase
import com.example.musicdao.domain.usecases.torrents.GetTorrentStatusFlowUseCase
import com.example.musicdao.domain.usecases.GetReleaseUseCase
import com.example.musicdao.domain.usecases.SaturatedRelease
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.O)
class ReleaseScreenViewModel(
    private
    val releaseId: String,
    private val getReleaseUseCase: GetReleaseUseCase,
    private val downloadIntentUseCase: DownloadIntentUseCase,
    private val getTorrentStatusFlowUseCase: GetTorrentStatusFlowUseCase
) : ViewModel() {

    private val _saturatedRelease: MutableStateFlow<SaturatedRelease> =
        MutableStateFlow(getReleaseUseCase.invoke(releaseId))
    val saturatedReleaseState: StateFlow<SaturatedRelease> = _saturatedRelease

    val _torrentHandleState: MutableStateFlow<TorrentHandleStatus?> = MutableStateFlow(null)
    val torrentHandleState: StateFlow<TorrentHandleStatus?> = _torrentHandleState

    private val _update: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            if (saturatedReleaseState.value.files == null) {
                downloadIntentUseCase.invoke(releaseId)
            }
            val flow = getTorrentStatusFlowUseCase(releaseId)
            if (flow != null) {
                val stateFlow = flow.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = null
                )
                stateFlow.collect {
                    _torrentHandleState.value = it
                    // TODO: turn this into boolean
                    val status = _torrentHandleState.value
                    if (status != null && status.finishedDownloading == "true" && !_update.value) {
                        _update.value = true
                        _saturatedRelease.value = getReleaseUseCase(releaseId)
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            releaseId: String,
            getReleaseUseCase: GetReleaseUseCase = AppContainer.getReleaseUseCase,
            downloadIntentUseCase: DownloadIntentUseCase = AppContainer.downloadIntentuseCase,
            getTorrentStatusFlowUseCase: GetTorrentStatusFlowUseCase = AppContainer.getTorrentStatusFlowUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReleaseScreenViewModel(
                    releaseId,
                    getReleaseUseCase,
                    downloadIntentUseCase,
                    getTorrentStatusFlowUseCase
                ) as T
            }
        }
    }
}
