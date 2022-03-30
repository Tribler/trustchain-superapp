package nl.tudelft.trustchain.eurotoken.community

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import mu.KotlinLogging
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.eurotoken.db.TrustStore
import nl.tudelft.trustchain.eurotoken.ui.settings.DefaultGateway

private val logger = KotlinLogging.logger {}

class EuroTokenCommunity(
    store: GatewayStore,
    trustStore : TrustStore
) : Community() {
    override val serviceId = "f0eb36102436bd55c7a3cdca93dcaefb08df0750"

    private lateinit var transactionRepository: TransactionRepository

    private lateinit var myTrustStore : TrustStore


    init {
        messageHandlers[MessageId.ROLLBACK_REQUEST] = ::onRollbackRequestPacket
        messageHandlers[MessageId.ATTACHMENT] = ::onLastAddressPacket
        if (store.getPreferred().isEmpty()) {
            DefaultGateway.addGateway(store)
        }

        myTrustStore = trustStore
    }

    @JvmName("setTransactionRepository1")
    fun setTransactionRepository(transactionRepositoryLocal: TransactionRepository) {
        transactionRepository = transactionRepositoryLocal
    }

    private fun onRollbackRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(RollbackRequestPayload.Deserializer)
        onRollbackRequest(peer, payload)
    }

    private fun onLastAddressPacket(packet: Packet) {
        val (peer, payload) = packet.getDecryptedAuthPayload(
            MessagePayload.Deserializer, myPeer.key as PrivateKey
        )

        val addresses : List<String> = payload.toString().split(",")

        for (i in addresses.indices) {
            myTrustStore.incrementTrust(defaultCryptoProvider.keyFromPublicBin(addresses[i].toByteArray()))
        }

        logger.debug { "RECEIVED EVA MESSAGE" + payload }
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
        const val ATTACHMENT = 4
    }

    class Factory(
        private val store: GatewayStore,
        private val trustStore : TrustStore
    ) : Overlay.Factory<EuroTokenCommunity>(EuroTokenCommunity::class.java) {
        override fun create(): EuroTokenCommunity {
            return EuroTokenCommunity(store, trustStore)
        }
    }

    fun sendAddressesOfLastTransactions(peer: Peer, num: Int = 50) {
        // Get all addresses of the last [num] incoming transactions
        val addresses : List<PublicKey> = transactionRepository.getTransactions(50).map{
            transaction: Transaction ->
            transaction.sender
        }

        val payload = MessagePayload(addresses.joinToString(separator = ","))
        logger.debug { "-> $payload" }

        val packet = serializePacket(
            MessageId.ATTACHMENT,
            payload,
            encrypt = true,
            recipient = peer
        )

        // Send the list of addresses to the peer using EVA
        if (evaProtocolEnabled) evaSendBinary(
            peer,
            EVAId.EVA_LAST_ADDRESSES,
            payload.id,
            packet
        ) else send(peer, packet)
    }



    /**
     * Every community initializes a different version of the EVA protocol (if enabled).
     * To distinguish the incoming packets/requests an ID must be used to hold/let through the
     * EVA related packets.
     */
    object EVAId {
        const val EVA_LAST_ADDRESSES = "eva_last_addresses"
    }
}
