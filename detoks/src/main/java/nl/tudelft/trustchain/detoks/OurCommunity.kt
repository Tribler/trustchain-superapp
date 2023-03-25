package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.trustchain.detoks.db.OurTransactionStore

class OurCommunity(
    store: OurTransactionStore,
    context: Context,
) : Community() {

    private var myTokenStore : OurTransactionStore

    /**
     * The context used to access the shared preferences.
     */
    private var myContext : Context

    override val serviceId = "12313685c1912a191279f8248fc8db5899c5df6a"

    private val MESSAGE_ID = 1
    init {
        messageHandlers[MESSAGE_ID] = ::onMessage

        myTokenStore = store
        myContext = context
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(Token.Deserializer)
        Log.d("OurCommunity", peer.mid + ": " + payload.unique_id)
    }


    class Factory(
        private val store: OurTransactionStore,
        private val context : Context,
    ) : Overlay.Factory<OurCommunity>(OurCommunity::class.java) {
        override fun create(): OurCommunity {
            return OurCommunity(store, context)
        }
    }
}

