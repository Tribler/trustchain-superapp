package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPayload
import nl.tudelft.ipv8.messaging.Address
import nl.tudelft.ipv8.messaging.Packet
import java.util.*


open class TransactionEngine (override val serviceId: String): Community() {

    companion object {
        const val MESSAGE_TRANSACTION_ID = 2
    }

    /**
     * Broadcasts a token transaction to all known peers.
     * @param amount the amount of tokens to send
     * @param senderMid the member ID of the peer that sends the amount of tokens
     * @param recipientMid the member ID of the peer that will receive the amount of tokens
     */
    fun broadcastTokenTransaction(amount: Int,
                                  senderMid: String,
                                  recipientMid: String) {
        for (peer in getPeers()) {
            sendTokenTransaction(amount, senderMid, recipientMid, peer.address)
        }
    }

    /**
     * Sends a token transaction to a peer.
     * @param amount the amount of tokens to be sent
     * @param senderMid the member ID of the peer that sends the amount of tokens
     * @param recipientMid the member ID of the peer that will receive the amount of tokens
     * @param receiverAddress the address of the peer that receives the transaction. That
     * peer may be different than the recipient of the amount of tokens in the transaction
     */
    fun sendTokenTransaction(amount: Int,
                             senderMid: String,
                             recipientMid: String,
                             receiverAddress: Address) {
        val packet = serializePacket(
            MESSAGE_TRANSACTION_ID,
            TransactionMessage(amount, senderMid, recipientMid)
        )
        send(receiverAddress, packet)
    }

    fun sendTransaction(block: TrustChainBlock,
                        peer: Peer,
                        encrypt: Boolean = false,
                        msgID: Int) {
        Log.d("TransactionEngine",
              "Creating transaction to peer with public key: " + peer.key.pub().toString())
        sendBlockToRecipient(peer, block, encrypt, msgID)
    }

    private fun sendBlockToRecipient(peer: Peer,
                                     block: TrustChainBlock,
                                     encrypt: Boolean,
                                     msgID: Int) {
        val payload = HalfBlockPayload.fromHalfBlock(block)

        val data = if (encrypt) {
            serializePacket(
                msgID, payload, false, encrypt = true, recipient = peer, peer = myPeer
            )
        } else {
            serializePacket(msgID, payload, false, peer = myPeer)
        }
        send(peer, data)
    }

    override fun onPacket(packet: Packet) {
        val sourceAddress = packet.source
        val data = packet.data

        val probablePeer = network.getVerifiedByAddress(sourceAddress)
        if (probablePeer != null) {
            probablePeer.lastResponse = Date()
        }

        val packetPrefix = data.copyOfRange(0, prefix.size)
        if (!packetPrefix.contentEquals(prefix)) {
            // logger.debug("prefix not matching")
            return
        }

        val msgId = data[prefix.size].toUByte().toInt()

        val handler = messageHandlers[msgId]

        if (handler != null) {
            try {
                handler(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.d("TransactionEngine",
                "Received unknown message $msgId from $sourceAddress" )
        }
    }

    class Factory(private val serviceId: String) : Overlay.Factory<TransactionEngine>(TransactionEngine::class.java) {
        override fun create(): TransactionEngine {
            return TransactionEngine(serviceId)
        }
    }
}
