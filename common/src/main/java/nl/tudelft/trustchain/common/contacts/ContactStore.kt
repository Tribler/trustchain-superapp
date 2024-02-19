package nl.tudelft.trustchain.common.contacts

import android.content.Context
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import nl.tudelft.common.sqldelight.Database
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider

class ContactStore(context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "common.db")
    private val database = Database(driver)

    fun addContact(
        publicKey: PublicKey,
        name: String
    ) {
        database.dbContactQueries.addContact(name, publicKey.keyToBin())
    }

    fun updateContact(
        publicKey: PublicKey,
        name: String
    ) {
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
        } else {
            null
        }
    }

    fun getContactFromPublickey(publicKey: PublicKey): Flow<Contact?> {
        return database.dbContactQueries.getContact(publicKey.keyToBin()) { name, public_key ->
            Contact(
                name,
                defaultCryptoProvider.keyFromPublicBin(public_key)
            )
        }.asFlow().mapToOneOrNull(Dispatchers.IO)
    }

    fun getContacts(): Flow<List<Contact>> {
        return database.dbContactQueries.getAll { name, public_key ->
            val publicKey = defaultCryptoProvider.keyFromPublicBin(public_key)
            Contact(name, publicKey)
        }.asFlow().mapToList(Dispatchers.IO)
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
