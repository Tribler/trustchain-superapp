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

import nl.tudelft.ipv8.android.IPv8Android
import java.net.InetAddress

class TransactionRepository (
    public val trustChainCommunity: TrustChainCommunity
) {
    fun getGatewayPeer(): Peer {

        //val ipaddress = InetAddress.getByName("eurotoken.hectobyte.net").address.toString()
        val ipaddress = "10.0.0.27"
        val tcpip = IPv4Address(ipaddress, 8090)

        val pubkey = "4c69624e61434c504b3a035fd325276e03b9d0d106a91353cdd00f7a21aa861be79226224809cfedf80cbcc0e210c2ddc2f91a1fbc3e1e3cd0622e32027a27a8be7f5d28a73b42c0369f"
        val key = defaultCryptoProvider.keyFromPublicBin(pubkey.hexToBytes())

        return Peer(key, tcpip)
    }

    fun getBlockBalanceChange(block: TrustChainBlock?) : Long {
        if (block == null) return 0
        return if (
            ( listOf(BLOCK_TYPE_TRANSFER).contains(block.type) && block.isProposal ) ||
            ( listOf(BLOCK_TYPE_DESTROY).contains(block.type) && block.isProposal )
        ) {
            // block is sending money
            //-(block.transaction[KEY_AMOUNT] as Long)
            -(block.transaction[KEY_AMOUNT] as BigInteger).toLong()
        } else if (
            ( listOf(BLOCK_TYPE_TRANSFER).contains(block.type) && block.isAgreement) ||
            ( listOf(BLOCK_TYPE_CREATE).contains(block.type) && block.isAgreement )
        ) {
            // block is receiving money
            (block.transaction[KEY_AMOUNT] as BigInteger).toLong()
            //block.transaction[KEY_AMOUNT] as Long
        } else {
            //block does nothing
            0
        }
    }

    fun getVerifiedBalanceForBlock(block: TrustChainBlock?) : Long {
        if (block == null) return 0
        if (!EUROTOKEN_TYPES.contains(block.type)) return getVerifiedBalanceForBlock(trustChainCommunity.database.getBlockBefore(block))
        if ( listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_DESTROY, BLOCK_TYPE_CHECKPOINT).contains(block.type) && block.isProposal ) {
            // block contains balance but linked block determines verification
            return if (trustChainCommunity.database.getLinked(block) != null){ //verified
                (block.transaction[KEY_BALANCE] as Long)
            } else { // subtract transfer amount and recurse TODO: maybe crawl for block??
                val amount = if (block.type == BLOCK_TYPE_CHECKPOINT) 0
                else (block.transaction[KEY_AMOUNT] as BigInteger).toLong()
                getVerifiedBalanceForBlock(trustChainCommunity.database.getBlockBefore(block)) - amount
            }
        } else if (listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_CREATE).contains(block.type) && block.isAgreement) {
            // block is receiving money, but balance is not verified, just recurse
            return getVerifiedBalanceForBlock(trustChainCommunity.database.getBlockBefore(block))
        } else {
            //bad type that shouldn't exist, for now just ignore and return for next
            return getVerifiedBalanceForBlock(trustChainCommunity.database.getBlockBefore(block))
        }
    }

    fun getBalanceForBlock(block: TrustChainBlock?) : Long {
        if (block == null) return 0
        if (!EUROTOKEN_TYPES.contains(block.type)) return getBalanceForBlock(trustChainCommunity.database.getBlockBefore(block))
        return if ( // block contains balance (base case)
            (listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_DESTROY, BLOCK_TYPE_CHECKPOINT).contains(block.type) && block.isProposal)
        ) {
            (block.transaction[KEY_BALANCE] as Long)
        } else if (listOf(BLOCK_TYPE_TRANSFER, BLOCK_TYPE_CREATE).contains(block.type) && block.isAgreement) {
            // block is receiving money add it and recurse
            getBalanceForBlock(trustChainCommunity.database.getBlockBefore(block)) + (block.transaction[KEY_AMOUNT] as BigInteger).toLong()
        } else {
            //bad type that shouldn't exist, for now just ignore and return for next
            getBalanceForBlock(trustChainCommunity.database.getBlockBefore(block))
        }
    }

    fun getMyBalance() : Long {
        val mykey = IPv8Android.getInstance().myPeer.publicKey.keyToBin()
        val latestBlock = trustChainCommunity.database.getLatest(mykey)
        //val latestBlocks = trustChainCommunity.database.getLatestBlocks(mykey, limit=1, blockTypes=EUROTOKEN_TYPES)
        //val latestBlock : TrustChainBlock? = if (latestBlocks.isNotEmpty()) latestBlocks[0] else null
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
            KEY_AMOUNT to BigInteger.valueOf(amount),
            KEY_BALANCE to (BigInteger.valueOf(getMyBalance() - amount).toLong())
        )
        return trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_TRANSFER, transaction,
            recipient
        )
    }

    fun getPeer(recipient: String, ip: String, port: Int): Peer {
        val key = defaultCryptoProvider.keyFromPublicBin(recipient.hexToBytes())
        val address = IPv4Address(ip, port)
        return Peer(key, address)
    }

    fun sendCheckpointProposal(recipient: String, ip: String, port: Int): TrustChainBlock {
        return sendCheckpointProposal(getPeer(recipient, ip, port))
    }

    fun sendCheckpointProposal(peer: Peer): TrustChainBlock {
        Log.w("EuroTokenBlockCheck", "Creating check..." )
        val transaction = mapOf(
            KEY_BALANCE to BigInteger.valueOf(getMyBalance()).toLong()
        )
        val block =  trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_CHECKPOINT, transaction,
            peer.publicKey.keyToBin()
        )

        Log.w("EuroTokenBlockCheck", "Block made" )

        trustChainCommunity.sendBlock(block, peer)
        Log.w("EuroTokenBlockCheck", "Sent to peer" )
        return block
    }

    fun sendDestroyProposal(recipient: ByteArray, ip: String, port: Int, paymentId: String, amount: Long): TrustChainBlock {
        Log.w("EuroTokenBlockDestroy", "Creating destroy..." )
        val key = defaultCryptoProvider.keyFromPublicBin(recipient)
        val address = IPv4Address(ip, port)
        val peer = Peer(key, address)

        val transaction = mapOf(
            KEY_PAYMENT_ID to paymentId,
            KEY_AMOUNT to BigInteger.valueOf(amount),
            KEY_BALANCE to (BigInteger.valueOf(getMyBalance() - amount).toLong())
        )
        val block =  trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_DESTROY, transaction,
            recipient
        )
        Log.w("EuroTokenBlockDestroy", "Block made" )

        trustChainCommunity.sendBlock(block, peer)
        Log.w("EuroTokenBlockDestroy", "Sent to peer" )
        return block
    }

    fun getTransactions(): List<Transaction> {
        val mykey = trustChainCommunity.myPeer.publicKey.keyToBin()
        return trustChainCommunity.database.getLatestBlocks(mykey, 1000).filter {
                block: TrustChainBlock -> EUROTOKEN_TYPES.contains(block.type) }.map {
                block : TrustChainBlock -> val sender = defaultCryptoProvider.keyFromPublicBin(block.publicKey)
            Transaction(
                block,
                sender,
                defaultCryptoProvider.keyFromPublicBin(block.linkPublicKey),
                if (block.transaction.containsKey(KEY_AMOUNT)) { (block.transaction[KEY_AMOUNT] as BigInteger).toLong()} else 0L,
                block.type,
                getBlockBalanceChange(block) < 0,
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
                val change = getBlockBalanceChange(block)
                if (block.isProposal){
                    Log.w("EuroTokenBlockTransfer", "Proposal validating..." )
                    if (block.transaction[KEY_BALANCE] != balanceBefore + change) {
                        return false
                    }
                    Log.w("EuroTokenBlockTransfer", "Valid" )
                } else {
                    if (database.getLinked(block)?.transaction?.equals(block.transaction) != true) return false //TODO: crawl??
                }
                return true
            }
        })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_TRANSFER, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                Log.w("EuroTokenBlockTransfer", "sig request ${block.transaction}")
                //agree if validated
                trustChainCommunity.sendBlock(trustChainCommunity.createAgreementBlock(block, block.transaction))
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
                //if (!block.transaction.containsKey(KEY_BALANCE)) return false
                if (block.isProposal) {
                    if (!block.transaction.containsKey(KEY_AMOUNT)) return false
                    if (!block.transaction.containsKey(KEY_PAYMENT_ID)) return false
                    Log.w("EuroTokenBlockCreate", "Is valid proposal" )
                } else {
                    if (database.getLinked(block)?.transaction?.equals(block.transaction) != true) return false //TODO: crawl??
                }
                //TODO: validate gateway ID here
                return true
            }
        })

        trustChainCommunity.registerBlockSigner(BLOCK_TYPE_CREATE, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                Log.w("EuroTokenBlockCreate", "sig request")
                //only gateways should sign creations
                trustChainCommunity.sendBlock(trustChainCommunity.createAgreementBlock(block, block.transaction))
            }
        })

        trustChainCommunity.addListener(BLOCK_TYPE_CREATE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.w("EuroTokenBlockCreate", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })

    }
    fun addDestructionListners(){
        trustChainCommunity.registerTransactionValidator(BLOCK_TYPE_DESTROY, object : TransactionValidator {
            override fun validate(
                block: TrustChainBlock,
                database: TrustChainStore
            ): Boolean {
                if (block.isProposal) {
                    Log.w("EuroTokenBlockDestroy", "Validating..." )
                    if (!block.transaction.containsKey(KEY_BALANCE)) return false
                    if (!block.transaction.containsKey(KEY_AMOUNT)) return false
                    if (!block.transaction.containsKey(KEY_PAYMENT_ID)) return false
                    val balanceBefore = getBalanceForBlock(database.getBlockBefore(block))
                    if (block.transaction[KEY_BALANCE] != balanceBefore + getBlockBalanceChange(block))
                        return false
                    Log.w("EuroTokenBlockDestroy", "Valid" )
                    Log.w("EuroTokenBlockDestroy", "Acceptance" )
                } else {
                    Log.w("EuroTokenBlockDestroy", "Acceptance" )
                    //if (database.getLinked(block)?.transaction?.equals(block.transaction) != true) return false //TODO: crawl??
                }
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
                Log.d("EuroTokenBlockDestroy", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }

    fun addCheckpointListners(){
        trustChainCommunity.registerTransactionValidator(BLOCK_TYPE_CHECKPOINT, object : TransactionValidator {
            override fun validate(
                block: TrustChainBlock,
                database: TrustChainStore
            ): Boolean {
                if (block.isProposal) {
                    if (!block.transaction.containsKey(KEY_BALANCE)) return false
                    val balanceBefore = getBalanceForBlock(database.getBlockBefore(block))
                    if (block.transaction[KEY_BALANCE] != balanceBefore + getBlockBalanceChange(block))
                        return false
                } else {
                    Log.d("EuroTokenBlockCheck", "acceptance")
                    //if (database.getLinked(block)?.transaction?.equals(block.transaction) != true) return false //TODO: crawl??
                }
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
                Log.d("EuroTokenBlockCheck", "onBlockReceived: ${block.blockId} ${block.transaction}")
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
            return (if (amount <0 ) "-" else "") + "€" + (amount / 100).toString() + "," + (abs(amount) % 100).toString().padStart(2, '0')
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
