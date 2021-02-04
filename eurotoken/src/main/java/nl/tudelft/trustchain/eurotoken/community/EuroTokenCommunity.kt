package nl.tudelft.trustchain.eurotoken.community

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes

class EuroTokenCommunity(
    private val context: Context
) : Community() {
    override val serviceId = "f0eb36102436bd55c7a3cdca93dcaefb08df0750"

    init {
        //messageHandlers[MessageId.MESSAGE] = ::onMessagePacket
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun connectToGateway(public_key: String, ip: String, port: Int, payment_id: String) {
        val key = defaultCryptoProvider.keyFromPublicBin(public_key.hexToBytes())
        val address = IPv4Address(ip, port)
        val peer = Peer(key, address)

        val payload = MessagePayload( payment_id )

        val packet = serializePacket(
            MessageId.GATEWAY_CONNECT,
            payload
        )

        send(peer, packet)
    }

    object MessageId {
        const val GATEWAY_CONNECT = 1
    }

    class Factory(
        private val context: Context
    ) : Overlay.Factory<EuroTokenCommunity>(EuroTokenCommunity::class.java) {
        override fun create(): EuroTokenCommunity {
            return EuroTokenCommunity(context)
        }
    }
}
