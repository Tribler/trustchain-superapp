package nl.tudelft.trustchain.valuetransfer.ui.contacts

import com.mattskala.itemadapter.Item
import java.io.File
import java.util.*

data class ChatMediaItem(
    val messageID: String,
    val senderName: String,
    val sendDate: Date,
    val type: String,
    val file: File,
    val fileName: String?
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is ChatMediaItem && other.messageID == messageID
    }
}
