package nl.tudelft.trustchain.valuetransfer.ui.contacts

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import nl.tudelft.trustchain.peerchat.entity.ChatMessage

data class ContactChatItem(
    val chatMessage: ChatMessage,
    val transaction: TrustChainBlock?,
    val blocks: List<TrustChainBlock>,
    val loadMoreMessages: Boolean,
    val shouldShowDate: Boolean,
    val transactionIsReceived: Boolean,
    val attachtmentTransferProgress: TransferProgress?
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is ContactChatItem && other.chatMessage.id == chatMessage.id
    }
}
