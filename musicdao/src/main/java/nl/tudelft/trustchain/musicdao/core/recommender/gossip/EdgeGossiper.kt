package nl.tudelft.trustchain.musicdao.core.recommender.gossip

import android.widget.Toast
import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.eva.PeerBusyException
import nl.tudelft.ipv8.messaging.eva.TimeoutException
import nl.tudelft.ipv8.messaging.eva.TransferType
import nl.tudelft.trustchain.common.freedomOfComputing.AppPayload
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.Pair

/**
 * Gossips edges in the network to keep graphs synced
 */

lateinit var edgeGossiperInstance: EdgeGossiper
const val GOSSIP_DELAY: Long = 10000
const val DOWNLOAD_DELAY: Long = 20000

const val TORRENT_ATTEMPTS_THRESHOLD = 1
const val EVA_RETRIES = 10
private val logger = KotlinLogging.logger {}

class EdgeGossiper(
    private val sessionManager: SessionManager,
    private val recommenderCommunityBase: RecommenderCommunityBase,
    private val toastingEnabled: Boolean
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        fun getInstance(
            sessionManager: SessionManager,
            recommenderCommunity: RecommenderCommunityBase,
            toastingEnabled: Boolean = true
        ): EdgeGossiper {
            if (!::edgeGossiperInstance.isInitialized) {
                edgeGossiperInstance = EdgeGossiper(sessionManager, recommenderCommunity, toastingEnabled)
            }
            return edgeGossiperInstance
        }
    }

    fun start() {
        scope.launch {
            //gossip node to node edges
        }
        scope.launch {
            // gossip node to song edges
        }
    }
}
