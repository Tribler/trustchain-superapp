package nl.tudelft.trustchain.valuetransfer.ui.contacts

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.ContactImage
import nl.tudelft.trustchain.peerchat.entity.ContactState

data class ChatItem(
    val contact: Contact,
    val lastMessage: ChatMessage?,
    val isOnline: Boolean,
    val isBluetooth: Boolean,
    val state: ContactState?,
    val image: ContactImage?
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is ChatItem && contact.mid == other.contact.mid
    }
}
