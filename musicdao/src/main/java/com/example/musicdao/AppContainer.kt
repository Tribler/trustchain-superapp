package com.example.musicdao

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import com.example.musicdao.ipv8.MusicCommunity
import com.example.musicdao.net.ContentSeeder
import com.example.musicdao.repositories.ReleaseRepository
import com.example.musicdao.repositories.SwarmHealthRepository
import com.example.musicdao.repositories.TorrentRepository
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.SessionParams
import com.frostwire.jlibtorrent.SettingsPack


object AppContainer {
    lateinit var sessionManager: SessionManager
    lateinit var contentSeeder: ContentSeeder

    lateinit var currentCallback: (List<Uri>) -> Unit

    //    lateinit var swarmHealthMap = mutableMapOf<Sha1Hash, SwarmHealth>()
    lateinit var swarmHealthRepository: SwarmHealthRepository
    lateinit var releaseRepository: ReleaseRepository
    lateinit var activity: MusicActivity

    //    lateinit var torrentRepository: TorrentRepository
    lateinit var releaseTorrentRepository: TorrentRepository

    fun provide(
        applicationContext: Context,
        musicCommunity: MusicCommunity,
        _activity: MusicActivity
    ) {
        activity = _activity
        sessionManager = SessionManager().apply {
            start(createSessionParams(applicationContext))
        }
        contentSeeder =
            ContentSeeder(applicationContext.cacheDir, sessionManager).apply {
                start()
            }
        swarmHealthRepository = SwarmHealthRepository(
            sessionManager, contentSeeder, musicCommunity
        )
        releaseRepository = ReleaseRepository(musicCommunity)
//        torrentRepository = TorrentRepository(sessionManager, applicationContext.cacheDir)
        releaseTorrentRepository = TorrentRepository(
            sessionManager,
            applicationContext.cacheDir,
        )
    }

    private fun createSessionParams(applicationContext: Context): SessionParams {
        val settingsPack = SettingsPack()

        val port =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getString("musicdao_port", "")
                ?.toIntOrNull()
        if (port != null) {
            val interfaceFormat = "0.0.0.0:%1\$d,[::]:%1\$d"
            settingsPack.listenInterfaces(String.format(interfaceFormat, port))
        }

        return SessionParams(settingsPack)
    }
}
