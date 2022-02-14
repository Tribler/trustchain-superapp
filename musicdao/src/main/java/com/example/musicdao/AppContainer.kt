package com.example.musicdao

import TorrentCache
import TorrentEngine
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.example.musicdao.core.cache.CacheDatabase
import com.example.musicdao.core.cache.GsonParser
import com.example.musicdao.core.cache.parser.Converters
import com.example.musicdao.core.usecases.*
import com.example.musicdao.core.usecases.torrents.DownloadIntentUseCase
import com.example.musicdao.core.usecases.torrents.GetAllActiveTorrentsUseCase
import com.example.musicdao.core.usecases.torrents.GetTorrentStatusFlowUseCase
import com.example.musicdao.core.ipv8.MusicCommunity
import com.example.musicdao.core.repositories.ReleaseRepository
import com.example.musicdao.core.repositories.SwarmHealthRepository
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.SessionParams
import com.frostwire.jlibtorrent.SettingsPack
import com.frostwire.jlibtorrent.swig.settings_pack
import com.google.gson.Gson
import java.nio.file.Paths

object AppContainer {
    lateinit var getAllActiveTorrentsUseCase: GetAllActiveTorrentsUseCase
    lateinit var searchUseCase: SearchUseCase
    lateinit var getTorrentStatusFlowUseCase: GetTorrentStatusFlowUseCase
    lateinit var downloadIntentuseCase: DownloadIntentUseCase
    lateinit var getReleaseUseCase: GetRelease
    lateinit var createReleaseUseCase: CreateReleaseUseCase
    lateinit var sessionManager: SessionManager

    lateinit var torrentEngine: TorrentEngine
    lateinit var torrentCache: TorrentCache

    lateinit var currentCallback: (List<Uri>) -> Unit

    //    lateinit var swarmHealthMap = mutableMapOf<Sha1Hash, SwarmHealth>()
    lateinit var swarmHealthRepository: SwarmHealthRepository
    lateinit var releaseRepository: ReleaseRepository
    lateinit var activity: MusicActivity

    lateinit var database: CacheDatabase
    lateinit var getAllReleases: GetAllReleases

    @RequiresApi(Build.VERSION_CODES.O)
    fun provide(
        applicationContext: Context,
        musicCommunity: MusicCommunity,
        _activity: MusicActivity
    ) {
        activity = _activity
        sessionManager = SessionManager().apply {
            start(createSessionParams(applicationContext))
        }

        database = Room.databaseBuilder(
            applicationContext,
            CacheDatabase::class.java, "musicdao-database"
        ).fallbackToDestructiveMigration()
            .addTypeConverter(Converters(GsonParser(Gson())))
            .build()

        releaseRepository = ReleaseRepository(musicCommunity, database = database)

        val downloadFinishUseCase = DownloadFinishUseCase(database)::invoke
        torrentEngine = TorrentEngine(sessionManager, downloadFinishUseCase::invoke)
        torrentCache = TorrentCache(torrentEngine, Paths.get("${applicationContext.cacheDir}"))


        createReleaseUseCase = CreateReleaseUseCase(
            releaseRepository,
            torrentCache
        )
        getReleaseUseCase = GetRelease(database)

        downloadIntentuseCase = DownloadIntentUseCase(torrentCache)
        getTorrentStatusFlowUseCase = GetTorrentStatusFlowUseCase(torrentCache)
        searchUseCase = SearchUseCase(releaseRepository, getReleaseUseCase)
        getAllActiveTorrentsUseCase =
            GetAllActiveTorrentsUseCase(getTorrentStatusFlowUseCase, torrentEngine)
        getAllReleases = GetAllReleases(database)

    }

    private fun createSessionParams(applicationContext: Context): SessionParams {
        val settingsPack = SettingsPack()

        val port =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getString("musicdao_port", "10021")
                ?.toIntOrNull()
        if (port != null) {
            val interfaceFormat = "0.0.0.0:%1\$d,[::]:%1\$d"
            settingsPack.listenInterfaces(String.format(interfaceFormat, port))
        }

        settingsPack.setBoolean(
            settings_pack.bool_types.announce_to_all_trackers.swigValue(),
            true
        )
        settingsPack.setBoolean(settings_pack.bool_types.announce_to_all_tiers.swigValue(), true)
        settingsPack.setBoolean(
            settings_pack.bool_types.listen_system_port_fallback.swigValue(),
            false
        )
        settingsPack.setBoolean(settings_pack.bool_types.enable_upnp.swigValue(), false)
        settingsPack.setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(), false)
        settingsPack.setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(), false)

        // Disable Local Discovery
        settingsPack.setBoolean(settings_pack.bool_types.enable_lsd.swigValue(), false)

        // Increase the limits of auto-managed (queuing) torrents
        settingsPack.setInteger(settings_pack.int_types.active_downloads.swigValue(), 1000)
        settingsPack.setInteger(settings_pack.int_types.active_seeds.swigValue(), 1000)
        settingsPack.setInteger(settings_pack.int_types.active_checking.swigValue(), 1000)
        settingsPack.setInteger(settings_pack.int_types.active_limit.swigValue(), 1000)

        return SessionParams(settingsPack)
    }
}
