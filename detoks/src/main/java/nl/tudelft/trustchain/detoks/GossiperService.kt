package nl.tudelft.trustchain.detoks

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import kotlin.system.exitProcess

class GossiperService : Service() {
    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.IO)

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
        scope.launch {
            gossipTorrents()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()

        exitProcess(0)
    }

    private suspend fun gossipTorrents() {
        while (scope.isActive) {
            val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!

            val randomPeer = pickRandomPeer(deToksCommunity)
            if(randomPeer != null)
                deToksCommunity.gossipWith(randomPeer)

            delay(4000)
        }
    }

    private fun pickRandomPeer(deToksCommunity: DeToksCommunity): Peer? {
        val peers = deToksCommunity.getPeers()
        if (peers.isEmpty()) return null
        return peers.random()
    }
}
