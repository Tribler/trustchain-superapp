package nl.tudelft.trustchain.common.eurotoken

import android.util.Log
import kotlinx.coroutines.*
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.blocks.*
import java.lang.Math.abs
import java.math.BigInteger

@OptIn(ExperimentalUnsignedTypes::class)
class TransactionRepository(
    val trustChainCommunity: TrustChainCommunity,
    val gatewayStore: GatewayStore
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getGatewayPeer(): Peer? {
        return gatewayStore.getPreferred().getOrNull(0)?.peer
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

    fun sendTransferProposal(recipient: ByteArray, amount: Long): Boolean {
        if (getMyVerifiedBalance() - amount < 0) {
            return false
        }
        scope.launch {
            sendTransferProposalSync(recipient, amount)
        }
        return true
    }

    fun sendTransferProposalSync(recipient: ByteArray, amount: Long): TrustChainBlock? {
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

    fun sendCheckpointProposal(peer: Peer) {
        val transaction = mapOf(
            KEY_BALANCE to BigInteger.valueOf(getMyBalance()).toLong()
        )
        val block = trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_CHECKPOINT, transaction,
            peer.publicKey.keyToBin()
        )
        scope.launch {
            trustChainCommunity.sendBlock(block, peer)
        }
    }

    fun attemptRollback(peer: Peer?, blockHash: ByteArray) {
        if (peer != null && peer.publicKey != getGatewayPeer()?.publicKey) {
            Log.w("EuroTokenBlockRollback", "Not a valid gateway")
            return
        }
        val rolledBackBlock = trustChainCommunity.database.getBlockWithHash(blockHash)
        if (rolledBackBlock == null) {
            Log.d("EuroTokenBlockRollback", "block not found")
            return
        }
        if (!rolledBackBlock.publicKey.contentEquals(trustChainCommunity.myPeer.publicKey.keyToBin())) {
            Log.d("EuroTokenBlockRollback", "Not my block")
            return
        }
        val amount = rolledBackBlock.transaction[KEY_AMOUNT] as BigInteger
        val transaction = mapOf(
            KEY_TRANSACTION_HASH to blockHash.toHex(),
            KEY_AMOUNT to amount,
            KEY_BALANCE to (BigInteger.valueOf(getMyBalance() - amount.toLong()).toLong())
        )
        Log.d("EuroTokenBlockRollback", (transaction[KEY_BALANCE] as Long).toString())
        scope.launch {
            trustChainCommunity.createProposalBlock(
                BLOCK_TYPE_ROLLBACK, transaction,
                rolledBackBlock.publicKey
            )
        }
    }

    fun sendDestroyProposalWithIBAN(
        iban: String,
        amount: Long
    ): TrustChainBlock? {
        Log.w("EuroTokenBlockDestroy", "Creating destroy...")
        val peer = getGatewayPeer() ?: return null

        if (getMyVerifiedBalance() - amount < 0) {
            return null
        }

        val transaction = mapOf(
            KEY_IBAN to iban,
            KEY_AMOUNT to BigInteger.valueOf(amount),
            KEY_BALANCE to (BigInteger.valueOf(getMyBalance() - amount).toLong())
        )
        val block = trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_DESTROY, transaction,
            peer.publicKey.keyToBin()
        )

        trustChainCommunity.sendBlock(block, peer)
        return block
    }

    fun sendDestroyProposalWithPaymentID(
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

        trustChainCommunity.sendBlock(block, peer)
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

    private fun addTransferListeners() {
        trustChainCommunity.registerTransactionValidator(
            BLOCK_TYPE_TRANSFER,
            EuroTokenTransferValidator(this)
        )

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
                // Auto verifyBalance
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
            EuroTokenCreationValidator(this))

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
            EuroTokenDestructionValidator(this)
        )

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_DESTROY, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                // only gateways should sign destructions
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_DESTROY, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
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
            EuroTokenCheckpointValidator(this)
        )

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_CHECKPOINT, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                // only gateways should sign checkpoints
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_CHECKPOINT, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d(
                    "EuroTokenBlockCheck",
                    "onBlockReceived: ${block.isProposal} ${block.blockId} ${block.transaction}"
                )
            }
        })
    }

    private fun addRollbackListeners() {
        trustChainCommunity.registerTransactionValidator(
            BLOCK_TYPE_ROLLBACK,
            EuroTokenRollBackValidator(this)
        )

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_ROLLBACK, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                // rollbacks don't need to be signed, their existence is a declaration of forfeit
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_ROLLBACK, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
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
//        private lateinit var instance: TransactionRepository
//        fun getInstance(gatewayStore: GatewayStore, trustChainCommunity: TrustChainCommunity): TransactionRepository {
//            if (!Companion::instance.isInitialized) {
//                instance = TransactionRepository(trustChainCommunity, gatewayStore )
//            }
//            return instance
//        }

        fun prettyAmount(amount: Long): String {
            return "€" + (amount / 100).toString() + "," + (abs(amount) % 100).toString()
                .padStart(2, '0')
        }

        const val BLOCK_TYPE_TRANSFER = "eurotoken_transfer"
        const val BLOCK_TYPE_CREATE = "eurotoken_creation"
        const val BLOCK_TYPE_DESTROY = "eurotoken_destruction"
        const val BLOCK_TYPE_CHECKPOINT = "eurotoken_checkpoint"
        const val BLOCK_TYPE_ROLLBACK = "eurotoken_rollback"

        val EUROTOKEN_TYPES = listOf(
            BLOCK_TYPE_TRANSFER,
            BLOCK_TYPE_CREATE,
            BLOCK_TYPE_DESTROY,
            BLOCK_TYPE_CHECKPOINT,
            BLOCK_TYPE_ROLLBACK
        )

        const val KEY_AMOUNT = "amount"
        const val KEY_BALANCE = "balance"
        const val KEY_TRANSACTION_HASH = "transaction_hash"
        const val KEY_PAYMENT_ID = "payment_id"
        const val KEY_IBAN = "iban"
    }
}
