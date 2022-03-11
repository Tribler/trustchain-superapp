package com.example.musicdao

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.example.musicdao.core.database.CacheDatabase
import com.example.musicdao.core.database.GsonParser
import com.example.musicdao.core.database.parser.Converters
import com.example.musicdao.core.ipv8.MusicCommunity
import com.example.musicdao.core.usecases.DownloadFinishUseCase
import com.example.musicdao.core.wallet.WalletConfig
import com.example.musicdao.core.wallet.WalletConfig.Companion.DEFAULT_FAUCET_ENDPOINT
import com.example.musicdao.core.wallet.WalletConfig.Companion.DEFAULT_FILE_PREFIX
import com.example.musicdao.core.wallet.WalletConfig.Companion.DEFAULT_NETWORK_PARAMS
import com.example.musicdao.core.wallet.WalletConfig.Companion.DEFAULT_REGTEST_BOOTSTRAP_IP
import com.example.musicdao.core.wallet.WalletConfig.Companion.DEFAULT_REGTEST_BOOTSTRAP_PORT
import com.example.musicdao.core.wallet.WalletService
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.SessionParams
import com.frostwire.jlibtorrent.SettingsPack
import com.frostwire.jlibtorrent.swig.settings_pack
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import nl.tudelft.ipv8.android.IPv8Android
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class HiltModules {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext applicationContext: Context): CacheDatabase {
        return Room.databaseBuilder(
            applicationContext,
            CacheDatabase::class.java, "musicdao-database"
        ).fallbackToDestructiveMigration()
            .addTypeConverter(Converters(GsonParser(Gson())))
            .build()
    }

    @Provides
    @Singleton
    fun createSessionParams(@ApplicationContext applicationContext: Context): SessionManager {
        val settingsPack = SettingsPack()

        val port =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getString("musicdao_port", "10181")
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

        return SessionManager().apply {
            start(SessionParams(settingsPack))
        }
    }

    @Provides
    @Singleton
    fun musicCommunity(): MusicCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("MusicCommunity is not configured")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    @Singleton
    fun path(@ApplicationContext applicationContext: Context): CachePath {
        return CachePath(applicationContext)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    @Singleton
    fun downloadFinishUseCase(
        database: CacheDatabase,
        cachePath: CachePath
    ): DownloadFinishUseCase {
        return DownloadFinishUseCase(database = database, cachePath = cachePath)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    @Singleton
    fun provideWalletService(
        @ApplicationContext applicationContext: Context,
    ): WalletService {
        return WalletService(
            WalletConfig(
                networkParams = DEFAULT_NETWORK_PARAMS,
                filePrefix = DEFAULT_FILE_PREFIX,
                cacheDir = Paths.get("${applicationContext.cacheDir}").toFile(),
                regtestFaucetEndPoint = DEFAULT_FAUCET_ENDPOINT,
                regtestBootstrapIp = DEFAULT_REGTEST_BOOTSTRAP_IP,
                regtestBootstrapPort = DEFAULT_REGTEST_BOOTSTRAP_PORT
            )
        )
    }
}

class CachePath(val applicationContext: Context) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun getPath(): Path? {
        return Paths.get("${applicationContext.cacheDir}")
    }
}
