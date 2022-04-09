package nl.tudelft.trustchain.frost

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import kotlin.random.Random

class FrostCommunity(private val context: Context): Community(){
    override val serviceId = "98c1f6342f30528ada9647197f0503d48db9c2fb"

    init {
        messageHandlers[MessageId.SEND_KEY] = ::onDistributeKey
        messageHandlers[MessageId.ACK_KEY] = ::onAckKey
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

    private fun onDistributeKey(packet: Packet) {
        Log.i("FROST", "Key packet received $packet")
        val (peer, payload) = packet.getDecryptedAuthPayload(KeyPacketMessage.Deserializer, myPeer.key as PrivateKey)
        val keyShare = payload.keyShare
        saveKeyShare(keyShare)

        ackKey(peer, keyShare)

        Log.i("FROST", "Key fragment acked $keyShare")

//        getKeyShare()

    }

    private fun ackKey(peer: Peer, keyShare: ByteArray){
        val ack = serializePacket(
            MessageId.ACK_KEY,
            Ack(keyShare),
            encrypt = true,
            sign = true,
            recipient = peer
        )
        Log.i("FROST", " ${myPeer.address} sending key ack to ${peer.address}")
        send(peer, ack)
    }

    private fun onAckKey(packet: Packet){
        val (peer, payload) = packet.getDecryptedAuthPayload(Ack.Deserializer, myPeer.key as PrivateKey)
        Log.i("FROST", "${myPeer.address} acked key ${payload.keyShare} from ${peer.address}")
        val ackBuffer = readFile("acks.txt")
        val newBuffer = "$ackBuffer \n ${peer.address}"

        writeToFile("acks.txt", newBuffer)
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
                    MessageId.SEND_KEY,
                    KeyPacketMessage(keyShare),
                    encrypt = true,
                    sign = true,
                    recipient = peer
                )
                Log.i("FROST", "${myPeer.address} sending key fragment to ${peer.address}")
                send(peer, packet)
                count += 1
            }
        }
        return count
    }

    private fun writeToFile(filePath: String, text: String){
        this.context.openFileOutput(filePath, Context.MODE_PRIVATE).use {
            it?.write(text.toByteArray())
            Log.i("FROST", "Write: $text to $filePath")
        }
    }

    private fun readFile(filePath: String): String? {
        this.context.openFileInput(filePath).use { stream ->
            val text = stream?.bufferedReader().use {
                it?.readText()
            }
            Log.i("FROST", "Read: $text from $filePath")
            return text
        }
    }


    object MessageId {
        const val SEND_KEY = 0
        const val ACK_KEY = 1
    }

    class Factory(
        private val context: Context
    ) : Overlay.Factory<FrostCommunity>(FrostCommunity::class.java) {
        override fun create(): FrostCommunity {
            return FrostCommunity(context)
        }
    }
}
