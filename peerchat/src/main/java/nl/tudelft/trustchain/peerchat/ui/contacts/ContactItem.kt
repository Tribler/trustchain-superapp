package nl.tudelft.trustchain.peerchat.ui.contacts

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.Contact
import java.util.*

data class ContactItem(
    val contact: Contact,
    val lastMessage: ChatMessage?,
    val isOnline: Boolean,
    val isBluetooth: Boolean
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is ContactItem && contact.mid == other.contact.mid
    }
}
