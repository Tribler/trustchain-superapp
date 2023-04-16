package nl.tudelft.trustchain.common.contacts

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import nl.tudelft.common.sqldelight.Database
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider

class ContactStore(context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "common.db")
    private val database = Database(driver)

    fun addContact(publicKey: PublicKey, name: String) {
        database.dbContactQueries.addContact(name, publicKey.keyToBin())
    }

    fun updateContact(publicKey: PublicKey, name: String) {
        database.dbContactQueries.addContact(name, publicKey.keyToBin())
    }

    fun getContactFromPublicKey(publicKey: PublicKey): Contact? {
        val contact =
            database.dbContactQueries.getContact(publicKey.keyToBin()).executeAsOneOrNull()
        return if (contact != null) {
            Contact(
                contact.name,
                defaultCryptoProvider.keyFromPublicBin(contact.public_key)
            )
        } else null
    }

    fun getContactFromPublickey(publicKey: PublicKey): Flow<Contact?> {
        return database.dbContactQueries.getContact(publicKey.keyToBin()) { name, publicKeyDB ->
            Contact(
                name,
                defaultCryptoProvider.keyFromPublicBin(publicKeyDB)
            )
        }.asFlow().mapToOneOrNull()
    }

    fun getContacts(): Flow<List<Contact>> {
        return database.dbContactQueries.getAll { name: String, publicKeyDB: ByteArray ->
            val publicKey = defaultCryptoProvider.keyFromPublicBin(publicKeyDB)
            Contact(name, publicKey)
        }.asFlow().mapToList()
    }

    fun deleteContact(contact: Contact) {
        database.dbContactQueries.deleteContact(contact.publicKey.keyToBin())
    }

    companion object {
        private lateinit var instance: ContactStore
        fun getInstance(context: Context): ContactStore {
            if (!Companion::instance.isInitialized) {
                instance = ContactStore(context)
            }
            return instance
        }
    }
}
