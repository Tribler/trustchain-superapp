package nl.tudelft.trustchain.detoks_engine.trustchain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.AgreementBlockBuilder
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.ProposalBlockBuilder
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPairPayload
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


class GroupedAdapter(
    private val trustChainCommunity: TrustChainCommunity,
    private val maxGroupBy: Int = 4,
    private val flushIntervalMillis: Long = 50,
    private val resendTimeoutMillis: Long = 1000,
    private val resendLimit: Int = 0
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val logger = KotlinLogging.logger("GroupedTokenTransactionAdapter")
    private val myPublicKey = trustChainCommunity.myPeer.publicKey.keyToBin()

    private var recvProposalCallback: ((TrustChainTransaction, Boolean) -> Unit)? = null
    private var recvAgreementCallback: ((TrustChainTransaction, Boolean) -> Unit)? = null


    val maxTransactionSize by lazy {
        val proposal = ProposalBlockBuilder(
            trustChainCommunity.myPeer,
            trustChainCommunity.database,
            TOKEN_BLOCK_TYPE,
            emptyMap<Any?, Any?>(),
            myPublicKey
        ).sign()
        val agreement = AgreementBlockBuilder(
            trustChainCommunity.myPeer,
            trustChainCommunity.database,
            proposal,
            proposal.transaction
        ).sign()
        val payload = HalfBlockPairPayload.fromHalfBlocks(proposal, agreement)
        val packet = trustChainCommunity.serializePacket(messageId = 0, payload = payload, sign = false)
        return@lazy ((UdpEndpoint.UDP_PAYLOAD_LIMIT - packet.size) / 2).also { logger.info { "Max transaction size: $it" } }
    }


    init {
        trustChainCommunity.addListener(TOKEN_BLOCK_TYPE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                logger.debug { "Receive block: ${block.summarize()}" }
                val isFromMe = block.publicKey.contentEquals(myPublicKey)
                val isForMe = block.linkPublicKey.contentEquals(myPublicKey)
                if (block.isProposal && !isFromMe) {
                    block.transaction.asGrouped()?.forEach { recvProposalCallback?.invoke(it, isForMe) }
                    agreeTransaction(block)
                } else if (block.isAgreement && !isFromMe) {
                    block.transaction.asGrouped()?.forEach { recvAgreementCallback?.invoke(it, isForMe) }
                    if (isForMe)
                        transmittingBlocks.remove(block.linkedBlockId)?.cancel()
                }
            }
        })
        startSender()
    }


    fun onTransactionProposalReceived(handler: ((transaction: TrustChainTransaction, isForMe: Boolean) -> Unit)) {
        recvProposalCallback = handler
    }


    fun onTransactionAgreementReceived(handler: ((transaction: TrustChainTransaction, isForMe: Boolean) -> Unit)) {
        recvAgreementCallback = handler
    }


    fun getPeers(): List<Peer> = trustChainCommunity.getPeers()


    fun proposeTransaction(transaction: TrustChainTransaction, peer: Peer) {
        val buf = bufferedTransactions.getOrPut(peer) { ConcurrentLinkedQueue<TrustChainTransaction>() }
        buf.offer(transaction)
        logger.debug { "Buffer transaction proposal: $transaction to $peer" }
    }


    private fun agreeTransaction(proposal: TrustChainBlock) = scope.launch {
        // Inefficient. See database io in BlockBuilder.sign() and TrustChainCommunity.validateAndPersistBlock()
        val block = synchronized(trustChainCommunity.database) {
            return@synchronized trustChainCommunity.createAgreementBlock(proposal, proposal.transaction)
        }
        logger.debug { "Send agreement block: ${block.summarize()}" }
    }


    private val bufferedTransactions = ConcurrentHashMap<Peer, ConcurrentLinkedQueue<TrustChainTransaction>>()
    private val transmittingBlocks = ConcurrentHashMap<String, Job>()

    private fun startSender() = scope.launch {
        while (isActive) {
            delay(flushIntervalMillis)
            for ((peer, buf) in bufferedTransactions) {
                launch propose@{
                    val grouped = buf.pollN(maxGroupBy)
                    if (grouped.isEmpty())
                        return@propose

                    // Inefficient. See above
                    val block = synchronized(trustChainCommunity.database) {
                        return@synchronized trustChainCommunity.createProposalBlock(
                            TOKEN_BLOCK_TYPE,
                            mapOf("grouped" to grouped),
                            peer.publicKey.keyToBin()
                        )
                    }
                    logger.debug { "Send proposal block: ${block.summarize()}" }

                    if (resendLimit > 0) {
                        val job = launch {
                            repeat(resendLimit) {
                                delay(resendTimeoutMillis)
                                if (isActive) {
                                    trustChainCommunity.sendBlock(block, peer)
                                    trustChainCommunity.sendBlock(block)
                                    logger.debug { "Resend proposal block: ${block.summarize()}" }
                                } else
                                    return@repeat
                            }
                        }
                        val prevJob = transmittingBlocks.replace(block.blockId, job)
                        if (prevJob != null)
                            throw RuntimeException("Duplicated job ${block.blockId}")
                    }
                }
            }
        }
    }


    companion object {
        const val TOKEN_BLOCK_TYPE = "token_block_grouped"
    }
}


private fun <E> ConcurrentLinkedQueue<E>.pollN(n: Int): List<E> {
    val list = mutableListOf<E>()
    repeat(n) {
        val ele = this.poll() ?: return@repeat
        list.add(ele)
    }
    return list
}


private fun TrustChainTransaction.asGrouped(key: String = "grouped"): List<TrustChainTransaction>? {
    val list = (this[key] as? List<*>) ?: return null
    if (list.any { (it as? TrustChainTransaction) == null })
        return null
    @Suppress("UNCHECKED_CAST")
    return list as? List<TrustChainTransaction>
}


private fun TrustChainBlock.summarize(): String = StringBuilder().let {
    it.append("TrustChainBlock||: ")
    it.append("fromID: ${publicKey.toString().substring(0, 8)}.$sequenceNumber, ")
    it.append("toID: ${linkPublicKey.toString().substring(0, 8)}.$linkSequenceNumber, ")
    it.append("isProposal/Agreement: $isProposal/$isAgreement, ")
    it.append("transaction: $transaction ")
    it.append(":||")
    return@let it.toString()
}
