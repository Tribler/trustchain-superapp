package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.detoks.db.OurTransactionStore

data class DeToksTransaction(val recipient: Peer, val toks: List<Token>)

class DeToksTransactionEngine (
    val tokenStore: OurTransactionStore,
    val context: Context,
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler
) : TrustChainCommunity(settings, database, crawler) {

    override val serviceId = "12313685c1912a191279f8248fc8db5899c5df6a"

    private val SINGLE_BLOCK = "SingleBlock"
    private val GROUPED_BLOCK = "GroupedBlock"

    private val LOGTAG = "DeToksTransactionEngine"

    init {
        // setup block listeners
        addListener(SINGLE_BLOCK, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                if (block.isProposal) {
                    Log.d(LOGTAG, "Received SINGLE proposal block")
                    receiveSingleTokenProposal(block)
                } else if (block.isAgreement) {
                    Log.d(LOGTAG, "Received SINGLE agreement block")
                    receiveSingleTokenAgreement(block)
                }
            }
        })

        addListener(GROUPED_BLOCK, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                if (block.isProposal) {
                    Log.d(LOGTAG, "Received GROUPED proposal block")
                    receiveGroupedTokenProposal(block)
                } else if (block.isAgreement) {
                    Log.d(LOGTAG, "Received GROUPED agreement block")
                    receiveGroupedTokenAgreement(block)
                }
            }
        })
    }

    // Single token, no grouping
    fun sendTokenSingle(tok: Token, peer: Peer): TrustChainBlock {
        Log.d(LOGTAG, "Sending token")
        val transaction = mapOf("token" to tok.toString())

        return createProposalBlock(
            SINGLE_BLOCK,
            transaction,
            peer.publicKey.keyToBin()
        )
    }

    private fun receiveSingleTokenProposal(block: TrustChainBlock) {
        val token = block.transaction["token"] as String
        val (uid, pk) = token.split(",")
        println(pk)

        // todo insert token in database
        Log.d(LOGTAG, "Saving received $token to database")

        val transaction = mapOf("tokenSent" to uid)
        createAgreementBlock(block, transaction)
    }

    private fun receiveSingleTokenAgreement(block: TrustChainBlock) {
        val tokenId = block.transaction["tokenSent"] as String
        // todo insert token in database
        Log.d(LOGTAG, "Removing spent $tokenId from database")
    }

    // Grouped tokens
    fun sendTokenGrouped(transactions: List<List<Token>>, peer: Peer, groupSize: Int): TrustChainBlock {

        val groups = transactions.chunked(groupSize)

        for (group in groups) {
            val transactionList: MutableList<Map<String, Any>> = mutableListOf()
        }


        Log.d(LOGTAG, "Sending grouped transactions: $transactions")

//        return createProposalBlock(
//            GROUPED_BLOCK,
//            trustChainTransaction,
//            peer.publicKey.keyToBin()
//        )
    }

    private fun receiveGroupedTokenProposal(block: TrustChainBlock) {

        val transactions = block.transaction["transactions"] as List<*>

        Log.d(LOGTAG, "Received transactions: ${transactions}")
    }

    private fun receiveGroupedTokenAgreement(block: TrustChainBlock) {
        val tokenId = block.transaction["tokenSent"] as String
        // todo insert token in database
        Log.d(LOGTAG, "Removing spent $tokenId from database")
    }


    class Factory(
        private val store: OurTransactionStore,
        private val context: Context,
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<DeToksTransactionEngine>(DeToksTransactionEngine::class.java) {
        override fun create(): DeToksTransactionEngine {
            return DeToksTransactionEngine(store, context, settings, database, crawler)
        }
    }
}

