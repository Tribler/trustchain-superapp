package com.example.musicdao.ui.release

import android.util.Log
import androidx.lifecycle.*
import com.example.musicdao.AppContainer
import com.example.musicdao.AppContainer.releaseRepository
import com.example.musicdao.domain.usecases.GetTorrentUseCase
import com.example.musicdao.repositories.ReleaseBlock
import com.example.musicdao.repositories.ReleaseRepository
import com.example.musicdao.repositories.TorrentRepository
import com.example.musicdao.util.MyResult
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

data class UIState(
    val releaseBlock: ReleaseBlock?,
    val isDownloading: Boolean,
    val isDownloaded: Boolean,
    val files: List<File>,
    val handle: TorrentHandle?
)

val DEFAULT_UI_STATE = UIState(
    releaseBlock = null,
    isDownloaded = false,
    isDownloading = false,
    files = listOf(),
    handle = null
)

data class DownloadingTrack(
    val title: String,
    val artist: String,
    val progress: Int,
    val file: File,
    val fileIndex: Int
)

data class MyTrack(
    val title: String,
    val artist: String,
    val file: File,
)

sealed interface ReleaseUIState {

    val isLoading: Boolean

    /**
     * There are no posts to render.
     *
     * This could either be because they are still loading or they failed to load, and we are
     * waiting to reload them.
     */
    data class Nothing(
        override val isLoading: Boolean,
    ) : ReleaseUIState

    data class NoTracks(
        override val isLoading: Boolean,
        val releaseBlock: ReleaseBlock
    ) : ReleaseUIState

    /**
     * There are posts to render, as contained in [postsFeed].
     *
     * There is guaranteed to be a [selectedPost], which is one of the posts from [postsFeed].
     */
    data class Downloading(
        val downloadingTracks: List<DownloadingTrack>,
        val torrentHandle: TorrentHandle,
        override val isLoading: Boolean,
        val releaseBlock: ReleaseBlock,
    ) : ReleaseUIState

    data class Downloaded(
        val tracks: List<MyTrack>,
        override val isLoading: Boolean,
        val releaseBlock: ReleaseBlock,
        val torrentHandle: TorrentHandle?
    ) : ReleaseUIState

    data class DownloadedWithCover(
        val tracks: List<MyTrack>,
        val cover: File,
        override val isLoading: Boolean,
        val releaseBlock: ReleaseBlock,
        val torrentHandle: TorrentHandle?
    ) : ReleaseUIState

    data class Seeding(
        val tracks: List<MyTrack>,
        override val isLoading: Boolean,
        val releaseBlock: ReleaseBlock,
        val torrentHandle: TorrentHandle
    ) : ReleaseUIState
}

private data class ReleaseViewModelState(
    val isLoading: Boolean = false,
    val releaseBlock: ReleaseBlock? = null,
    val downloadingTracks: List<DownloadingTrack> = emptyList(),
    val torrentHandle: TorrentHandle? = null,
    val tracks: List<MyTrack> = emptyList(),
    val cover: File? = null,
    val isSeeding: Boolean = false
) {
    fun toUiState(): ReleaseUIState {
        if (releaseBlock == null) {
            return ReleaseUIState.Nothing(isLoading)
        }
        if (!tracks.isEmpty()) {
            if (torrentHandle != null) {
                return ReleaseUIState.Seeding(
                    tracks,
                    isLoading,
                    releaseBlock,
                    torrentHandle
                )
            }
            if (cover != null) {
                return ReleaseUIState.DownloadedWithCover(
                    tracks,
                    cover,
                    isLoading,
                    releaseBlock,
                    torrentHandle
                )
            } else {
                return ReleaseUIState.Downloaded(
                    tracks,
                    isLoading,
                    releaseBlock,
                    torrentHandle
                )
            }
        }
        if (torrentHandle != null) {
            return ReleaseUIState.Downloading(
                downloadingTracks,
                torrentHandle,
                isLoading,
                releaseBlock
            )
        }
        return ReleaseUIState.NoTracks(
            isLoading,
            releaseBlock
        )
    }
}

class ReleaseScreenViewModel(
    private
    val releaseId: String,
    releaseRepository: ReleaseRepository,
    private val torrentRepository: TorrentRepository,
    private val getTorrentUseCase: GetTorrentUseCase
) : ViewModel() {

    private val viewModelState = MutableStateFlow(ReleaseViewModelState(isLoading = true))
    val uiState = viewModelState.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, viewModelState.value.toUiState())

    val torrentState: MutableStateFlow<MyResult<TorrentHandle>> =
        getTorrentUseCase.invoke(releaseId)

    private var attemptingToDownload: Boolean = false

    init {
        viewModelScope.launch {
            val releaseBlock = releaseRepository.getReleaseBlock(releaseId)
            viewModelState.value = viewModelState.value.copy(releaseBlock = releaseBlock)

            refresh()
        }
    }

    suspend fun refresh() {
        viewModelState.value = viewModelState.value.copy(isLoading = true)

        val releaseBlock = releaseRepository.getReleaseBlock(releaseId)
        val isDownloaded = torrentRepository.isDownloaded(releaseBlock.torrentInfoName)
        val isDownloading = torrentRepository.isDownloading(releaseBlock.torrentInfoName)

        if (isDownloaded) {
            val files = torrentRepository.getFiles(releaseBlock.torrentInfoName)
            val tracks = files.map {
                MyTrack(
                    title = releaseBlock.title,
                    artist = releaseBlock.artist,
                    file = it,
                )
            }

            var cover: File? = null
            if (files.size != 0) {
                val file = files.get(0).parentFile
                if (file != null && file.exists()) {
                    cover = Util.findCoverArt(file)
                }
            }
            if (cover != null) {
                viewModelState.value =
                    viewModelState.value.copy(tracks = tracks, cover = cover, isLoading = false)
            } else {
                viewModelState.value = viewModelState.value.copy(tracks = tracks, isLoading = false)
            }

        } else if (isDownloading) {
            refreshDownloadingTracks(releaseBlock)
        } else {
            iterativelyAttemptToStartDownload()
        }
    }

    fun refreshDownloadingTracks(releaseBlock: ReleaseBlock) {
        val handle = torrentRepository.getHandle(releaseBlock.torrentInfoName)!!
        val files = handle.torrentFile().files()
        val fileProgress = handle.fileProgress()

        val downloadingTracks = (0..(files.numFiles() - 1)).map {
            DownloadingTrack(
                title = files.fileName(it),
                artist = releaseBlock.artist,
                progress = Util.calculateDownloadProgress(
                    fileProgress.get(it),
                    files.fileSize(it)
                ),
                file = File("${torrentRepository.directory}/${files.filePath(it)}"),
                fileIndex = it
            )
        }
        viewModelState.value = viewModelState.value.copy(
            torrentHandle = handle,
            downloadingTracks = downloadingTracks,
            isLoading = false
        )
    }

    suspend fun iterativelyAttemptToStartDownload() {
        val releaseBlock = releaseRepository.getReleaseBlock(releaseId)
        attemptingToDownload = true
        viewModelScope.launch {
            while (isActive && attemptingToDownload) {
                val result = torrentRepository.startDownload(
                    releaseBlock.torrentInfoName,
                    releaseBlock.magnet,
                    callback = {
                        refreshDownloadingTracks(releaseBlock)
                    })
                if (result != null) {
                    attemptingToDownload = false
                    refresh()
                }
            }
        }
    }


    fun setFilePriority(downloadingTrack: DownloadingTrack) {
        val state = uiState.value as ReleaseUIState.Downloading
        val pieceIndex =
            Util.calculatePieceIndex(downloadingTrack.fileIndex, state.torrentHandle.torrentFile())
        Util.setTorrentPriorities(
            state.torrentHandle,
            false,
            pieceIndex,
            downloadingTrack.fileIndex
        )

    }

    companion object {
        fun provideFactory(
            releaseId: String,
            releaseRepository: ReleaseRepository,
            torrentRepository: TorrentRepository,
            getTorrentUseCase: GetTorrentUseCase = AppContainer.getTorrentUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReleaseScreenViewModel(
                    releaseId,
                    releaseRepository,
                    torrentRepository,
                    getTorrentUseCase
                ) as T
            }
        }
    }
}
