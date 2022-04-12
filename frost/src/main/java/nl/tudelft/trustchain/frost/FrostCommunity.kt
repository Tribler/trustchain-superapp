package nl.tudelft.trustchain.frost

import android.content.Context
import android.util.Log
import bitcoin.*
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import kotlin.random.Random

val THRESHOLD = 3

class FrostCommunity(private val context: Context,
                     private var signers: MutableList<FrostSigner>,
                     private var keyShares: MutableList<ByteArray>,
                     private var secret: FrostSecret
): Community(){
    override val serviceId = "98c1f6342f30528ada9647197f0503d48db9c2fb"

    init {
        messageHandlers[MessageId.SEND_KEY] = ::onDistributeShares
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

    private fun onDistributeShares(packet: Packet) {
        Log.i("FROST", "Key packet received $packet")
        val (peer, payload) = packet.getDecryptedAuthPayload(KeyPacketMessage.Deserializer, myPeer.key as PrivateKey)
        val keyShare = payload.keyShare
        val i = getIndexOfSigner(peer.address.ip)
        this.keyShares.add(i, keyShare)

        val ackBuffer = readFile(this.context,"received_shares.txt")
        val newBuffer = "$ackBuffer \n ${peer.address}"
        writeToFile(this.context, "received_shares.txt", newBuffer)

        ackKey(peer, keyShare)

        Log.i("FROST", "Key fragment acked $keyShare")

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
        if (!signerInList(myPeer.address.toString())) {
            Log.i("FROST", "${myPeer.address} creating own signer")
            // add self as signer
            val signer = FrostSigner(threshold)
            val secret = FrostSecret()
            NativeSecp256k1.generateKey(secret, signer)
            this.secret = secret
            signer.ip = myPeer.address.toString()
            this.signers.add(signer)

            for (peer in getPeers()) {
                if (peer != myPeer) {
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
                    if (sendBack){
                        Log.i("FROST", "${myPeer.address} sending signer to ${peer.address}")
                        send(peer, packet)
                    }

                    else if (!signerInList(peer.address.toString())) {
                        Log.i("FROST", "${myPeer.address} sending signer to ${peer.address}")
                        send(peer, packet)
                    }
                }
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

    private fun getIndexOfSigner(ip: String): Int{
        var i = -1
        for (signer in signers){
            i++
            if(ip == signer.ip){
                break
            }
        }
        return i
    }

    private fun getPeerFromIP(ip: String): Peer?{
        var peer: Peer? = null
        for (p in getPeers()){
            if(p.address.toString() == ip)
                 peer = p
        }
        return peer
    }

    private fun getPublicKeysFromSigners(): Array<ByteArray>{
        var keys = mutableListOf<ByteArray>()
        for (signer in signers){
            keys.add(signer.pubkey)
        }
        return keys.toTypedArray()
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
        signer.ip = peer.address.toString()

        Log.i("FROST", "${myPeer.address} received signer from ${peer.address}")

        if(!signerInList(peer.address.toString())){
            Log.i("FROST", "${myPeer.address} signer was unknown, adding to list")
            this.signers.add(signer)
            createSigner(THRESHOLD, true)
        }
        else
            Log.i("FROST", "${myPeer.address} signer was known")
            createSigner(THRESHOLD, false)
    }


    fun distributeShares(
        keyShare: ByteArray,
        peers: List<Peer>? = null
    ){
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
                Log.i("FROST", "${myPeer.address} sending key share to ${peer.address}")
                send(peer, packet)
            }
        }
    }

    fun createShares(){
        val i = getIndexOfSigner(myPeer.address.toString())
        this.signers.sort()
        val res = NativeSecp256k1.sendShares(getPublicKeysFromSigners(), this.secret, this.signers[i])
        var j = 0
        for(share in res){
            val peerAdress = this.signers[j].ip
            val list = mutableListOf<Peer>()
            list.add(getPeerFromIP(peerAdress)!!)
            distributeShares(share, list)
            j++
        }
    }

    fun receiveFrost(){
        val i = getIndexOfSigner(myPeer.address.toString())
        NativeSecp256k1.receiveFrost(arrayOf(this.keyShares[i]), this.secret, this.signers.toTypedArray(), i)
    }

    object MessageId {
        const val SEND_KEY = 0
        const val ACK_KEY = 1
        const val SEND_SIGNER = 2
    }

    class Factory(
        private val context: Context,
        private val signers: MutableList<FrostSigner>,
        private val keyShares: MutableList<ByteArray>,
        private val secret: FrostSecret
    ) : Overlay.Factory<FrostCommunity>(FrostCommunity::class.java) {
        override fun create(): FrostCommunity {
            return FrostCommunity(context, signers, keyShares, secret)
        }
    }
}

