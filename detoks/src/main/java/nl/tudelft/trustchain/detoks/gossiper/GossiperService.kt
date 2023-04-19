package nl.tudelft.trustchain.detoks.gossiper

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.DeToksCommunity
import kotlin.system.exitProcess

class GossiperService : Service() {
    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Add all gossiper services in the list to have them started by this service.
     */
    private val gossiperList: List<Gossiper> = listOf(
        BootGossiper(1000L, 4),
        NetworkSizeGossiper(30000L, 4, 1),
        TorrentGossiper(4000L, 4, 4, this),
        WatchTimeGossiper(4000L, 4, 4, this)
    )


    inner class LocalBinder : Binder() {
        fun getService(): GossiperService = this@GossiperService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        gossiperList.forEach { it.startGossip(scope) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()

        exitProcess(0)
    }
}
