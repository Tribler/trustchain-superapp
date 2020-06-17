package nl.tudelft.trustchain.peerchat.ui.feed

import kotlinx.coroutines.flow.first
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.Contact

class PostRepository(
    private val trustChainCommunity: TrustChainCommunity,
    private val peerChatStore: PeerChatStore
) {
    fun createPost(text: String): TrustChainBlock {
        val transaction = mapOf(
            KEY_TEXT to text
        )
        return trustChainCommunity.createProposalBlock(BLOCK_TYPE_POST, transaction,
            ANY_COUNTERPARTY_PK)
    }

    fun createReply(blockHash: ByteArray, text: String): TrustChainBlock? {
        val transaction = mapOf(
            KEY_TEXT to text
        )
        val block = trustChainCommunity.database.getBlockWithHash(blockHash)
        return if (block != null) {
            trustChainCommunity.createAgreementBlock(block, transaction)
        } else {
            null
        }
    }

    fun likePost(block: TrustChainBlock) {
        val likeBlock = LikeBlockBuilder(trustChainCommunity.myPeer,
            trustChainCommunity.database, block).sign()
        trustChainCommunity.onBlockCreated(likeBlock)
    }

    suspend fun getPostsByFriends(): List<PostItem> {
        val myPeer = IPv8Android.getInstance().myPeer
        val myContact = Contact("You", myPeer.publicKey)
        val contacts = peerChatStore.getContacts().first()
        val blocks = trustChainCommunity.database
            .getBlocksWithType(BLOCK_TYPE_POST)
            .sortedByDescending { it.insertTime }
        return blocks.map { block ->
            val contact = if (block.publicKey.contentEquals(myPeer.publicKey.keyToBin()))
                myContact
            else contacts.find {
                it.publicKey.keyToBin().contentEquals(block.publicKey)
            }
            PostItem(block, contact)
        }
    }

    companion object {
        private const val BLOCK_TYPE_POST = "post"
        private const val BLOCK_TYPE_LIKE = "like"
        const val KEY_TEXT = "text"
    }

    class LikeBlockBuilder(
        myPeer: Peer,
        database: TrustChainStore,
        private val link: TrustChainBlock
    ) : BlockBuilder(myPeer, database) {
        override fun update(builder: TrustChainBlock.Builder) {
            builder.type = BLOCK_TYPE_LIKE
            builder.rawTransaction = TransactionEncoding.encode(mapOf<Nothing, Nothing>())
            builder.linkPublicKey = link.publicKey
            builder.linkSequenceNumber = link.sequenceNumber
        }
    }
}
