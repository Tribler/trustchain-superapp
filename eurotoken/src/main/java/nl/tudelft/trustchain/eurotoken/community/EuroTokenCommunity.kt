package nl.tudelft.trustchain.eurotoken.community

import android.os.Build
import androidx.annotation.RequiresApi
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.eurotoken.ui.settings.DefaultGateway

class EuroTokenCommunity(
    store: GatewayStore
) : Community() {
    override val serviceId = "f0eb36102436bd55c7a3cdca93dcaefb08df0750"

    private lateinit var transactionRepository: TransactionRepository

    init {
        messageHandlers[MessageId.ROLLBACK_REQUEST] = ::onRollbackRequestPacket
        if (store.getPreferred().isEmpty()) {
            DefaultGateway.addGateway(store)
        }
    }

    @JvmName("setTransactionRepository1")
    fun setTransactionRepository(transactionRepositoryLocal: TransactionRepository) {
        transactionRepository = transactionRepositoryLocal
    }

    private fun onRollbackRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(RollbackRequestPayload.Deserializer)
        onRollbackRequest(peer, payload)
    }

    private fun onRollbackRequest(peer: Peer, payload: RollbackRequestPayload) {
        transactionRepository.attemptRollback(peer, payload.transactionHash)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun connectToGateway(public_key: String, ip: String, port: Int, payment_id: String) {
        val key = defaultCryptoProvider.keyFromPublicBin(public_key.hexToBytes())
        val address = IPv4Address(ip, port)
        val peer = Peer(key, address)

        val payload = MessagePayload(payment_id)

        val packet = serializePacket(
            MessageId.GATEWAY_CONNECT,
            payload
        )

        send(peer, packet)
    }

    fun requestRollback(transactionHash: ByteArray, peer: Peer) {
        val payload = RollbackRequestPayload(transactionHash)

        val packet = serializePacket(
            MessageId.ROLLBACK_REQUEST,
            payload
        )

        send(peer, packet)
    }

    object MessageId {
        const val GATEWAY_CONNECT = 1
        const val ROLLBACK_REQUEST = 1
    }

    class Factory(
        private val store: GatewayStore
    ) : Overlay.Factory<EuroTokenCommunity>(EuroTokenCommunity::class.java) {
        override fun create(): EuroTokenCommunity {
            return EuroTokenCommunity(store)
        }
    }
}
