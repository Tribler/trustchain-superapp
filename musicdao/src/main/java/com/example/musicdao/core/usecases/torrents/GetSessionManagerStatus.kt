package com.example.musicdao.core.usecases.torrents

import com.frostwire.jlibtorrent.SessionManager
import javax.inject.Inject

class GetSessionManagerStatus @Inject constructor(val sessionManager: SessionManager) {

    operator fun invoke(): SessionManagerStatus {
        return SessionManagerStatus(
            interfaces = sessionManager.listenInterfaces(),
            dhtNodes = sessionManager.dhtNodes(),
            uploadRate = sessionManager.uploadRate(),
            downloadRate = sessionManager.downloadRate()
        )
    }
}

data class SessionManagerStatus(
    val interfaces: String,
    val dhtNodes: Long,
    val uploadRate: Long,
    val downloadRate: Long
)
