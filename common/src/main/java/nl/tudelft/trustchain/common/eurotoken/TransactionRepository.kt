package nl.tudelft.trustchain.common.eurotoken

import android.util.Log
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import java.lang.Math.abs
import java.math.BigInteger

class TransactionRepository (
    private val trustChainCommunity: TrustChainCommunity
) {

    fun getBlockBalanceChange(block: TrustChainBlock?) : Long {
        if (block == null) return 0
        return if ( listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_DESTROY).contains(block.type) && block.isProposal ) {
            // block is sending money
            -(block.transaction[KEY_AMOUNT] as BigInteger).toLong()
        } else if (listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_CREATE).contains(block.type) && block.isAgreement) {
            // block is receiving money
            (block.transaction[KEY_AMOUNT] as BigInteger).toLong()
        } else {
            //block does nothing
            0
        }
    }

    fun getVerifiedBalanceForBlock(block: TrustChainBlock?) : Long {
        if (block == null) return 0
        if (!EUROTOKEN_TYPES.contains(block.type)) return getVerifiedBalanceForBlock(trustChainCommunity.database.getBlockBefore(block))
        if (block.type == BLOCK_TYPE_CREATE && block.isAgreement) {
            // block is verified balance
            return (block.transaction[KEY_BALANCE] as BigInteger).toLong()
        } else if ( listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_DESTROY, BLOCK_TYPE_CHECKPOINT).contains(block.type) && block.isProposal ) {
            // block contains balance but linked block determines verification
            if (trustChainCommunity.database.getLinked(block) != null){ //verified
                return (block.transaction[KEY_BALANCE] as BigInteger).toLong()
            } else { // subtract transfer amount and recurse TODO: maybe crawl for block??
                val amount = if (block.type == BLOCK_TYPE_CHECKPOINT) 0
                else (block.transaction[KEY_AMOUNT] as BigInteger).toLong()
                return getVerifiedBalanceForBlock(trustChainCommunity.database.getBlockBefore(block)) - amount
            }
        } else if (listOf(BLOCK_TYPE_TRANSFER).contains(block.type) && block.isAgreement) {
            // block is receiving money, but it is not verified, just recurse
            return getVerifiedBalanceForBlock(trustChainCommunity.database.getBlockBefore(block))
        } else {
            //bad type that shouldn't exist, for now just ignore and return for next
            return getVerifiedBalanceForBlock(trustChainCommunity.database.getBlockBefore(block))
        }
    }

    fun getBalanceForBlock(block: TrustChainBlock?) : Long {
        if (block == null) return 0
        if (!EUROTOKEN_TYPES.contains(block.type)) return getBalanceForBlock(trustChainCommunity.database.getBlockBefore(block))
        if ( // block contains balance (base case)
            (listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_DESTROY, BLOCK_TYPE_CHECKPOINT).contains(block.type) && block.isProposal) ||
            (block.type == BLOCK_TYPE_CREATE && block.isAgreement)
        ) {
            return (block.transaction[KEY_BALANCE] as BigInteger).toLong()
        } else if (listOf(BLOCK_TYPE_TRANSFER).contains(block.type) && block.isAgreement) {
            // block is receiving money add it and recurse
            return getBalanceForBlock(trustChainCommunity.database.getBlockBefore(block)) -
                (block.transaction[KEY_AMOUNT] as BigInteger).toLong()
        } else {
            //bad type that shouldn't exist, for now just ignore and return for next
            return getBalanceForBlock(trustChainCommunity.database.getBlockBefore(block))
        }
    }

    fun getMyBalance() : Long {
        val mykey = trustChainCommunity.myPeer.publicKey.keyToBin()
        val latestBlocks = trustChainCommunity.database.getLatestBlocks(mykey, 1, EUROTOKEN_TYPES)
        val latestBlock : TrustChainBlock? = if (latestBlocks.size == 1) latestBlocks[0] else null
        return getBalanceForBlock(latestBlock)
    }

    fun isBlockTransactionVerified(block: TrustChainBlock?): Boolean {
        if (block == null) return false
        if (block.type == BLOCK_TYPE_CREATE && block.isAgreement) {
            // block is verified
            return true
        } else if ( listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_DESTROY, BLOCK_TYPE_CHECKPOINT).contains(block.type) && block.isProposal ) {
            // linked block determines verification
            if (trustChainCommunity.database.getLinked(block) != null){ //verified
                return true
            } else { // if not verified yet TODO: maybe crawl for block??
                return false //isBlockTransactionVerified(trustChainCommunity.database.getBlockAfter(block))
            }
        } else {
            // block is not a verification block, recurse
            return isBlockTransactionVerified(trustChainCommunity.database.getBlockAfter(block))
        }
    }

    fun sendTransferProposal(recipient: ByteArray, amount: Long): TrustChainBlock {
        val transaction = mapOf(
            KEY_AMOUNT to amount,
            KEY_BALANCE to BigInteger.valueOf(getMyBalance() - amount)
        )
        return trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_TRANSFER, transaction,
            recipient
        )
    }

    fun sendCheckpointProposal(recipient: String, ip: String, port: Int): TrustChainBlock {
        val transaction = mapOf(
            KEY_BALANCE to BigInteger.valueOf(getMyBalance())
        )
        val block =  trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_CHECKPOINT, transaction,
            recipient.hexToBytes()
        )

        val key = defaultCryptoProvider.keyFromPublicBin(recipient.hexToBytes())
        val address = IPv4Address(ip, port)
        val peer = Peer(key, address)

        trustChainCommunity.sendBlock(block, peer)
        return block
    }

    fun sendDestroyProposal(paymentId: String, amount: Long, recipient: String, ip: String, port: Int): TrustChainBlock {
        val transaction = mapOf(
            KEY_PAYMENT_ID to paymentId,
            KEY_AMOUNT to BigInteger.valueOf(amount),
            KEY_BALANCE to BigInteger.valueOf(getMyBalance() - amount)
        )
        val block =  trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_DESTROY, transaction,
            recipient.hexToBytes()
        )

        val key = defaultCryptoProvider.keyFromPublicBin(recipient.hexToBytes())
        val address = IPv4Address(ip, port)
        val peer = Peer(key, address)

        trustChainCommunity.sendBlock(block, peer)
        return block
    }

    fun getTransactions(): List<Transaction> {
        val mykey = trustChainCommunity.myPeer.publicKey.keyToBin()
        return trustChainCommunity.database.getLatestBlocks(mykey, 1000, EUROTOKEN_TYPES).map { block : TrustChainBlock ->
            val sender = defaultCryptoProvider.keyFromPublicBin(block.publicKey)
            Transaction(
                block,
                sender,
                defaultCryptoProvider.keyFromPublicBin(block.linkPublicKey),
                (block.transaction[KEY_AMOUNT] as BigInteger).toLong(),
                block.type,
                sender == trustChainCommunity.myPeer.publicKey,
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

    fun addTransferListners(){
        trustChainCommunity.registerTransactionValidator(BLOCK_TYPE_TRANSFER, object : TransactionValidator {
            override fun validate(
                block: TrustChainBlock,
                database: TrustChainStore
            ): Boolean {
                if (!block.transaction.containsKey(KEY_BALANCE)) return false
                if (!block.transaction.containsKey(KEY_AMOUNT)) return false
                val balanceBefore = getBalanceForBlock(database.getBlockBefore(block))
                if (block.isProposal){
                    if (block.transaction[KEY_BALANCE] != balanceBefore + getBlockBalanceChange(block))
                        return false
                } else {
                    if (database.getLinked(block) != null) return false//TODO: maybe crawl
                }
                return true
            }
        })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_TRANSFER, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                //agree if validated
                trustChainCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_TRANSFER, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("EuroTokenBlock", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }

    fun addCreationListners(){
        trustChainCommunity.registerTransactionValidator(BLOCK_TYPE_CREATE, object : TransactionValidator {
            override fun validate(
                block: TrustChainBlock,
                database: TrustChainStore
            ): Boolean {
                if (!block.transaction.containsKey(KEY_BALANCE)) return false
                if (!block.transaction.containsKey(KEY_AMOUNT)) return false
                if (!block.transaction.containsKey(KEY_PAYMENT_ID)) return false
                val balanceBefore = getBalanceForBlock(database.getBlockBefore(block))
                if (block.transaction[KEY_BALANCE] != balanceBefore + getBlockBalanceChange(block))
                    return false
                //TODO: validate gateway ID here
                return true
            }
        })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_CREATE, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                //only gateways should sign creations
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_CREATE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("EuroTokenBlock", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })

    }
    fun addDestructionListners(){
        trustChainCommunity.registerTransactionValidator(BLOCK_TYPE_DESTROY, object : TransactionValidator {
            override fun validate(
                block: TrustChainBlock,
                database: TrustChainStore
            ): Boolean {
                if (!block.transaction.containsKey(KEY_BALANCE)) return false
                if (!block.transaction.containsKey(KEY_AMOUNT)) return false
                if (!block.transaction.containsKey(KEY_PAYMENT_ID)) return false
                val balanceBefore = getBalanceForBlock(database.getBlockBefore(block))
                if (block.transaction[KEY_BALANCE] != balanceBefore + getBlockBalanceChange(block))
                    return false
                //TODO: validate gateway here
                return true
            }
        })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_DESTROY, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                //only gateways should sign destructions
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_DESTROY, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("EuroTokenBlock", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }

    fun addCheckpointListners(){
        trustChainCommunity.registerTransactionValidator(BLOCK_TYPE_CHECKPOINT, object : TransactionValidator {
            override fun validate(
                block: TrustChainBlock,
                database: TrustChainStore
            ): Boolean {
                if (!block.transaction.containsKey(KEY_BALANCE)) return false
                val balanceBefore = getBalanceForBlock(database.getBlockBefore(block))
                if (block.transaction[KEY_BALANCE] != balanceBefore + getBlockBalanceChange(block))
                    return false
                //TODO: validate gateway here
                return true
            }
        })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_CHECKPOINT, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                //only gateways should sign checkpoints
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_CHECKPOINT, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("EuroTokenBlock", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }

    fun initTrustChainCommunity() {
        addTransferListners()
        addCreationListners()
        addDestructionListners()
        addCheckpointListners()
    }


    companion object {
        fun prettyAmount(amount: Long): String {
            return "€" + (amount / 100).toString() + "," + (abs(amount) % 100).toString().padStart(2, '0')
        }

        private const val BLOCK_TYPE_TRANSFER    = "eurotoken_transfer"
        private const val BLOCK_TYPE_CREATE      = "eurotoken_creation"
        private const val BLOCK_TYPE_DESTROY     = "eurotoken_destruction"
        private const val BLOCK_TYPE_CHECKPOINT  = "eurotoken_checkpoint"

        private val EUROTOKEN_TYPES  = listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_CREATE, BLOCK_TYPE_DESTROY, BLOCK_TYPE_CHECKPOINT)

        const val KEY_AMOUNT = "amount"
        const val KEY_BALANCE = "balance"
        const val KEY_PAYMENT_ID = "payment_id"

    }

}
