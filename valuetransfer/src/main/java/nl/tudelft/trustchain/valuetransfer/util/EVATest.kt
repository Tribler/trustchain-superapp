package nl.tudelft.trustchain.valuetransfer.util

import android.util.Log
import kotlinx.coroutines.*
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import java.util.*

class EVATest(
    val activity: ValueTransferMainActivity,
    private val peerPublicKey: PublicKey
) {
    private var peerChatCommunity: PeerChatCommunity = activity.getCommunity()!!
    private var trustChainCommunity: TrustChainCommunity = activity.getCommunity()!!
    private lateinit var peer: Peer

    init {
        val queue: Queue<EVATestSettings> = LinkedList()

//        val windowSizes = listOf(16, 32, 48, 64, 80, 96, 112, 128)
//        val blockSizes = listOf(800, 900, 1000, 1100, 1200)
//        val iteration = 0..4
//        val fileSizes = listOf(5, 10)
//        val retransmitInterval = 3

        val windowSizes = listOf(80)
        val blockSizes = listOf(1200)
        val iteration = 2..3
        val fileSizes = listOf(250)
        val retransmitInterval = 4

        for (windowSize in windowSizes) {
            for (blockSize in blockSizes) {
                for (it in iteration) {
                    for (fileSize in fileSizes) {
                        queue.add(EVATestSettings(fileSize, windowSize, blockSize, retransmitInterval, it))
                    }
                }
            }
        }

        peerChatCommunity.evaProtocol!!.apply {
            debugMode = true
        }

        peerChatCommunity.setEVAOnSendCompleteCallback { _, _, _ ->
            Log.d("EVAPROTOCOL", "EVA SEND COMPLETE")

            if (queue.isEmpty()) {
                peerChatCommunity.evaProtocol!!.apply {
                    debugMode = false
                }
            } else {
                queue.poll()?.let {
                    start(peer, it)
                }
            }
        }

        peerChatCommunity.setEVAOnReceiveCompleteCallback { _, _, id, _ ->
            Log.d("EVAPROTOCOL", "EVA RECEIVE COMPLETE with id '$id'")
        }

        if (trustChainCommunity.myPeer.publicKey != peerPublicKey) {
            CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    val peerTest = trustChainCommunity.getPeers()
                        .firstOrNull { it.publicKey == peerPublicKey }

                    if (peerTest != null && peerTest.isConnected()) {
                        peer = trustChainCommunity.getPeers()
                            .first { it.publicKey == peerPublicKey }

                        queue.poll()?.let {
                            start(peer, it)
                        }

                        break
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun start(peer: Peer, settings: EVATestSettings) {
        val sizeInMB = settings.sizeInMB
        val windowSize = settings.windowSize
        val blockSize = settings.blockSize
        val retransmitInterval = settings.retransmitInterval
        val iteration = settings.iteration

        peerChatCommunity.evaProtocol!!.apply {
            windowSizeInBlocks = windowSize
            this.blockSize = blockSize
            retransmitIntervalInSec = retransmitInterval
        }

        val data = ByteArray(sizeInMB * 1024 * 1024)
        val id = "$sizeInMB-$windowSize-$blockSize-$retransmitInterval-$iteration"

        Log.d("EVAPROTOCOL", "EVA SEND WITH WINDOWSIZE: ${peerChatCommunity.evaProtocol!!.windowSizeInBlocks}")
        Log.d("EVAPROTOCOL", "EVA SEND WITH BLOCKSIZE: ${peerChatCommunity.evaProtocol!!.blockSize}")
        Log.d("EVAPROTOCOL", "EVA SEND WITH RETRANSMIT: ${peerChatCommunity.evaProtocol!!.retransmitIntervalInSec}")
        Log.d("EVAPROTOCOL", "EVA SEND WITH ID: $id")

        peerChatCommunity.evaSendBinary(
            peer,
            Community.EVAId.EVA_PEERCHAT_ATTACHMENT,
            id,
            data
        )
    }

    data class EVATestSettings(
        val sizeInMB: Int,
        val windowSize: Int,
        val blockSize: Int,
        val retransmitInterval: Int,
        val iteration: Int
    )
}
