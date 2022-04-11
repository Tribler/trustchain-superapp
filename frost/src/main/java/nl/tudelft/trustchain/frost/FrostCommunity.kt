package nl.tudelft.trustchain.frost

import android.content.Context
import android.util.Log
import bitcoin.FrostCache
import bitcoin.FrostSecret
import bitcoin.FrostSession
import bitcoin.FrostSigner
import bitcoin.NativeSecp256k1
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import kotlin.random.Random

val THRESHOLD = 3

class FrostCommunity(private val context: Context,
                     private val signers: MutableList<FrostSigner>,
                     private val cache: FrostCache,
                     private val session: FrostSession
): Community(){
    override val serviceId = "98c1f6342f30528ada9647197f0503d48db9c2fb"

    init {
        messageHandlers[MessageId.SEND_KEY] = ::onDistributeKey
        messageHandlers[MessageId.ACK_KEY] = ::onAckKey
        messageHandlers[MessageId.SEND_SIGNER] = ::onCreateSigner
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
        val ackBuffer = readFile(this.context,"acks.txt")
        val newBuffer = "$ackBuffer \n ${peer.address}"
        writeToFile(this.context, "acks.txt", newBuffer)
    }

    private fun saveKeyShare(keyShare: ByteArray){
        writeToFile(this.context, "key_share.txt", keyShare.toString())
        Log.i("FROST", "File written ${myPeer.address}")
    }

    private fun getKeyShare(): ByteArray? {
        val key = readFile(this.context, "key_share.txt")
        Log.i("FROST", "LOADED: $key ${myPeer.address}")
        return key?.toByteArray()
    }

    fun createSigner(threshold: Int, sendBack: Boolean) {
        // add self as signer
        val signer = FrostSigner(threshold)
        val secret = FrostSecret()
        NativeSecp256k1.generateKey(secret, signer)
        signer.ip = myPeer.address.toString()
        if(!signerInList(myPeer.address.toString())) {
            this.signers.add(signer)
        }

        for (peer in getPeers()) {
            val packet = serializePacket(
                MessageId.SEND_SIGNER,
                FrostSignerPacket(
                    signer.pubkey,
                    signer.pubnonce,
                    signer.partial_sig,
                    signer.vss_hash,
                    signer.pubcoeff
                ),
                encrypt = true,
                sign = true,
                recipient = peer
            )
            Log.i("FROST", "${myPeer.address} sending signer to ${peer.address}")
            if(sendBack)
                send(peer, packet)
            else if(!signerInList(peer.address.toString())) {
                send(peer, packet)
            }
        }
    }

    private fun signerInList(ip: String): Boolean {
        for (signer in signers){
            if(ip == signer.ip){
                return true
            }
        }
        return false
    }


    fun onCreateSigner(packet: Packet) {
        val (peer, payload) = packet.getDecryptedAuthPayload(FrostSignerPacket.Deserializer, myPeer.key as PrivateKey)
        val signer = FrostSigner(
            payload.pubkey,
            payload.pubnonce,
            payload.partial_sig,
            payload.vss_hash,
            payload.pubcoeff
        )
        if(!signerInList(peer.address.toString()))
            createSigner(THRESHOLD, true)
        else createSigner(THRESHOLD, false)
        this.signers.add(signer)
        Log.i("FROST", "${myPeer.address} received signer from ${peer.address}")
        createSigner(THRESHOLD, true)
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

    object MessageId {
        const val SEND_KEY = 0
        const val ACK_KEY = 1
        const val SEND_SIGNER = 2
    }

    class Factory(
        private val context: Context,
        private val signers: MutableList<FrostSigner>,
        private val cache: FrostCache,
        private val session: FrostSession
    ) : Overlay.Factory<FrostCommunity>(FrostCommunity::class.java) {
        override fun create(): FrostCommunity {
            return FrostCommunity(context, signers, cache, session)
        }
    }
}

