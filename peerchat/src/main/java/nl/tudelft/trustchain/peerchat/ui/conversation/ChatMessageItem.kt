package nl.tudelft.trustchain.peerchat.ui.conversation

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.peerchat.entity.ChatMessage

data class ChatMessageItem(
    val chatMessage: ChatMessage,
    val shouldShowAvatar: Boolean,
    val shouldShowDate: Boolean,
    val participantName: String
): Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is ChatMessageItem && other.chatMessage.id == chatMessage.id
    }
}
