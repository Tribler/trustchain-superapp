package nl.tudelft.trustchain.frost

import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.messaging.Packet
import java.io.File
import java.io.PrintWriter
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
    private fun initiateWalkingModel() {
        try {
            Log.i("FROST", "Initiate random walk")
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
        Log.i("FROST", "Walking model is de-packaged")

        // TODO handle encryption

        Log.i("FROST", "Key fragment saved $keyShare")

        saveKeyShare(keyShare)
    }


    private fun saveKeyShare(keyShare: String){
        var file = File("key_share.txt")
        file.writeText(keyShare)
    }


    @ExperimentalUnsignedTypes
    fun distributeKey(
        keyShare: String,
        ttl: UInt = 2u,
        originPublicKey: ByteArray = myPeer.publicKey.keyToBin()
    ): Int {
        var count = 0
        for (peer in getPeers()) {
            if(peer == myPeer){
                saveKeyShare(keyShare)
            }
            else{
                val packet = serializePacket(
                    MessageId.ID,
                    KeyPacketMessage(originPublicKey, ttl, keyShare)
                )
                Log.i("FROST", "Sending key fragment to ${peer.address}")
                send(peer, packet)
                count += 1
            }
        }
        return count
    }


    object MessageId {
        val ID: Int
            get() { return 0 }
    }

}
