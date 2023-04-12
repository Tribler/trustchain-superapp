package nl.tudelft.trustchain.detoks_engine.trustchain

import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.trustchain.detoks_engine.manage_tokens.Transaction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

class CommunityAdapter(
    private val trustChainCommunity: TrustChainCommunity,
    private val maxGroupBy: Int = 4,
    private val flushIntervalMillis: Long = 50,
    private val resendTimeoutMillis: Long = 1000,
    private val resendLimit: Int = 0
) {
    private val trustChainHelper: TrustChainHelper = TrustChainHelper(trustChainCommunity)
    val myPublicKey = trustChainHelper.getMyPublicKey()
    val logger = KotlinLogging.logger("TokenTransaction")
    private val scope = CoroutineScope(Dispatchers.IO)
    lateinit var recvTransactionHandler: ((transaction: Transaction) -> Unit)
    private var blockPointer = -1
    private var tokenInBlockPointer = -1
    private var tokenCount = 0


    private val bufferedTransactions = ConcurrentHashMap<Peer, ConcurrentLinkedQueue<String>>()
    private val transmittingBlocks = ConcurrentHashMap<String, Job>()

    init {
        trustChainCommunity.addListener(TOKEN_BLOCK_TYPE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                handleBlock(block)
            }
        })

        // Find last sent token
        var lastSentToken: String? = null
        var block = trustChainCommunity.database.getLatest(myPublicKey, TOKEN_BLOCK_TYPE)
        while(block != null) {
            // Proposal with my public key is a sent token block
            if (block.isProposal && !block.isSelfSigned) {
                lastSentToken = Transaction.fromTrustChainTransactionObject(block.transaction).tokens.last()
                break
            }
            block = trustChainCommunity.database.getBlockBefore(block, TOKEN_BLOCK_TYPE)
        }


        // Find all first relevant received token
        block = trustChainCommunity.database.getLatest(myPublicKey, TOKEN_BLOCK_TYPE)
        while (block != null) {
            // Agreement with my public key AS LINK KEY is a rec token block
            if (block.isAgreement) {
                blockPointer = block.sequenceNumber.toInt()
                tokenInBlockPointer = 0
                if (lastSentToken != null && Transaction.fromTrustChainTransactionObject(block.transaction).tokens.contains(lastSentToken)) {
                    tokenInBlockPointer = Transaction.fromTrustChainTransactionObject(block.transaction).tokens.indexOf(lastSentToken)
                    tokenCount += Transaction.fromTrustChainTransactionObject(block.transaction).tokens.size - tokenInBlockPointer
                    popToken()
                    break
                }
                tokenCount += Transaction.fromTrustChainTransactionObject(block.transaction).tokens.size
            }
            block = trustChainCommunity.database.getBlockBefore(block, TOKEN_BLOCK_TYPE)
        }
        startSender()
    }

    private fun handleBlock(block: TrustChainBlock) {
        logger.debug("Block token received: ${block.transaction}, is proposal: ${block.isProposal}, is agreement: ${block.isAgreement}, PK til 8: ${block.publicKey.toString().substring(0, 8)}, is self signed ${block.isSelfSigned}")

        // A proposal block for me (so i receive tokens), action is to agree
        if (block.isProposal && (block.isSelfSigned || !block.publicKey.contentEquals(myPublicKey))) {
            agreeTransaction(block)
        }

        // Agreement block signed by me means i accept the tokens sent to me
        else if (block.isAgreement && block.publicKey.contentEquals(myPublicKey)) {
            recvTransactionHandler(Transaction.fromTrustChainTransactionObject(block.transaction))
            val trans = Transaction.fromTrustChainTransactionObject(block.transaction)
            tokenCount += trans.tokens.size
            if (blockPointer == -1) {
                blockPointer = block.sequenceNumber.toInt()
                tokenInBlockPointer = 0
            }
        }

        // Other party accepted my proposal
        else if (block.isAgreement && block.publicKey.contentEquals(myPublicKey)) {
            transmittingBlocks.remove(block.linkedBlockId)?.cancel()
        }
    }

    fun sendTokens(amount: Int, peer: Peer) {
        if (tokenCount > 0) {
            val nToSend = min(amount, tokenCount)
            val tokens = mutableListOf<String>()
            repeat(nToSend) {
                tokens.add(popToken()!!)
            }
            proposeTransaction(Transaction(tokens), peer)
        }

    }
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
                            GroupedAdapter.TOKEN_BLOCK_TYPE,
                            Transaction(grouped).toTrustChainTransaction(),
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

    private fun popToken() : String? {
        if (tokenCount > 0) {
            // Fetch token at pointer
            val block = trustChainCommunity.database.get(myPublicKey, blockPointer.toUInt())!!
            val transaction = Transaction.fromTrustChainTransactionObject(block.transaction)
            val token = transaction.tokens[tokenInBlockPointer]

            // Decrement token count
            tokenCount--

            // If last token set pointers to -1 and return
            if (tokenCount == 0) {
                blockPointer = -1
                tokenInBlockPointer = -1
                return token
            }

            // I at end of block search for next one
            if (tokenInBlockPointer == transaction.tokens.size - 1) {
                tokenInBlockPointer = 0
                var nextBlock = trustChainCommunity.database.getBlockAfter(block, TOKEN_BLOCK_TYPE)

                while (!nextBlock!!.isAgreement) {
                    nextBlock = trustChainCommunity.database.getBlockAfter(nextBlock, TOKEN_BLOCK_TYPE)
                }
                blockPointer = nextBlock.sequenceNumber.toInt()

            // else only move in block pointer
            } else {
                tokenInBlockPointer++
            }
            return token
        }
        return null
    }

    private fun proposeTransaction(transaction: Transaction, peer: Peer) {
        logger.debug("Proposing transaction: ${transaction.transactionId} with first token: ${transaction.tokens[0]}")
        val buf = bufferedTransactions.getOrPut(peer) {ConcurrentLinkedQueue<String>()}
        for (token: String in transaction.tokens) {
            buf.offer(token)
        }
//        trustChainCommunity.createProposalBlock(TOKEN_BLOCK_TYPE, transaction.toTrustChainTransaction(), peer.publicKey.keyToBin())
    }

    fun injectTokens(tokens: List<String>) {
        logger.debug{"Starting token injection for $tokens"}
        val transaction = Transaction(tokens)
        trustChainCommunity.createProposalBlock(TOKEN_BLOCK_TYPE, transaction.toTrustChainTransaction(), myPublicKey)
    }

    fun setReceiveTransactionHandler(handler: ((transaction: Transaction) -> Unit)) {
        recvTransactionHandler = handler
    }

    private fun agreeTransaction(proposal: TrustChainBlock) {
        val trustTransaction = proposal.transaction
        val block = synchronized(trustChainCommunity.database) {
            return@synchronized trustChainHelper.createAgreementBlock(proposal, trustTransaction)
        }
        logger.debug("Agreeing to transactionblock: ${block.summarize()}")
    }

    fun getPeers(): List<Peer> {
        return trustChainCommunity.getPeers()
    }

    fun getTokens(): List<String> {
        val tokens = mutableListOf<String>()
        if (tokenCount == 0) return tokens

        var currentBlock: TrustChainBlock? = trustChainCommunity.database.get(myPublicKey, blockPointer.toUInt())
        while (tokens.size < tokenCount) {
            currentBlock!!
            if (currentBlock.isAgreement) {
                tokens.addAll(Transaction.fromTrustChainTransactionObject(currentBlock.transaction).tokens)
            }
            currentBlock = trustChainCommunity.database.getBlockAfter(currentBlock, TOKEN_BLOCK_TYPE)
        }
        return tokens
    }

    companion object {
        const val TOKEN_BLOCK_TYPE = "token_block"
    }

    private fun <E> ConcurrentLinkedQueue<E>.pollN(n: Int): List<E> {
        val list = mutableListOf<E>()
        repeat(n) {
            val ele = this.poll() ?: return@repeat
            list.add(ele)
        }
        return list
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
}
