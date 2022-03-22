package nl.tudelft.trustchain.literaturedao.ipv8
import android.util.Log
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.Packet

class LiteratureCommunity(
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler)  {
    override val serviceId: String = "0215eded9b27e6905a6d3fd02cc64d363c03a026"

    object MessageID {
        const val DEBUG_MESSAGE = 1
        const val SEARCH_QUERY = 2
    }

    class Factory(
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<LiteratureCommunity>(LiteratureCommunity::class.java) {
        override fun create(): LiteratureCommunity {
            return LiteratureCommunity(settings, database, crawler)
        }
    }


    private fun onDebugMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(DebugMessage.Deserializer)
        Log.i("Lit Dao", peer.mid + ": " + payload.message)
    }

    private fun onQueryMessage(packet: Packet) {
        // handle query message here
    }

    init {
        messageHandlers[MessageID.DEBUG_MESSAGE] = ::onDebugMessage
        messageHandlers[MessageID.SEARCH_QUERY] = ::onQueryMessage
    }


}
