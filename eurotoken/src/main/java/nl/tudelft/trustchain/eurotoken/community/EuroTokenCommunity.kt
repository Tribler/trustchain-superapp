package nl.tudelft.trustchain.eurotoken.community

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import mu.KotlinLogging
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.eurotoken.DEMO_MODE_ENABLED_PREF
import nl.tudelft.trustchain.eurotoken.EUROTOKEN_PREFERENCES
import nl.tudelft.trustchain.eurotoken.db.TrustStore
import nl.tudelft.trustchain.eurotoken.ui.settings.DefaultGateway

private val logger = KotlinLogging.logger {}

class EuroTokenCommunity(
    store: GatewayStore,
    trustStore : TrustStore,
    context: Context,
) : Community() {
    override val serviceId = "f0eb36102436bd55c7a3cdca93dcaefb08df0750"

    private lateinit var transactionRepository: TransactionRepository

    private var myTrustStore : TrustStore

    private var myContext : Context


    init {
        messageHandlers[MessageId.ROLLBACK_REQUEST] = ::onRollbackRequestPacket
        messageHandlers[MessageId.ATTACHMENT] = ::onLastAddressPacket
        if (store.getPreferred().isEmpty()) {
            DefaultGateway.addGateway(store)
        }

        myTrustStore = trustStore
        myContext = context
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
        logger.debug { "RECEIVED EVA MESSAGE PT 0" }
        val (_, payload) = packet.getDecryptedAuthPayload(
            TransactionsPayload.Deserializer, myPeer.key as PrivateKey
        )

        val addresses : List<ByteArray> = String(payload.data).split(",").map { it.toByteArray() }
        for (i in addresses.indices) {
            myTrustStore.incrementTrust(addresses[i])
        }

        logger.debug { "DONE ADDING" }
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
        private val trustStore : TrustStore,
        private val context : Context,
    ) : Overlay.Factory<EuroTokenCommunity>(EuroTokenCommunity::class.java) {
        override fun create(): EuroTokenCommunity {
            return EuroTokenCommunity(store, trustStore, context)
        }
    }

    fun generatePublicKey() : String {
        return defaultCryptoProvider.generateKey().pub().keyToBin().toHex()
    }

    fun generatePublicKeys(length: Int) : List<String> {
        val publicKeys = mutableListOf<String>()
        for (i in 0 until length) {
            publicKeys.add(generatePublicKey())
        }

        logger.debug { "-> Generated ${publicKeys?.size} public keys" }
        logger.debug { "-> Public keys: ${publicKeys?.joinToString(", ")}" }
        return publicKeys
    }


    fun sendAddressesOfLastTransactions(peer: Peer, num: Int = 50) {
        val pref = myContext.getSharedPreferences(EUROTOKEN_PREFERENCES, Context.MODE_PRIVATE)
        val demoModeEnabled = pref.getBoolean(DEMO_MODE_ENABLED_PREF, false)

        logger.debug{"ADDRESS CHECK " + myPeer.publicKey.keyToBin().toHex()}

        var addresses : ArrayList<String> = ArrayList()
        // Add own public key to list of addresses.
        addresses.add(myPeer.publicKey.keyToBin().toHex())
        if (demoModeEnabled) {
            // Generate [num] addresses if in demo mode
            addresses.addAll(generatePublicKeys(num))
        } else {
            // Get all addresses of the last [num] incoming transactions
            addresses.addAll(transactionRepository.getTransactions(num).map { transaction: Transaction ->
                transaction.sender.toString()
            })
        }

        val payload = TransactionsPayload("1", addresses.joinToString(separator = ",").toByteArray())
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
