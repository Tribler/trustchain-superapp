package nl.tudelft.trustchain.frost

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import kotlin.random.Random

@ExperimentalUnsignedTypes
class FrostCommunity(private val context: Context): Community(){
    override val serviceId = "98c1f6342f30528ada9647197f0503d48db9c2fb"

    init {
        messageHandlers[MessageId.ID] = ::onDistributeKey
        messageHandlers[MessageId.ACK] = ::onAckKey
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
        val (peer, payload) = packet.getDecryptedAuthPayload(KeyPacketMessage, myPeer.key as PrivateKey)
        val keyShare = payload.keyShare
        Log.i("FROST", "Walking model is de-packaged")

        saveKeyShare(keyShare)

        ackKey(peer, keyShare)

        Log.i("FROST", "Key fragment acked $keyShare")

        getKeyShare()

    }

    private fun ackKey(peer: Peer, keyShare: ByteArray){
        val ack = serializePacket(
            MessageId.ACK,
            Ack(keyShare),
            encrypt = true,
            recipient = peer
        )
        Log.i("FROST", "Sending key ack ${myPeer.address}")
        send(peer, ack)
    }

    private fun onAckKey(packet: Packet){
        val (peer, payload) = packet.getDecryptedAuthPayload(Ack, myPeer.key as PrivateKey)
        Log.i("FROST", "${peer.address} acked key  ${payload.keyShare}")
    }

    private fun saveKeyShare(keyShare: ByteArray){
        this.context.openFileOutput("key_share.txt", Context.MODE_PRIVATE).use {
            it?.write(keyShare)
            Log.i("FROST", "File written ${myPeer.address}")
        }
    }

    private fun getKeyShare(){
        this.context.openFileInput("key_share.txt").use { stream ->
            val text = stream?.bufferedReader().use {
                it?.readText()
            }
            Log.i("FROST", "LOADED: $text ${myPeer.address}")
        }
    }


    @ExperimentalUnsignedTypes
    fun distributeKey(
        keyShare: ByteArray,
        peers: List<Peer>? = null
    ): Int {
        var count = 0
        var peerList = peers
        if(peerList == null){
            peerList = getPeers()
        }

        for (peer in peerList) {
            if(peer == myPeer){
                saveKeyShare(keyShare)
            }
            else{
                val packet = serializePacket(
                    MessageId.ID,
                    KeyPacketMessage(keyShare),
                    encrypt = true,
                    recipient = peer
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
        val ACK: Int
            get() { return 1 }
    }

    class Factory(
        private val context: Context
    ) : Overlay.Factory<FrostCommunity>(FrostCommunity::class.java) {
        override fun create(): FrostCommunity {
            return FrostCommunity(context)
        }
    }
}
