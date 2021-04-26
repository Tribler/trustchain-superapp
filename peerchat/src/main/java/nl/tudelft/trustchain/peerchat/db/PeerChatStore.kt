package nl.tudelft.trustchain.peerchat.db

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.peerchat.sqldelight.Database
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import java.util.*

class PeerChatStore(context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "peerchat.db")
    private val database = Database(driver)
    val contactsStore = ContactStore.getInstance(context)

    private val messageMapper = { id: String,
        message: String,
        senderPk: ByteArray,
        receipientPk: ByteArray,
        outgoing: Long,
        timestamp: Long,
        ack: Long,
        read: Long, attachmentType: String?,
        attachmentSize: Long?,
        attachmentContent: ByteArray?,
        attachmentFetched: Long,
        transaction_hash: ByteArray?
        ->
        ChatMessage(
            id,
            message,
            if (attachmentType != null && attachmentSize != null && attachmentContent != null)
                MessageAttachment(
                    attachmentType,
                    attachmentSize,
                    attachmentContent
                ) else null,
            defaultCryptoProvider.keyFromPublicBin(senderPk),
            defaultCryptoProvider.keyFromPublicBin(receipientPk),
            outgoing == 1L,
            Date(timestamp),
            ack == 1L,
            read == 1L,
            attachmentFetched == 1L,
            transactionHash = transaction_hash
        )
    }

    private fun getAllMessages(): Flow<List<ChatMessage>> {
        return database.dbMessageQueries.getAll(messageMapper)
            .asFlow().mapToList()
    }

    fun getUnacknowledgedMessages(): List<ChatMessage> {
        return database.dbMessageQueries.getUnacknowledgedMessages(messageMapper).executeAsList()
    }

    fun getUnfetchedAttachments(): List<ChatMessage> {
        return database.dbMessageQueries.getUnfetchedAttachments(messageMapper).executeAsList()
    }

    fun setAttachmentFetched(id: String) {
        database.dbMessageQueries.setAttachmentFetched(id.hexToBytes())
    }

    fun getContactsWithLastMessages(): Flow<List<Pair<Contact, ChatMessage?>>> {
        return combine(contactsStore.getContacts(), getAllMessages()) { contacts, messages ->
            val notContacts = messages
                .asSequence()
                .filter { !it.outgoing }
                .map { it.sender }
                .distinct()
                .filter { publicKey -> contacts.find { it.publicKey == publicKey } == null }
                .map { Contact("", it) }
                .toList()

            (contacts + notContacts).map { contact ->
                val lastMessage = messages.findLast {
                    it.recipient == contact.publicKey || it.sender == contact.publicKey
                }
                Pair(contact, lastMessage)
            }
        }
    }

    fun addMessage(message: ChatMessage) {
        database.dbMessageQueries.addMessage(
            message.id,
            message.message,
            message.sender.keyToBin(),
            message.recipient.keyToBin(),
            if (message.outgoing) 1L else 0L,
            message.timestamp.time,
            if (message.ack) 1L else 0L,
            if (message.read) 1L else 0L,
            message.attachment?.type,
            message.attachment?.size,
            message.attachment?.content,
            if (message.attachmentFetched) 1L else 0L,
            message.transactionHash
        )
    }

    fun ackMessage(id: String) {
        database.dbMessageQueries.ackMessage(id)
    }

    fun getAllByPublicKey(publicKey: PublicKey): Flow<List<ChatMessage>> {
        val publicKeyBin = publicKey.keyToBin()
        return database.dbMessageQueries.getAllByPublicKey(
            publicKeyBin,
            publicKeyBin,
            messageMapper
        )
            .asFlow().mapToList()
    }

    companion object {
        private lateinit var instance: PeerChatStore
        fun getInstance(context: Context): PeerChatStore {
            if (!::instance.isInitialized) {
                instance = PeerChatStore(context)
            }
            return instance
        }
    }
}
