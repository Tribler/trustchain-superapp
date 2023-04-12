package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.detoks.db.TokenStore
import java.util.UUID

class DeToksTransactionEngine (
    val tokenStore: TokenStore,
    val context: Context,
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler
) : TrustChainCommunity(settings, database, crawler) {

    override val serviceId = "12313685c1912a191279f8248fc8db5899c5df6a"

    private val SINGLE_BLOCK = "SingleBlock"
    private val GROUPED_BLOCK = "GroupedBlock"

    private val LOGTAG = "DeToksTransactionEngine"

    private lateinit var selectedPeer : Peer
    private lateinit var selfPeer : Peer
    private var sendingToSelf = true

    // This value is 1000 because we will be doing 1000 transactions benchmarks
    private var tokenIDIncrementer = 1000

    init {
        // setup block listeners
        addListener(SINGLE_BLOCK, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d(LOGTAG, "")
                if (sendingToSelf || AndroidCryptoProvider.keyFromPublicBin(block.linkPublicKey) == selfPeer.publicKey) {
                    if (block.isProposal) {
                        Log.d(LOGTAG, "Received SINGLE proposal block")
                        receiveSingleTokenProposal(block)
                    } else if (block.isAgreement) {
                        Log.d(LOGTAG, "Received SINGLE agreement block")
                        receiveSingleTokenAgreement(block)
                    }
                }
            }
        })

        addListener(GROUPED_BLOCK, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                // LinkPublicKey = the addressee of the block
                if (sendingToSelf || AndroidCryptoProvider.keyFromPublicBin(block.linkPublicKey) == selfPeer.publicKey) {
                    if (block.isProposal) {
                        Log.d(LOGTAG, "Received GROUPED proposal block")
                        receiveGroupedTokenProposal(block)
                    } else if (block.isAgreement) {
                        Log.d(LOGTAG, "Received GROUPED agreement block")
                        receiveGroupedTokenAgreement(block)
                    }
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
        val (uid, pk, intId) = token.split(",")
        println(pk)

        // Add token to personal database
        // Add token to personal database
        // If sending to self ->  Increment the ID to avoid duplicate ID errors.
        var newID = uid
        Log.d(LOGTAG, "Sending to self: $sendingToSelf")
        if (sendingToSelf) {
            newID = UUID.randomUUID().toString()
        }
        tokenStore.addToken((newID), pk, intId.toLong())
        Log.d(LOGTAG, "Saving received $token to database")

        val transaction = mapOf("tokenSent" to uid)
        createAgreementBlock(block, transaction)
    }

    private fun receiveSingleTokenAgreement(block: TrustChainBlock) {
        val tokenId = block.transaction["tokenSent"] as String

        // Remove token from personal database
        tokenStore.removeTokenByID(tokenId)

        Log.d(LOGTAG, "Removing spent $tokenId from database")
    }

    // Grouped tokens
    fun sendTokenGrouped(transactions: List<List<Token>>, peer: Peer): TrustChainBlock {

        val groupedTransactions = mutableListOf<List<String>>()
        for (transaction in transactions) {
            val tokenList: MutableList<String> = mutableListOf()
            for (token in transaction) {
                tokenList.add(token.toString())
            }
            groupedTransactions.add(tokenList)
        }

        val transaction = mapOf("transactions" to groupedTransactions)

        Log.d(LOGTAG, "Sending grouped transactions: $transactions")

        return createProposalBlock(
            GROUPED_BLOCK,
            transaction,
            peer.publicKey.keyToBin()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun receiveGroupedTokenProposal(block: TrustChainBlock) {
        val transactions = block.transaction["transactions"] as List<List<String>>
        Log.d(LOGTAG, "Received grouped transaction: ${transactions}")
        //extract tokens, create grouped agreement block
        val grouped_agreement_uids = mutableListOf<List<String>>()
        for (transaction in transactions) {
            val tokenList: MutableList<String> = mutableListOf()
            for (token in transaction) {
                val (uid, _, intId) = token.split(",")
                tokenList.add(uid)

                // Add token to personal database
                // If sending to self ->  Increment the ID to avoid duplicate ID errors.
                var newID = uid
                Log.d(LOGTAG, "Sending to self: $sendingToSelf")
                if (sendingToSelf) {
                    newID = UUID.randomUUID().toString()
                }
                tokenStore.addToken((newID), block.publicKey.toString(), intId.toLong())
                Log.d(LOGTAG, "Saving received $token to database")
            }
            grouped_agreement_uids.add(tokenList.toList())
        }
        val transaction = mapOf("tokensSent" to grouped_agreement_uids.toList())
        createAgreementBlock(block, transaction)
    }
    @Suppress("UNCHECKED_CAST")
    private fun receiveGroupedTokenAgreement(block: TrustChainBlock) {
        Log.d(LOGTAG, "Received grouped agreement block")
        val tokenIds = block.transaction["tokensSent"] as List<List<String>>
        Log.d(LOGTAG, "${tokenIds}}")
        val tokensToRemove= mutableListOf<String>()
        for (tokens in tokenIds ) {
                for(token_id in tokens) {
                    // Remove token from personal database
                    tokenStore.removeTokenByID(token_id)
                    tokensToRemove.add(token_id)
                }
        }
        Log.d(LOGTAG, "Removing spent tokens $tokensToRemove from database")
    }

    fun addPeer(peer: Peer, self : Boolean = false) {
        selectedPeer = peer
        sendingToSelf = self
        Log.d(LOGTAG, "Selected peer: ${selectedPeer.publicKey}")
    }

    fun initializePeers(self: Peer) {
        selectedPeer = self
        selfPeer = self
    }

    fun getSelectedPeer() : Peer {
        return selectedPeer
    }

    fun getSelfPeer() : Peer {
        return selfPeer
    }

    fun isPeerSelected() : Boolean {
        return ::selectedPeer.isInitialized
    }



    class Factory(
        private val store: TokenStore,
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

