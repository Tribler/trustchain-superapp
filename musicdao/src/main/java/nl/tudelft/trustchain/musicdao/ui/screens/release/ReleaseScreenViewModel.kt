package nl.tudelft.trustchain.musicdao.ui.screens.release

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import nl.tudelft.trustchain.musicdao.core.cache.CacheDatabase
import nl.tudelft.trustchain.musicdao.core.cache.entities.AlbumEntity
import nl.tudelft.trustchain.musicdao.core.repositories.model.Album
import nl.tudelft.trustchain.musicdao.core.torrent.TorrentEngine
import nl.tudelft.trustchain.musicdao.core.torrent.status.TorrentStatus
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
    private val database: CacheDatabase,
    private val torrentEngine: TorrentEngine,
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
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(releaseId) as T
            }
        }
    }

    private var releaseLiveData: LiveData<AlbumEntity> = MutableLiveData(null)
    var saturatedReleaseState: LiveData<Album?> = MutableLiveData()

    private val _torrentState: MutableStateFlow<TorrentStatus?> = MutableStateFlow(null)
    val torrentState: StateFlow<TorrentStatus?> = _torrentState

    init {
        viewModelScope.launch {
            releaseLiveData = database.dao.getLiveData(releaseId)
            saturatedReleaseState = releaseLiveData.map { it.toAlbum() }

            val release = database.dao.get(releaseId)

            release.let { _release ->
                if (!_release.isDownloaded) {
                    torrentEngine.download(_release.magnet)
                }

                while (isActive) {
                    if (_release.infoHash != null) {
                        _torrentState.value = torrentEngine.getTorrentStatus(_release.infoHash)
                    }
                    delay(1000L)
                }
            }
        }
    }
}
