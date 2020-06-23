package nl.tudelft.trustchain.peerchat.ui.feed

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.Contact
import java.util.*

data class PostItem(
    val block: TrustChainBlock,
    val contact: Contact?,
    val linkedBlock: TrustChainBlock?,
    val linkedContact: Contact?,
    val replies: List<TrustChainBlock>,
    val likes: List<TrustChainBlock>,
    /**
     * True if I liked this post.
     */
    val liked: Boolean
) : Item()
