package nl.tudelft.trustchain.peerchat.db

import android.content.Context
import android.graphics.BitmapFactory
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.peerchat.sqldelight.Database
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityInfo
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.ContactImage
import nl.tudelft.trustchain.peerchat.entity.ContactState
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import java.util.*

class PeerChatStore(context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "peerchat.db")
    private val database = Database(driver)
    val contactsStore = ContactStore.getInstance(context)

    private val messageMapper =
        { id: String, message: String, senderPk: ByteArray, receipientPk: ByteArray, outgoing: Long, timestamp: Long, ack: Long, read: Long, attachmentType: String?, attachmentSize: Long?, attachmentContent: ByteArray?, attachmentFetched: Long, transaction_hash: ByteArray? ->
            ChatMessage(
                id,
                message,
                if (attachmentType != null && attachmentSize != null && attachmentContent != null) MessageAttachment(
                    attachmentType, attachmentSize, attachmentContent
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

    fun getAllMessages(): Flow<List<ChatMessage>> {
        return database.dbMessageQueries.getAll(messageMapper).asFlow().mapToList()
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

    fun getMessageByTransactionHash(hash: ByteArray): ChatMessage? {
        return database.dbMessageQueries.getMessageByTransactionHash(hash, messageMapper)
            .executeAsOneOrNull()
    }

    /**
     * Get the list of last messages by public key, filtered if archived, blocked or not
     */
    fun getLastMessages(
        isRecent: Boolean,
        isArchive: Boolean,
        isBlocked: Boolean
    ): Flow<List<ChatMessage>> {
        return combine(
            contactsStore.getContacts(), getAllMessages(), getAllContactState()
        ) { _, messages, state ->
            messages.asSequence().sortedByDescending {
                it.timestamp.time
            }.distinctBy {
                if (it.outgoing) it.recipient else it.sender
            }.filter { message ->
                state.filter {
                    (it.publicKey == if (message.outgoing) message.recipient else message.sender)
                }.let { stateOfContact ->
                    when {
                        isRecent -> stateOfContact.isEmpty() || stateOfContact.any { !it.isArchived && !it.isBlocked }
                        isArchive -> stateOfContact.any { it.isArchived && !it.isBlocked }
                        isBlocked -> stateOfContact.any { it.isBlocked }
                        else -> false
                    }
                }
            }.toList()
        }
    }

    fun getContactsWithLastMessages(): Flow<List<Pair<Contact, ChatMessage?>>> {
        return combine(contactsStore.getContacts(), getAllMessages()) { contacts, messages ->
            val notContacts =
                messages.asSequence().filter { !it.outgoing }.map { it.sender }.distinct()
                    .filter { publicKey -> contacts.find { it.publicKey == publicKey } == null }
                    .map { Contact("", it) }.toList()

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
            publicKeyBin, publicKeyBin, messageMapper
        ).asFlow().mapToList()
    }

    fun getAllSentByPublicKeyToMe(publicKey: PublicKey): List<ChatMessage> {
        return database.dbMessageQueries.getAllByPublicKey(
            publicKey.keyToBin(), publicKey.keyToBin(), messageMapper
        ).executeAsList()
    }

    fun deleteMessagesOfPublicKey(publicKey: PublicKey) {
        database.dbMessageQueries.deleteMessagesOfPublicKey(
            publicKey.keyToBin(), publicKey.keyToBin()
        )
    }

    // Get all attachments of type sent by contact
    fun getAttachmentsOfType(publicKey: PublicKey, type: String): Flow<List<MessageAttachment>> {
        return combine(contactsStore.getContacts(), getAllByPublicKey(publicKey)) { _, messages ->
            messages.asSequence().filter {
                it.attachment != null && it.attachment.type == type
            }.sortedByDescending {
                it.timestamp.time
            }.map {
                MessageAttachment(
                    it.attachment!!.type, it.attachment.size, it.attachment.content
                )
            }.toList()
        }
    }

    // Chat contact image
    fun createContactImageTable() {
        database.dbContactImageQueries.createContactImageTable()
    }

    private val contactImageMapper = {
            public_key: ByteArray,
            image_hash: String?,
            image: ByteArray?,
        ->
        ContactImage(
            defaultCryptoProvider.keyFromPublicBin(public_key),
            image_hash,
            if (image != null) BitmapFactory.decodeByteArray(image, 0, image.size) else null
        )
    }

    fun getAllContactImages(): Flow<List<ContactImage>> {
        return database.dbContactImageQueries.getAll(contactImageMapper).asFlow().mapToList()
    }

    fun getContactImage(publicKey: PublicKey): ContactImage? {
        return database.dbContactImageQueries.getContactImage(
            publicKey.keyToBin(), contactImageMapper
        ).executeAsOneOrNull()
    }

    fun getContactImageFlow(publicKey: PublicKey): Flow<ContactImage?> {
        return database.dbContactImageQueries.getContactImage(
            publicKey.keyToBin(), contactImageMapper
        ).asFlow().mapToOneOrNull()
    }

    fun getContactImageHash(publicKey: PublicKey): String? {
        return database.dbContactImageQueries.getContactImage(
            publicKey.keyToBin(), contactImageMapper
        ).executeAsOneOrNull()?.imageHash
    }

    fun hasContactImage(publicKey: PublicKey): Boolean {
        return database.dbContactImageQueries.getContactImage(publicKey.keyToBin())
            .executeAsOneOrNull() != null
    }

    fun setContactImage(publicKey: PublicKey, imageBytes: ByteArray, hash: String) {
        if (hasContactImage(publicKey)) {
            database.dbContactImageQueries.setContactImage(hash, imageBytes, publicKey.keyToBin())
        } else {
            database.dbContactImageQueries.addContactImage(publicKey.keyToBin(), hash, imageBytes)
        }
    }

    fun removeContactImage(publicKey: PublicKey) {
        database.dbContactImageQueries.deleteContactImage(publicKey.keyToBin())
    }

    // Chat contact options
    fun createContactStateTable() {
        database.dbContactStateQueries.createContactStateTable()
    }

    fun setState(publicKey: PublicKey, type: String, status: Boolean, value: String? = null) {
        val state = if (status) 1L else 0L
        val publicKeyBin = publicKey.keyToBin()

        if (database.dbContactStateQueries.hasContact(publicKeyBin).executeAsOneOrNull() != null) {
            when (type) {
                STATUS_ARCHIVE -> database.dbContactStateQueries.setArchiveState(
                    state, publicKeyBin
                )
                STATUS_MUTE -> database.dbContactStateQueries.setMuteState(state, publicKeyBin)
                STATUS_BLOCK -> database.dbContactStateQueries.setBlockState(state, publicKeyBin)
                STATUS_VERIFICATION -> database.dbContactStateQueries.setVerificationState(
                    state, publicKeyBin
                )
                STATUS_IMAGE_HASH -> database.dbContactStateQueries.setImageHash(
                    value, publicKeyBin
                )
                STATUS_INITIALS -> database.dbContactStateQueries.setInitials(
                    value, publicKeyBin
                )
                STATUS_SURNAME -> database.dbContactStateQueries.setSurname(value, publicKeyBin)
            }
        } else {
            when (type) {
                STATUS_ARCHIVE -> database.dbContactStateQueries.addContact(
                    publicKeyBin, state, 0L, 0L, 0L, null, null, null
                )
                STATUS_MUTE -> database.dbContactStateQueries.addContact(
                    publicKeyBin, 0L, state, 0L, 0L, null, null, null
                )
                STATUS_BLOCK -> database.dbContactStateQueries.addContact(
                    publicKeyBin, 0L, 0L, state, 0L, null, null, null
                )
                STATUS_VERIFICATION -> database.dbContactStateQueries.addContact(
                    publicKeyBin, 0L, 0L, 0L, state, null, null, null
                )
                STATUS_IMAGE_HASH -> database.dbContactStateQueries.addContact(
                    publicKeyBin, 0L, 0L, 0L, 0L, value, null, null
                )
                STATUS_INITIALS -> database.dbContactStateQueries.addContact(
                    publicKeyBin, 0L, 0L, 0L, 0L, null, value, null
                )
                STATUS_SURNAME -> database.dbContactStateQueries.addContact(
                    publicKeyBin, 0L, 0L, 0L, 0L, null, null, value
                )
            }
        }
    }

    fun setIdentityState(publicKey: PublicKey, identityInfo: IdentityInfo) {
        if (getContactState(publicKey) != null) {
            database.dbContactStateQueries.updateContactIdentity(
                if (identityInfo.isVerified) 1L else 0L,
                identityInfo.imageHash,
                identityInfo.initials,
                identityInfo.surname,
                publicKey.keyToBin()
            )
        } else {
            database.dbContactStateQueries.addContact(
                publicKey.keyToBin(),
                0L,
                0L,
                0L,
                if (identityInfo.isVerified) 1L else 0L,
                identityInfo.imageHash,
                identityInfo.initials,
                identityInfo.surname
            )
        }
    }

    fun getContactState(publicKey: PublicKey): ContactState? {
        return database.dbContactStateQueries.getOne(publicKey.keyToBin()).executeAsOneOrNull()
            ?.let { contactState ->
                ContactState(
                    defaultCryptoProvider.keyFromPublicBin(contactState.public_key),
                    contactState.is_archived == 1L,
                    contactState.is_muted == 1L,
                    contactState.is_blocked == 1L,
                    IdentityInfo(
                        contactState.initials,
                        contactState.surname,
                        contactState.is_verified == 1L,
                        contactState.image_hash ?: ""
                    )
                )
            }
    }

    private val contactStateMapper =
        { public_key: ByteArray, is_archived: Long, is_muted: Long, is_blocked: Long, is_verified: Long, image_hash: String?, initials: String?, surname: String? ->
            ContactState(
                defaultCryptoProvider.keyFromPublicBin(public_key),
                is_archived == 1L,
                is_muted == 1L,
                is_blocked == 1L,
                IdentityInfo(
                    initials, surname, is_verified == 1L, image_hash
                )
            )
        }

    fun getContactStateFlow(publicKey: PublicKey): Flow<ContactState?> {
        return database.dbContactStateQueries.getOne(publicKey.keyToBin(), contactStateMapper)
            .asFlow().mapToOneOrNull()
    }

    fun getContactStateForType(publicKey: PublicKey, type: String): Boolean {
        val publicKeyBytes = publicKey.keyToBin()
        return when (type) {
            STATUS_ARCHIVE -> database.dbContactStateQueries.getArchiveState(publicKeyBytes)
                .executeAsOneOrNull()
            STATUS_MUTE -> database.dbContactStateQueries.getMuteState(publicKeyBytes)
                .executeAsOneOrNull()
            STATUS_BLOCK -> database.dbContactStateQueries.getBlockState(publicKeyBytes)
                .executeAsOneOrNull()
            STATUS_VERIFICATION -> database.dbContactStateQueries.getVerificationState(
                publicKeyBytes
            ).executeAsOneOrNull()
            else -> null
        } == 1L
    }

    fun getContactStateValueForType(publicKey: PublicKey, type: String): String? {
        val publicKeyBytes = publicKey.keyToBin()
        return when (type) {
            STATUS_IMAGE_HASH -> database.dbContactStateQueries.getImageHash(publicKeyBytes)
                .executeAsOneOrNull()?.image_hash
            STATUS_INITIALS -> database.dbContactStateQueries.getInitials(publicKeyBytes)
                .executeAsOneOrNull()?.initials
            STATUS_SURNAME -> database.dbContactStateQueries.getSurname(publicKeyBytes)
                .executeAsOneOrNull()?.surname
            else -> null
        }
    }

    fun getAllContactState(): Flow<List<ContactState>> {
        return database.dbContactStateQueries.getAll { public_key, is_archived, is_muted, is_blocked, is_verified, image_hash, initials, surname ->
            val publicKey = defaultCryptoProvider.keyFromPublicBin(public_key)
            ContactState(
                publicKey, is_archived == 1L, is_muted == 1L, is_blocked == 1L,
                IdentityInfo(
                    initials, surname, is_verified == 1L, image_hash
                )
            )
        }.asFlow().mapToList()
    }

    fun removeContactState(publicKey: PublicKey) {
        database.dbContactStateQueries.removeContact(publicKey.keyToBin())
    }

    companion object {
        const val STATUS_ARCHIVE = "status_archive"
        const val STATUS_MUTE = "status_mute"
        const val STATUS_BLOCK = "status_block"
        const val STATUS_VERIFICATION = "status_verification"
        const val STATUS_IMAGE_HASH = "status_image_hash"
        const val STATUS_INITIALS = "status_initials"
        const val STATUS_SURNAME = "status_surname"

        private lateinit var instance: PeerChatStore
        fun getInstance(context: Context): PeerChatStore {
            if (!::instance.isInitialized) {
                instance = PeerChatStore(context)
            }
            return instance
        }
    }
}
