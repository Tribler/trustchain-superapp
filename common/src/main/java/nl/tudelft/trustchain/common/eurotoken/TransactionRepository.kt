package nl.tudelft.trustchain.common.eurotoken

import android.util.Log
import kotlinx.coroutines.runBlocking
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.toHex
import java.lang.Math.abs
import java.math.BigInteger

class TransactionRepository(
    val trustChainCommunity: TrustChainCommunity,
    val gatewayStore: GatewayStore
) {
    fun getGatewayPeer(): Peer? {
        return gatewayStore.getPreferred().getOrNull(0)?.peer
    }

    private fun getBalanceChangeForBlock(block: TrustChainBlock?): Long {
        if (block == null) return 0
        return if (
            (listOf(BLOCK_TYPE_TRANSFER).contains(block.type) && block.isProposal) ||
            (listOf(BLOCK_TYPE_ROLLBACK).contains(block.type) && block.isProposal) ||
            (listOf(BLOCK_TYPE_DESTROY).contains(block.type) && block.isProposal)
        ) {
            // block is sending money
            -(block.transaction[KEY_AMOUNT] as BigInteger).toLong()
        } else if (
            (listOf(BLOCK_TYPE_TRANSFER).contains(block.type) && block.isAgreement) ||
            (listOf(BLOCK_TYPE_CREATE).contains(block.type) && block.isAgreement)
        ) {
            // block is receiving money
            (block.transaction[KEY_AMOUNT] as BigInteger).toLong()
            // block.transaction[KEY_AMOUNT] as Long
        } else {
            // block does nothing
            0
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun crawlForLinked(block: TrustChainBlock): TrustChainBlock? {
        val range = LongRange(
            block.sequenceNumber.toLong(),
            block.sequenceNumber.toLong()
        )
        val peer = trustChainCommunity.getPeers().find { it.publicKey.keyToBin().contentEquals(block.publicKey) }
            ?: Peer(defaultCryptoProvider.keyFromPublicBin(block.linkPublicKey))
        // Should only be run when receiving blocks, not when sending
        val blocks = runBlocking { trustChainCommunity.sendCrawlRequest(peer, block.publicKey, range, forHalfBlock = block) }
        if (blocks.isEmpty()) return null // No connection partial previous
        return blocks.find { // linked block
            it.publicKey.contentEquals(block.linkPublicKey) &&
            it.sequenceNumber == block.linkSequenceNumber
        } ?: block // no linked block exists
    }

    fun ensureCheckpointLinks(block: TrustChainBlock, database: TrustChainStore) {
        if (block.publicKey.contentEquals(trustChainCommunity.myPeer.publicKey.keyToBin())) return // no need to crawl own chain
        val blockBefore = database.getBlockWithHash(block.previousHash)
        if (BLOCK_TYPE_CHECKPOINT == block.type && block.isProposal) {
            // block could verify balance
            if (database.getLinked(block) != null) { // verified
                return
            } else { // gateway verification is missing
                val linked = crawlForLinked(block) // try to crawl for it
                    ?: return // peer didnt repond, TODO: Store this detail to make verification work better
                if (linked == block) { // No linked block exists in peer, so they sent the transaction based on a different checkpoint
                    ensureCheckpointLinks(blockBefore ?: return, database) // check next
                } else {
                    return // linked
                }
            }
        } else {
            ensureCheckpointLinks(blockBefore ?: return, database)
        }
    }

    fun getVerifiedBalanceForBlock(block: TrustChainBlock?, database: TrustChainStore): Long? {
        if (block == null) return null // Missing block
        Log.d("EuroTokenBlock", "Validation, Getting balance for block ${block.sequenceNumber}")
        if (block.isGenesis) return 0
        if (!EUROTOKEN_TYPES.contains(block.type)) {
            Log.d("EuroTokenBlock", "Validation, not eurotoken ")
            return getVerifiedBalanceForBlock(
                database.getBlockWithHash(
                    block.previousHash
                ), database
            )
        }
        if (BLOCK_TYPE_CHECKPOINT == block.type && block.isProposal) {
            // block contains balance but linked block determines verification
            if (database.getLinked(block) != null) { // verified
                Log.d("EuroTokenBlock", "Validation, valid checkpoint returning")
                return (block.transaction[KEY_BALANCE] as Long)
            } else {
                Log.d("EuroTokenBlock", "Validation, checkpoint missing acceptance")
                return getVerifiedBalanceForBlock(database.getBlockWithHash(block.previousHash), database)
            }
        } else if (listOf(
                BLOCK_TYPE_TRANSFER,
                BLOCK_TYPE_CREATE
            ).contains(block.type) && block.isAgreement
        ) {
            // block is receiving money, but balance is not verified, just recurse
            Log.d("EuroTokenBlock", "Validation, receiving money")
            return getVerifiedBalanceForBlock(
                database.getBlockWithHash(block.previousHash),
                database
            )
        } else if (listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_DESTROY, BLOCK_TYPE_ROLLBACK).contains(
                block.type
            ) && block.isProposal
        ) {
            Log.d("EuroTokenBlock", "Validation, sending money")
            // block is sending money, but balance is not verified, subtract transfer amount and recurse
            val amount = (block.transaction[KEY_AMOUNT] as BigInteger).toLong()
            return getVerifiedBalanceForBlock(
                database.getBlockWithHash(block.previousHash),
                database
            )?.minus(
                amount
            )
        } else {
            // bad type that shouldn't exist, for now just ignore and return for next
            Log.d("EuroTokenBlock", "Validation, bad type")
            return getVerifiedBalanceForBlock(
                database.getBlockWithHash(block.previousHash),
                database
            )
        }
    }

    fun getBalanceForBlock(block: TrustChainBlock?, database: TrustChainStore): Long? {
        if (block == null) return null
        if (block.isGenesis)
            return getBalanceChangeForBlock(block)
        if (!EUROTOKEN_TYPES.contains(block.type)) return getBalanceForBlock(
            database.getBlockWithHash(
                block.previousHash
            ), database
        )
        return if ( // block contains balance (base case)
            (listOf(
                BLOCK_TYPE_TRANSFER,
                BLOCK_TYPE_DESTROY,
                BLOCK_TYPE_CHECKPOINT,
                BLOCK_TYPE_ROLLBACK
            ).contains(block.type) && block.isProposal)
        ) {
            (block.transaction[KEY_BALANCE] as Long)
        } else if (listOf(
                BLOCK_TYPE_TRANSFER,
                BLOCK_TYPE_CREATE
            ).contains(block.type) && block.isAgreement
        ) {
            // block is receiving money add it and recurse
            getBalanceForBlock(database.getBlockWithHash(block.previousHash), database)?.plus(
                (block.transaction[KEY_AMOUNT] as BigInteger).toLong()
            )
        } else {
            // bad type that shouldn't exist, for now just ignore and return for next
            getBalanceForBlock(database.getBlockWithHash(block.previousHash), database)
        }
    }

    fun getMyVerifiedBalance(): Long {
        val mykey = IPv8Android.getInstance().myPeer.publicKey.keyToBin()
        val latestBlock = trustChainCommunity.database.getLatest(mykey) ?: return 0
        return getVerifiedBalanceForBlock(latestBlock, trustChainCommunity.database)!!
    }

    private fun getMyBalance(): Long {
        val myKey = IPv8Android.getInstance().myPeer.publicKey.keyToBin()
        val latestBlock = trustChainCommunity.database.getLatest(myKey) ?: return 0
        return getBalanceForBlock(latestBlock, trustChainCommunity.database)!!
    }

    fun sendTransferProposal(recipient: ByteArray, amount: Long): TrustChainBlock? {
        if (getMyVerifiedBalance() - amount < 0) {
            return null
        }
        val transaction = mapOf(
            KEY_AMOUNT to BigInteger.valueOf(amount),
            KEY_BALANCE to (BigInteger.valueOf(getMyBalance() - amount).toLong())
        )
        return trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_TRANSFER, transaction,
            recipient
        )
    }

    fun verifyBalance() {
        getGatewayPeer()?.let { sendCheckpointProposal(it) }
    }

    fun sendCheckpointProposal(peer: Peer): TrustChainBlock {
        Log.w("EuroTokenBlockCheck", "Creating check...")
        val transaction = mapOf(
            KEY_BALANCE to BigInteger.valueOf(getMyBalance()).toLong()
        )
        val block = trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_CHECKPOINT, transaction,
            peer.publicKey.keyToBin()
        )

        Log.w("EuroTokenBlockCheck", "Block made")

        trustChainCommunity.sendBlock(block, peer)
        Log.w("EuroTokenBlockCheck", "Sent to peer")
        return block
    }

    fun attemptRollback(peer: Peer?, blockHash: ByteArray): TrustChainBlock? {
        if (peer != null && peer.publicKey != getGatewayPeer()?.publicKey) {
            Log.w("EuroTokenBlockRollback", "Not a valid gateway")
            return null
        }
        val rolledBackBlock = trustChainCommunity.database.getBlockWithHash(blockHash)
        if (rolledBackBlock == null) {
            Log.d("EuroTokenBlockRollback", "block not found")
            return null
        }
        if (!rolledBackBlock.publicKey.contentEquals(trustChainCommunity.myPeer.publicKey.keyToBin())) {
            Log.d("EuroTokenBlockRollback", "Not my block")
            return null
        }
        val amount = rolledBackBlock.transaction[KEY_AMOUNT] as BigInteger
        val transaction = mapOf(
            KEY_TRANSACTION to blockHash.toHex(),
            KEY_AMOUNT to amount,
            KEY_BALANCE to (BigInteger.valueOf(getMyBalance() - amount.toLong()).toLong())
        )
        Log.d("EuroTokenBlockRollback", (transaction[KEY_BALANCE] as Long).toString())
        return trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_ROLLBACK, transaction,
            rolledBackBlock.publicKey
        )
    }

    fun sendDestroyProposal(
        recipient: ByteArray,
        ip: String,
        port: Int,
        paymentId: String,
        amount: Long
    ): TrustChainBlock? {
        Log.w("EuroTokenBlockDestroy", "Creating destroy...")
        val key = defaultCryptoProvider.keyFromPublicBin(recipient)
        val address = IPv4Address(ip, port)
        val peer = Peer(key, address)

        if (getMyVerifiedBalance() - amount < 0) {
            return null
        }

        val transaction = mapOf(
            KEY_PAYMENT_ID to paymentId,
            KEY_AMOUNT to BigInteger.valueOf(amount),
            KEY_BALANCE to (BigInteger.valueOf(getMyBalance() - amount).toLong())
        )
        val block = trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_DESTROY, transaction,
            recipient
        )
        Log.w("EuroTokenBlockDestroy", "Block made")

        trustChainCommunity.sendBlock(block, peer)
        Log.w("EuroTokenBlockDestroy", "Sent to peer")
        return block
    }

    fun getTransactions(): List<Transaction> {
        val myKey = trustChainCommunity.myPeer.publicKey.keyToBin()
        return trustChainCommunity.database.getLatestBlocks(myKey, 1000)
            .filter { block: TrustChainBlock -> EUROTOKEN_TYPES.contains(block.type) }
            .map { block: TrustChainBlock ->
                val sender = defaultCryptoProvider.keyFromPublicBin(block.publicKey)
                Transaction(
                    block,
                    sender,
                    defaultCryptoProvider.keyFromPublicBin(block.linkPublicKey),
                    if (block.transaction.containsKey(KEY_AMOUNT)) {
                        (block.transaction[KEY_AMOUNT] as BigInteger).toLong()
                    } else 0L,
                    block.type,
                    getBalanceChangeForBlock(block) < 0,
                    block.timestamp
                )
            }
    }

    fun getTransactionWithHash(hash: ByteArray?): TrustChainBlock? {
        return hash?.let {
            trustChainCommunity.database
                .getBlockWithHash(it)
        }
    }

    fun verifyBalanceAvailable(
        block: TrustChainBlock,
        database: TrustChainStore
    ): ValidationResult {
        val balance =
            getVerifiedBalanceForBlock(block, database) ?: return ValidationResult.PartialPrevious
        if (balance < 0) {
            return ValidationResult.Invalid(
                listOf(
                    "Insufficient balance ($balance) for amount (${
                        getBalanceChangeForBlock(
                            block
                        )
                    })"
                )
            )
        }
        return ValidationResult.Valid
    }

    fun verifyListedBalance(block: TrustChainBlock, database: TrustChainStore): ValidationResult {
        if (!block.transaction.containsKey(KEY_BALANCE)) return ValidationResult.Invalid(listOf("Missing balance"))
        if (block.isGenesis) {
            if (block.transaction.containsKey(KEY_AMOUNT)) {
                if (block.transaction[KEY_BALANCE] != -(block.transaction[KEY_AMOUNT] as BigInteger).toLong()) {
                    return ValidationResult.Invalid(listOf("Invalid genesis balance"))
                } else {
                    return ValidationResult.Valid
                }
            } else {
                if (block.transaction[KEY_BALANCE] != 0L) {
                    return ValidationResult.Invalid(listOf("Invalid genesis balance"))
                } else {
                    return ValidationResult.Valid
                }
            }
        }
        val blockBefore = database.getBlockWithHash(block.previousHash)
        if (blockBefore == null) {
            Log.d("EuroTokenBlock", "Has to crawl for previous!!")
            return ValidationResult.PartialPrevious
        }
        val balanceBefore =
            getBalanceForBlock(blockBefore, database) ?: return ValidationResult.PartialPrevious
        val change = getBalanceChangeForBlock(block)
        if (block.transaction[KEY_BALANCE] != balanceBefore + change) {
            Log.w("EuroTokenBlock", "Invalid balance")
            return ValidationResult.Invalid(listOf("Invalid balance"))
        }
        return ValidationResult.Valid
    }

    private fun addTransferListeners() {
        trustChainCommunity.registerTransactionValidator(
            BLOCK_TYPE_TRANSFER,
            object : TransactionValidator {
                override fun validate(
                    block: TrustChainBlock,
                    database: TrustChainStore
                ): ValidationResult {
                    if (block.isProposal) {
                        if (!block.transaction.containsKey(KEY_AMOUNT)) return ValidationResult.Invalid(
                            listOf("Missing amount")
                        )
                        var result = verifyListedBalance(block, database)
                        if (result != ValidationResult.Valid) {
                            return result
                        }
                        result = verifyBalanceAvailable(block, database)
                        if (result != ValidationResult.Valid) {
                            return result
                        }
                    } else {
                        if (database.getLinked(block)?.transaction?.equals(block.transaction) != true) {
                            return ValidationResult.Invalid(
                                listOf(
                                    "Linked transaction doesn't match (${block.transaction}, ${
                                        database.getLinked(
                                            block
                                        )?.transaction ?: "MISSING"
                                    })"
                                )
                            )
                        }
                    }
                    return ValidationResult.Valid
                }
            })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_TRANSFER, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                Log.w("EuroTokenBlockTransfer", "sig request ${block.transaction}")
                // agree if validated
                trustChainCommunity.sendBlock(
                    trustChainCommunity.createAgreementBlock(
                        block,
                        block.transaction
                    )
                )
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_TRANSFER, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                ensureCheckpointLinks(block, trustChainCommunity.database)
                if (block.isAgreement && block.publicKey.contentEquals(trustChainCommunity.myPeer.publicKey.keyToBin())) {
                    verifyBalance()
                }
                Log.d("EuroTokenBlock", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }

    private fun addCreationListeners() {
        trustChainCommunity.registerTransactionValidator(
            BLOCK_TYPE_CREATE,
            object : TransactionValidator {
                override fun validate(
                    block: TrustChainBlock,
                    database: TrustChainStore
                ): ValidationResult {
                    // if (!block.transaction.containsKey(KEY_BALANCE)) return false
                    if (block.isProposal) {
                        if (!block.transaction.containsKey(KEY_AMOUNT)) return ValidationResult.Invalid(
                            listOf("Missing amount")
                        )
                        if (!block.transaction.containsKey(KEY_PAYMENT_ID)) return ValidationResult.Invalid(
                            listOf("Missing Payment ID")
                        )
                        Log.w("EuroTokenBlockCreate", "Is valid proposal")
                    } else {
                        if (database.getLinked(block)?.transaction?.equals(block.transaction) != true) return ValidationResult.Invalid(
                            listOf("Linked transaction doesn't match")
                        )
                    }
                    // TODO: validate gateway ID here
                    return ValidationResult.Valid
                }
            })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_CREATE, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                Log.w("EuroTokenBlockCreate", "sig request")
                // only gateways should sign creations
                trustChainCommunity.sendBlock(
                    trustChainCommunity.createAgreementBlock(
                        block,
                        block.transaction
                    )
                )
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_CREATE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                ensureCheckpointLinks(block, trustChainCommunity.database)
                if (block.isAgreement && block.publicKey.contentEquals(trustChainCommunity.myPeer.publicKey.keyToBin())) {
                    verifyBalance()
                }
                Log.w(
                    "EuroTokenBlockCreate",
                    "onBlockReceived: ${block.blockId} ${block.transaction}"
                )
            }
        })
    }

    private fun addDestructionListeners() {
        trustChainCommunity.registerTransactionValidator(
            BLOCK_TYPE_DESTROY,
            object : TransactionValidator {
                override fun validate(
                    block: TrustChainBlock,
                    database: TrustChainStore
                ): ValidationResult {
                    if (!block.transaction.containsKey(KEY_AMOUNT)) return ValidationResult.Invalid(
                        listOf("Missing amount")
                    )
                    if (!block.transaction.containsKey(KEY_PAYMENT_ID)) return ValidationResult.Invalid(
                        listOf("Missing Payment id")
                    )
                    if (block.isProposal) {
                        var result = verifyListedBalance(block, database)
                        if (result != ValidationResult.Valid) {
                            return result
                        }
                        result = verifyBalanceAvailable(block, database)
                        if (result != ValidationResult.Valid) {
                            return result
                        }
                    }
                    // TODO: validate gateway here
                    return ValidationResult.Valid
                }
            })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_DESTROY, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                // only gateways should sign destructions
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_DESTROY, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                ensureCheckpointLinks(block, trustChainCommunity.database)
                Log.d(
                    "EuroTokenBlockDestroy",
                    "onBlockReceived: ${block.blockId} ${block.transaction}"
                )
            }
        })
    }

    private fun addCheckpointListeners() {
        trustChainCommunity.registerTransactionValidator(
            BLOCK_TYPE_CHECKPOINT,
            object : TransactionValidator {
                override fun validate(
                    block: TrustChainBlock,
                    database: TrustChainStore
                ): ValidationResult {
                    if (block.isProposal) {
                        var result = verifyListedBalance(block, database)
                        if (result != ValidationResult.Valid) {
                            return result
                        }
                        result = verifyBalanceAvailable(block, database)
                        if (result != ValidationResult.Valid) {
                            return result
                        }
                    }
                    // TODO: validate gateway here
                    return ValidationResult.Valid
                }
            })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_CHECKPOINT, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                // only gateways should sign checkpoints
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_CHECKPOINT, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d(
                    "EuroTokenBlockCheck",
                    "onBlockReceived: ${block.blockId} ${block.transaction}"
                )
            }
        })
    }

    private fun addRollbackListeners() {
        trustChainCommunity.registerTransactionValidator(
            BLOCK_TYPE_ROLLBACK,
            object : TransactionValidator {
                override fun validate(
                    block: TrustChainBlock,
                    database: TrustChainStore
                ): ValidationResult {
                    if (!block.transaction.containsKey(KEY_AMOUNT)) return ValidationResult.Invalid(
                        listOf("Missing amount")
                    )
                    if (!block.transaction.containsKey(KEY_TRANSACTION)) return ValidationResult.Invalid(
                        listOf("Missing transaction hash")
                    )
                    if (block.isProposal) {
                        var result = verifyListedBalance(block, database)
                        if (result != ValidationResult.Valid) {
                            return result
                        }
                        result = verifyBalanceAvailable(block, database)
                        if (result != ValidationResult.Valid) {
                            return result
                        }
                    }
                    return ValidationResult.Valid
                }
            })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_ROLLBACK, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                // rollbacks don't need to be signed, their existence is a declaration of forfeit
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_ROLLBACK, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                ensureCheckpointLinks(block, trustChainCommunity.database)
                Log.d(
                    "EuroTokenBlockRollback",
                    "onBlockReceived: ${block.blockId} ${block.transaction}"
                )
            }
        })
    }

    fun initTrustChainCommunity() {
        addTransferListeners()
        addCreationListeners()
        addDestructionListeners()
        addCheckpointListeners()
        addRollbackListeners()
    }

    companion object {
        fun prettyAmount(amount: Long): String {
            return "€" + (amount / 100).toString() + "," + (abs(amount) % 100).toString()
                .padStart(2, '0')
        }

        const val BLOCK_TYPE_TRANSFER = "eurotoken_transfer"
        const val BLOCK_TYPE_CREATE = "eurotoken_creation"
        const val BLOCK_TYPE_DESTROY = "eurotoken_destruction"
        const val BLOCK_TYPE_CHECKPOINT = "eurotoken_checkpoint"
        const val BLOCK_TYPE_ROLLBACK = "eurotoken_rollback"

        private val EUROTOKEN_TYPES = listOf(
            BLOCK_TYPE_TRANSFER,
            BLOCK_TYPE_CREATE,
            BLOCK_TYPE_DESTROY,
            BLOCK_TYPE_CHECKPOINT,
            BLOCK_TYPE_ROLLBACK
        )

        const val KEY_AMOUNT = "amount"
        const val KEY_BALANCE = "balance"
        const val KEY_TRANSACTION = "transaction_hash"
        const val KEY_PAYMENT_ID = "payment_id"
    }
}
