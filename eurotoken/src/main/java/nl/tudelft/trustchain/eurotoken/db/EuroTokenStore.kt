package nl.tudelft.trustchain.eurotoken.db

import android.content.Context
import nl.tudelft.eurotoken.sqldelight.Database
import nl.tudelft.trustchain.eurotoken.entity.Contact
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider

class EuroTokenStore(context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "eurotoken.db")
    private val database = Database(driver)

    fun addContact(publicKey: PublicKey, name: String) {
        database.dbContactQueries.addContact(name, publicKey.keyToBin())
    }

    fun getContacts(): List<Contact> {
        return database.dbContactQueries.getAll { name, public_key ->
            val publicKey = defaultCryptoProvider.keyFromPublicBin(public_key)
            Contact(name, publicKey)
        }.executeAsList()
    }

    fun deleteContact(contact: Contact) {
        database.dbContactQueries.deleteContact(contact.publicKey.keyToBin())
    }

    companion object {
        private lateinit var instance: EuroTokenStore
        fun getInstance(context: Context): EuroTokenStore {
            if (!::instance.isInitialized) {
                instance = EuroTokenStore(context)
            }
            return instance
        }
    }
}
