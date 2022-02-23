package com.example.musicdao.ui.release

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicdao.core.model.Album
import com.example.musicdao.core.torrent.api.TorrentHandleStatus
import com.example.musicdao.core.usecases.releases.GetRelease
import com.example.musicdao.core.usecases.torrents.DownloadIntentUseCase
import com.example.musicdao.core.usecases.torrents.GetTorrentStatusFlowUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.O)
class ReleaseScreenViewModel @AssistedInject constructor(
    @Assisted private val releaseId: String,
    private val getReleaseUseCase: GetRelease,
    private val downloadIntentUseCase: DownloadIntentUseCase,
    private val getTorrentStatusFlowUseCase: GetTorrentStatusFlowUseCase,
) : ViewModel() {

    @AssistedFactory
    interface ReleaseScreenViewModelFactory {
        fun create(releaseId: String): ReleaseScreenViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: ReleaseScreenViewModelFactory,
            releaseId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(releaseId) as T
            }
        }
    }

    private val _saturatedRelease: MutableStateFlow<Album?> = MutableStateFlow(null)
    val saturatedReleaseState: StateFlow<Album?> = _saturatedRelease

    val _torrentHandleState: MutableStateFlow<TorrentHandleStatus?> = MutableStateFlow(null)
    val torrentHandleState: StateFlow<TorrentHandleStatus?> = _torrentHandleState

    private val _update: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _saturatedRelease.value = getReleaseUseCase.invoke(releaseId)

            val release = _saturatedRelease.value

            if (release?.songs == null || release.songs.isEmpty()) {
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
}
