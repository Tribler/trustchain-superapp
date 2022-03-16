package com.example.musicdao.ui.screens.release

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import com.example.musicdao.CachePath
import com.example.musicdao.core.database.CacheDatabase
import com.example.musicdao.core.database.entities.AlbumEntity
import com.example.musicdao.core.model.Album
import com.example.musicdao.core.torrent.TorrentEngine
import com.example.musicdao.core.torrent.api.TorrentHandleStatus
import com.example.musicdao.core.usecases.releases.GetRelease
import com.example.musicdao.core.usecases.torrents.DownloadIntentUseCase
import com.example.musicdao.core.usecases.torrents.GetTorrentStatusFlowUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
@RequiresApi(Build.VERSION_CODES.O)
class ReleaseScreenViewModel @AssistedInject constructor(
    @Assisted private val releaseId: String,
    private val getReleaseUseCase: GetRelease,
    private val downloadIntentUseCase: DownloadIntentUseCase,
    private val getTorrentStatusFlowUseCase: GetTorrentStatusFlowUseCase,
    private val database: CacheDatabase,
    private val torrentEngine: TorrentEngine,
    private val cachePath: CachePath
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

    private var releaseLiveData: LiveData<AlbumEntity> = MutableLiveData(null)
    var saturatedReleaseState: LiveData<Album?> = MutableLiveData()

    val _torrentHandleState: MutableStateFlow<TorrentHandleStatus?> = MutableStateFlow(null)
    val torrentHandleState: StateFlow<TorrentHandleStatus?> = _torrentHandleState

    init {
        viewModelScope.launch {
            releaseLiveData = database.dao.getLiveData(releaseId)
            saturatedReleaseState = Transformations.map(releaseLiveData, { it.toAlbum() })

            val release = database.dao.get(releaseId)

            release.let { release ->
                if (!release.isDownloaded) {
                    downloadIntentUseCase.invoke(release.magnet)
                }

                while (isActive) {
                    val infoHash = TorrentEngine.magnetToInfoHash(release.magnet)
                    if (infoHash != null) {
                        torrentEngine.get(infoHash)?.let {
                            _torrentHandleState.value =
                                GetTorrentStatusFlowUseCase.mapTorrentHandle(
                                    it,
                                    cachePath.getPath()!!.toFile()
                                )
                        }
                    }
                    delay(1000L)
                }
            }
        }
    }
}
