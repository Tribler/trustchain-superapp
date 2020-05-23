package nl.tudelft.trustchain.peerchat.db

import android.content.Context
import android.util.Log
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.peerchat.sqldelight.Database
import nl.tudelft.trustchain.peerchat.Contact

class PeerChatStore(context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "peerchat.db")
    private val database = Database(driver)

    fun addContact(publicKey: PublicKey, name: String) {
        Log.d("AddContact", "save contact $name $publicKey")
        database.dbContactQueries.addContact(name, publicKey.keyToBin())
    }

    fun getContacts(): List<Contact> {
        return database.dbContactQueries.getAll { name, public_key ->
            val publicKey = defaultCryptoProvider.keyFromPublicBin(public_key)
            Contact(name, publicKey)
        }.executeAsList()
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
