package nl.tudelft.trustchain.frost

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.Packet
import java.util.*
import kotlin.random.Random

@ExperimentalUnsignedTypes
class FrostCommunity: Community(){
    override val serviceId = "98c1f6342f30528ada9647197f0503d48db9c2fb"

    init {
        messageHandlers[MessageId.ID] = ::onDistributeKey
    }
    override fun load() {
        super.load()

        if (Random.nextInt(0, 1) == 0) initiateWalkingModel()
    }
    /**
     * Load / create walking models (feature based and collaborative filtering)
     */
    fun initiateWalkingModel() {
        try {
            Log.i("FROST", "Initiate random walk")
//            val keyFragment = "Hello"
//            onBroadcast(keyFragment=keyFragment)
        } catch (e: Exception) {
            Log.i("FROST", "Random walk failed")
            e.printStackTrace()
        }
    }


    @ExperimentalUnsignedTypes
    fun onDistributeKey(packet: Packet) {
        Log.i("FROST", "Key packet received")
        val (_, payload) = packet.getAuthPayload(KeyPacketMessage)

        val keyShare = payload.keyShare
        // TODO handle encryption
        Log.i("FROST", "Walking model is de-packaged")

        Log.i("FROST", "Key fragment saved $keyShare")
        //save key fragment
    }

    @ExperimentalUnsignedTypes
    fun distributeKey(
        keyShare: String,
        ttl: UInt = 2u,
        originPublicKey: ByteArray = myPeer.publicKey.keyToBin()
    ): Int {
        var count = 0
        for (peer in getPeers().filter { it != myPeer }) {
            val packet = serializePacket(
                MessageId.ID,
                KeyPacketMessage(originPublicKey, ttl, keyShare)
            )
            Log.i("FROST", "Sending key fragment to ${peer.address}")
            send(peer, packet)
            count += 1
        }
        return count
    }


    object MessageId {
        val ID: Int
            get() { return 0 }
    }

}
