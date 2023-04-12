package nl.tudelft.trustchain.detoks

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.detoks.db.TokenStore

data class DeToksTransaction(val tokens: List<Token>, val recipient: Peer)

class DeToksTransactionEngine(
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

    private lateinit var selectedPeer: Peer
    private lateinit var selfPeer: Peer
    private var sendingToSelf = true

    private val enableTokenDB = false

    // This value is 1000 because we will be doing 1000 transactions benchmarks
    private var tokenIDIncrementer = 1000

    // Single token, no grouping
    fun sendTransactionSingle(transaction: DeToksTransaction): TrustChainBlock {
        Log.d(LOGTAG, "Sending transaction")
        val trustChainTransaction = mapOf("tokens" to transaction.tokens.map { it.toString() })

        return createProposalBlockMine(
            SINGLE_BLOCK,
            trustChainTransaction,
            transaction.recipient.publicKey.keyToBin()
        )
    }

//    fun sendTokensSingle(toks: List<Token>, peer: Peer) {
//
//        // must be done sequentially
//        val peerPk = peer.publicKey.keyToBin()
//        for (tok in toks) {
//            val transaction = mapOf("token" to tok.toString())
//            val block = ProposalBlockBuilder(myPeer, database, SINGLE_BLOCK, transaction, peerPk).sign()
//            onBlockCreated(block)
//            sendBlock(block)
//        }
//    }

    private fun receiveSingleTransactionProposal(block: TrustChainBlock) {

        val tokens = block.transaction["tokens"] as List<*>

        val tokenUids = tokens.map {tok ->
            tok as String
            val (uid, pk) = tok.split(",")

            // Add token to personal database
            // Add token to personal database
            // If sending to self ->  Increment the ID to avoid duplicate ID errors.
            var newID = uid.toInt()
            Log.d(LOGTAG, "Sending to self: $sendingToSelf")
            if (sendingToSelf) {
                newID += tokenIDIncrementer
            }
            if (enableTokenDB) {
                tokenStore.addToken(newID.toString(), pk)
            }
            Log.d(LOGTAG, "Saving received $tok to database")


            uid
        }

        val transaction = mapOf("tokensSent" to tokenUids)
        createAgreementBlockMine(block, transaction)
    }

    private fun receiveSingleTransactionAgreement(block: TrustChainBlock) {
        val tokenIds = block.transaction["tokensSent"] as List<*>

        for (tokenId in tokenIds) {
            tokenId as String

            if (enableTokenDB) {
                // Remove token from personal database
                tokenStore.removeTokenByID(tokenId)
            }
            Log.d(LOGTAG, "Removing spent $tokenId from database")

        }
    }

    // Grouped tokens
    fun sendTokenGrouped(transactions: List<List<Token>>, peer: Peer): TrustChainBlock {

        val transactionsList = transactions.map { transaction ->
            transaction.map { tok -> tok.toString() }
        }

        val transaction = mapOf("transactions" to transactionsList)

        Log.d(LOGTAG, "Sending grouped transactions: $transactions")

        return createProposalBlock(
            GROUPED_BLOCK,
            transaction,
            peer.publicKey.keyToBin()
        )
    }

    fun sendTokenGrouped(transactions: List<DeToksTransaction>): TrustChainBlock {

        val transactionsList = transactions.map { transaction ->
            Pair(transaction.tokens.map { it.toString() }, transaction.recipient)
        }

        val transaction = mapOf("transactions" to transactionsList)

        Log.d(LOGTAG, "Sending grouped transactions: $transactions")

        return createProposalBlock(
            GROUPED_BLOCK,
            transaction,
            transactions[0].recipient.publicKey.keyToBin()
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
                val (uid, _) = token.split(",")
                tokenList.add(uid)

                // Add token to personal database
                // If sending to self ->  Increment the ID to avoid duplicate ID errors.
                var newID = uid.toInt()
                Log.d(LOGTAG, "Sending to self: $sendingToSelf")
                if (sendingToSelf) {
                    newID += tokenIDIncrementer
                }
                if (enableTokenDB) {
                    tokenStore.addToken((newID).toString(), block.publicKey.toString())
                }
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
        val tokensToRemove = mutableListOf<String>()
        for (tokens in tokenIds) {
            for (token_id in tokens) {
                // Remove token from personal database
                tokenStore.removeTokenByID(token_id)
                tokensToRemove.add(token_id)
            }
        }
        Log.d(LOGTAG, "Removing spent tokens $tokensToRemove from database")
    }

    fun addPeer(peer: Peer, self: Boolean = false) {
        selectedPeer = peer
        sendingToSelf = self
        Log.d(LOGTAG, "Selected peer: ${selectedPeer.publicKey}")
    }

    fun initializePeers(self: Peer) {
        selectedPeer = self
        selfPeer = self
    }

    fun getSelectedPeer(): Peer {
        return selectedPeer
    }

    fun getSelfPeer(): Peer {
        return selfPeer
    }

    fun isPeerSelected(): Boolean {
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

    fun createProposalBlockMine(
        blockType: String,
        transaction: TrustChainTransaction,
        publicKey: ByteArray
    ): TrustChainBlock {
        val block = ProposalBlockBuilder(myPeer, database, blockType, transaction, publicKey).sign()

        onBlockCreated(block)

        sendBlock(block)

        return block
    }


    /**
     * Creates an agreement block that will be linked to the proposal block.
     *
     * @param link The proposal block which the agreement block will be linked to.
     * @param transaction A map with supplementary information concerning the transaction.
     */
    fun createAgreementBlockMine(
        link: TrustChainBlock,
        transaction: TrustChainTransaction
    ): TrustChainBlock {
        assert(
            link.linkPublicKey.contentEquals(myPeer.publicKey.keyToBin()) ||
                link.linkPublicKey.contentEquals(ANY_COUNTERPARTY_PK)
        ) {
            "Cannot counter sign block not addressed to self"
        }

        assert(link.linkSequenceNumber == UNKNOWN_SEQ) {
            "Cannot counter sign block that is not a request"
        }

        val block = AgreementBlockBuilder(myPeer, database, link, transaction).sign()

        onBlockCreated(block)

        sendBlockPair(link, block)

        return block
    }

    private fun onBlockCreated(block: TrustChainBlock) {

        // validate, sign and persist block, stop if does not validate
        val validation = validateAndPersistBlock(block)
        Log.d(LOGTAG, "Signed block, result: $validation")
        if (validation !is ValidationResult.PartialNext && validation !is ValidationResult.Valid) {
            throw RuntimeException("Signed block did not validate")
        }

        // send block to the counterparty
        val peer = network.getVerifiedByPublicKeyBin(block.linkPublicKey)
        if (peer != null) {
            // If there is a counterparty to sign, we send it
            sendBlock(block, peer = peer)
        }
    }

    fun validateAndPersistBlock(block: TrustChainBlock): ValidationResult {
        val validationResult = block.validate(database)

        if (validationResult is ValidationResult.Invalid) {
            Log.d(LOGTAG, "Block is invalid: ${validationResult.errors}")
        } else {
            if (!database.contains(block)) {
                try {
                    Log.d(LOGTAG, "addBlock " + block.sequenceNumber)
                    database.addBlock(block)
                } catch (e: Exception) {
                    Log.d(LOGTAG, "Failed to insert block into database")
                }

                // Replace listeners with this
                if (sendingToSelf || AndroidCryptoProvider.keyFromPublicBin(block.linkPublicKey) == selfPeer.publicKey) {
                    onBlockReceived(block)
                }

            }
        }
        return validationResult
    }

    private fun onBlockReceived(block: TrustChainBlock) {

        if (block.type == SINGLE_BLOCK) {

            if (block.isProposal) {
                Log.d(LOGTAG, "Received SINGLE proposal block")
                receiveSingleTransactionProposal(block)
            } else if (block.isAgreement) {
                Log.d(LOGTAG, "Received SINGLE agreement block")
                receiveSingleTransactionAgreement(block)
            }
        }

        if (block.type == GROUPED_BLOCK) {

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

    }

}
