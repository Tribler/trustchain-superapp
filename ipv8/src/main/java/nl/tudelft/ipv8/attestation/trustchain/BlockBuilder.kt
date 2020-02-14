package nl.tudelft.ipv8.attestation.trustchain

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.keyvault.PrivateKey

abstract class BlockBuilder(
    protected val myPeer: Peer,
    private val database: TrustChainStore
) {
    protected abstract fun update(builder: TrustChainBlock.Builder)

    fun sign(): TrustChainBlock {
        val builder = TrustChainBlock.Builder()

        update(builder)

        val prevBlock = database.getLatest(myPeer.publicKey.keyToBin())
        if (prevBlock != null) {
            builder.sequenceNumber = prevBlock.sequenceNumber + 1u
            builder.previousHash = prevBlock.calculateHash()
        } else {
            // Genesis block
            builder.sequenceNumber = GENESIS_SEQ
            builder.previousHash = GENESIS_HASH
        }

        builder.publicKey = myPeer.publicKey.keyToBin()
        builder.signature = EMPTY_SIG

        val block = builder.build()
        block.sign(myPeer.key as PrivateKey)

        return block
    }
}

class ProposalBlockBuilder(
    myPeer: Peer,
    database: TrustChainStore,
    private val blockType: String,
    private val transaction: TrustChainTransaction,
    private val publicKey: ByteArray
) : BlockBuilder(myPeer, database) {
    override fun update(builder: TrustChainBlock.Builder) {
        builder.type = blockType
        builder.rawTransaction = TransactionEncoding.encode(transaction)
        builder.linkPublicKey = publicKey
        builder.linkSequenceNumber = UNKNOWN_SEQ
    }
}

class AgreementBlockBuilder(
    myPeer: Peer,
    database: TrustChainStore,
    private val link: TrustChainBlock,
    private val transaction: TrustChainTransaction
) : BlockBuilder(myPeer, database) {
    override fun update(builder: TrustChainBlock.Builder) {
        builder.type = link.type
        builder.rawTransaction = TransactionEncoding.encode(transaction)
        builder.linkPublicKey = link.publicKey
        builder.linkSequenceNumber = link.sequenceNumber
    }
}
