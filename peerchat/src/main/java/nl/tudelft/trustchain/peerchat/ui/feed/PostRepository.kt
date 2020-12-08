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

    fun likePost(block: TrustChainBlock): Boolean {
        return if (!hasLikedPost(block)) {
            LikeBlockBuilder(
                trustChainCommunity.myPeer,
                trustChainCommunity.database, block
            ).sign()
//            trustChainCommunity.onBlockCreated(likeBlock) TODO create a public method for this
            true
        } else {
            false
        }
    }

    /**
     * Returns true if the current user has liked the post.
     */
    fun hasLikedPost(block: TrustChainBlock): Boolean {
        val myPeer = IPv8Android.getInstance().myPeer
        val linkedBlocks = trustChainCommunity.database
            .getAllLinked(block)
        return linkedBlocks.find {
            it.type == BLOCK_TYPE_LIKE &&
            it.publicKey.contentEquals(myPeer.publicKey.keyToBin())
        } != null
    }

    private fun findContactByPublicKey(contacts: List<Contact>, publicKeyBin: ByteArray): Contact? {
        val myPeer = IPv8Android.getInstance().myPeer
        val myContact = Contact("You", myPeer.publicKey)
        return if (publicKeyBin.contentEquals(myPeer.publicKey.keyToBin()))
            myContact
        else contacts.find {
            it.publicKey.keyToBin().contentEquals(publicKeyBin)
        }
    }

    suspend fun getPostsByFriends(): List<PostItem> {
        val myPeer = IPv8Android.getInstance().myPeer
        val contacts = peerChatStore.getContacts().first()
        val posts = trustChainCommunity.database
            .getBlocksWithType(BLOCK_TYPE_POST)
            .sortedByDescending { it.insertTime }
        val likes = trustChainCommunity.database
            .getBlocksWithType(BLOCK_TYPE_LIKE)
            .sortedByDescending { it.insertTime }
        return posts.map { post ->
            val contact = findContactByPublicKey(contacts, post.publicKey)
            val replies = posts.filter { it.linkedBlockId == post.blockId }
            val postLikes = likes.filter { it.linkedBlockId == post.blockId }
            val liked = postLikes.find {
                it.publicKey.contentEquals(myPeer.publicKey.keyToBin())
            } != null
            val linkedBlock = posts.find { it.blockId == post.linkedBlockId }
            val linkedContact = findContactByPublicKey(contacts, post.linkPublicKey)
            PostItem(post, contact, linkedBlock, linkedContact, replies, postLikes, liked)
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
