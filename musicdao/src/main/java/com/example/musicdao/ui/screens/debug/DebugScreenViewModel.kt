package com.example.musicdao.ui.screens.search

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicdao.core.torrent.TorrentEngine
import com.example.musicdao.core.torrent.status.SessionManagerStatus
import com.example.musicdao.core.torrent.status.TorrentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class DebugScreenViewModel @Inject constructor(
    private val torrentEngine: TorrentEngine,
) : ViewModel() {

    private val _status: MutableStateFlow<List<TorrentStatus>> = MutableStateFlow(listOf())
    val status: StateFlow<List<TorrentStatus>> = _status

    private val _sessionStatus: MutableStateFlow<SessionManagerStatus?> = MutableStateFlow(null)
    val sessionStatus: StateFlow<SessionManagerStatus?> = _sessionStatus

    init {
        viewModelScope.launch {
            while (isActive) {
                _status.value = torrentEngine.getAllTorrentStatus()
                delay(5000L)
            }
        }

        viewModelScope.launch {
            while (isActive) {
                _sessionStatus.value = torrentEngine.getSessionManagerStatus()
                delay(2000)
            }
        }
    }
}
