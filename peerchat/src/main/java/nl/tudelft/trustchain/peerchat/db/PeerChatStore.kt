package nl.tudelft.trustchain.peerchat.db

import android.content.Context
import android.util.Log
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.peerchat.sqldelight.Database
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.Contact
import java.util.*

class PeerChatStore(context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "peerchat.db")
    private val database = Database(driver)

    private val messageMapper = {
            id: String,
            message: String,
            senderPk: ByteArray,
            receipientPk: ByteArray,
            outgoing: Long,
            timestamp: Long,
            ack: Long,
            read: Long ->
        ChatMessage(
            id,
            message,
            defaultCryptoProvider.keyFromPublicBin(senderPk),
            defaultCryptoProvider.keyFromPublicBin(receipientPk),
            outgoing == 1L,
            Date(timestamp),
            ack == 1L,
            read == 1L
        )
    }

    fun addContact(publicKey: PublicKey, name: String) {
        Log.d("AddContact", "save contact $name $publicKey")
        database.dbContactQueries.addContact(name, publicKey.keyToBin())
    }

    fun getContacts(): Flow<List<Contact>> {
        return database.dbContactQueries.getAll { name, public_key ->
            val publicKey = defaultCryptoProvider.keyFromPublicBin(public_key)
            Contact(name, publicKey)
        }.asFlow().mapToList()
    }

    private fun getAllMessages(): Flow<List<ChatMessage>> {
        return database.dbMessageQueries.getAll(messageMapper)
            .asFlow().mapToList()
    }

    fun getUnacknowledgedMessages(): List<ChatMessage> {
        return database.dbMessageQueries.getUnacknowledgedMessages(messageMapper).executeAsList()
    }

    fun getContactsWithLastMessages(): Flow<List<Pair<Contact, ChatMessage?>>> {
        return combine(getContacts(), getAllMessages()) { contacts, messages ->
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
        database.dbMessageQueries.addMessage(message.id,
            message.message,
            message.sender.keyToBin(),
            message.recipient.keyToBin(),
            if (message.outgoing) 1L else 0L,
            message.timestamp.time,
            if (message.ack) 1L else 0L,
            if (message.read) 1L else 0L
        )
    }

    fun ackMessage(id: String) {
        database.dbMessageQueries.ackMessage(id)
    }

    fun getAllByPublicKey(publicKey: PublicKey): Flow<List<ChatMessage>> {
        val publicKeyBin = publicKey.keyToBin()
        return database.dbMessageQueries.getAllByPublicKey(publicKeyBin, publicKeyBin, messageMapper)
            .asFlow().mapToList()
    }

    fun deleteContact(contact: Contact) {
        database.dbContactQueries.deleteContact(contact.publicKey.keyToBin())
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
