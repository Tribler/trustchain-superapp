package nl.tudelft.trustchain.peerchat

import com.mattskala.itemadapter.Item
import java.util.*

data class ContactItem(
    val contact: Contact,
    val lastMessage: String?,
    val lastMessageDate: Date?
) : Item()
