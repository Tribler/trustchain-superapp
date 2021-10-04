package nl.tudelft.trustchain.peerchat.db

import android.content.Context
import android.util.Log
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.peerchat.sqldelight.Database
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.ContactState
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

//    private val archiveMapper = {
//        publicKey: ByteArray -> publicKey
//    }

    fun getAllMessages(): Flow<List<ChatMessage>> {
        return database.dbMessageQueries.getAll(messageMapper)
            .asFlow().mapToList()
    }

    fun getMessageById(id: String): ChatMessage? {
        return database.dbMessageQueries.getMessageById(id, messageMapper).executeAsOneOrNull()
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

    fun getLastMessages(isRecent: Boolean, isArchive: Boolean, isBlocked: Boolean): Flow<List<ChatMessage>> {
        return combine(contactsStore.getContacts(), getAllMessages(), getAllContactState()) { _, messages, state ->
            messages
                .asSequence()
                .sortedByDescending {
                    it.timestamp.time
                }
                .distinctBy {
                    if (it.outgoing) it.recipient else it.sender
                }
                .filter { message ->
                    state.filter {
                        (it.publicKey == if (message.outgoing) message.recipient else message.sender)
                    }.let { stateOfContact ->
                        when {
                            isRecent -> stateOfContact.isEmpty() || stateOfContact.any { !it.isArchived && !it.isBlocked }
                            isArchive -> stateOfContact.any { it.isArchived && !it.isBlocked }
                            isBlocked -> stateOfContact.any { it.isBlocked }
                            else -> false
                        }
//                        (isArchive && stateOfContact.any { it.isArchived }) || (!isArchive && (stateOfContact.isEmpty() || stateOfContact.any { !it.isArchived }))
                    }
                }
                .toList()
        }
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

    fun getAllSentByPublicKeyToMe(publicKey: PublicKey): List<ChatMessage> {
        return database.dbMessageQueries.getAllByPublicKey(
            publicKey.keyToBin(),
            publicKey.keyToBin(),
            messageMapper
        ).executeAsList()
    }

    fun deleteMessagesOfPublicKey(publicKey: PublicKey) {
        database.dbMessageQueries.deleteMessagesOfPublicKey(publicKey.keyToBin(), publicKey.keyToBin())
    }

    // Chat contact options
    fun createContactStateTable() {
        database.dbContactStateQueries.createContactStateTable()
    }

    fun setState(publicKey: PublicKey, type: String, status: Boolean) {
        val state = if (status) 1L else 0L

        if (database.dbContactStateQueries.hasContact(publicKey.keyToBin()).executeAsOneOrNull() != null) {
            when (type) {
                STATUS_ARCHIVE -> database.dbContactStateQueries.setArchiveState(state, publicKey.keyToBin())
                STATUS_MUTE -> database.dbContactStateQueries.setMuteState(state, publicKey.keyToBin())
                STATUS_BLOCK -> database.dbContactStateQueries.setBlockState(state, publicKey.keyToBin())
            }
        } else {
            when (type) {
                STATUS_ARCHIVE -> database.dbContactStateQueries.addContact(publicKey.keyToBin(), state, 0L, 0L)
                STATUS_MUTE -> database.dbContactStateQueries.addContact(publicKey.keyToBin(), 0L, state, 0L)
                STATUS_BLOCK -> database.dbContactStateQueries.addContact(publicKey.keyToBin(), 0L, 0L, state)
            }
        }
    }

    fun getContactState(publicKey: PublicKey): ContactState? {
        val contactState = database.dbContactStateQueries.getOne(publicKey.keyToBin()).executeAsOneOrNull()

        return if (contactState != null) {
            ContactState(
                defaultCryptoProvider.keyFromPublicBin(contactState.public_key),
                contactState.is_archived == 1L,
                contactState.is_muted == 1L,
                contactState.is_blocked == 1L
            )
        } else null
    }

    fun getContactStateForType(publicKey: PublicKey, type: String): Boolean {
        return when (type) {
            STATUS_ARCHIVE -> database.dbContactStateQueries.getArchiveState(publicKey.keyToBin()).executeAsOneOrNull()
            STATUS_MUTE -> database.dbContactStateQueries.getMuteState(publicKey.keyToBin()).executeAsOneOrNull()
            STATUS_BLOCK -> database.dbContactStateQueries.getBlockState(publicKey.keyToBin()).executeAsOneOrNull()
            else -> null
        } == 1L
    }

    fun getAllContactState(): Flow<List<ContactState>> {
        return database.dbContactStateQueries.getAll { public_key, is_archived, is_muted, is_blocked ->
            val publicKey = defaultCryptoProvider.keyFromPublicBin(public_key)
            ContactState(publicKey, is_archived == 1L, is_muted == 1L, is_blocked == 1L)
        }.asFlow().mapToList()
    }


    fun removeContactState(publicKey: PublicKey) {
        database.dbContactStateQueries.removeContact(publicKey.keyToBin())
    }

    companion object {
        const val STATUS_ARCHIVE = "status_archive"
        const val STATUS_MUTE = "status_mute"
        const val STATUS_BLOCK = "status_block"

        private lateinit var instance: PeerChatStore
        fun getInstance(context: Context): PeerChatStore {
            if (!::instance.isInitialized) {
                instance = PeerChatStore(context)
            }
            return instance
        }
    }
}
