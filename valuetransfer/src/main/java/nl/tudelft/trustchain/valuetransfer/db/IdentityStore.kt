package nl.tudelft.trustchain.valuetransfer.db

import android.app.Person
import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.valuetransfer.entity.Attribute
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.PersonalIdentity
import nl.tudelft.valuetransfer.sqldelight.Database
import java.util.*

class IdentityStore(context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "identities-vt.db")
    private val database = Database(driver)

    val peerChatStore = PeerChatStore.getInstance(context)

    private val identityMapper = {
            id: String,
            publicKey: ByteArray,
            name: String?,
            surname: String?,
            gender: Long?,
            dateOfBirth: Long?,
            placeOfBirth: String?,
            nationality: String?,
            personalNumber: Long?,
            documentNumber: String?,
            added: Long,
            modified: Long
            ->
                Identity(
                    id,
                    defaultCryptoProvider.keyFromPublicBin(publicKey),
                    PersonalIdentity(
                        name!!,
                        surname!!,
                        if (gender == 0L) "Male" else if (gender == 1L) "Female" else "Neutral",
                        Date(dateOfBirth!!),
                        placeOfBirth!!,
                        nationality!!,
                        personalNumber!!,
                        documentNumber!!
                    ),
                    Date(added),
                    Date(modified)
                )
    }

    private val attributeMapper = {
        id: String,
        name: String,
        value: String,
        added: Long,
        modified: Long
        -> Attribute(
            id,
            name,
            value,
            Date(added),
            Date(modified)
        )
    }

    fun createIdentitiesTable() {
        return database.dbIdentityQueries.createIdentitiesTable()
    }

    fun getAllIdentities(): Flow<List<Identity>> {
        return database.dbIdentityQueries.getAll(identityMapper)
            .asFlow().mapToList()
    }

    fun hasIdentity(): Boolean {
        if (database.dbIdentityQueries.getIdentity(identityMapper).executeAsOneOrNull() == null) {
            return false
        }
        return true
    }

    fun getIdentity(): Identity? {
        return database.dbIdentityQueries.getIdentity(identityMapper).executeAsOneOrNull()
    }

    fun getIdentityByPublicKey(publicKey: PublicKey): Flow<List<Identity>> {
        val publicKeyBin = publicKey.keyToBin()
        return database.dbIdentityQueries.getIdentityByPublicKey(publicKeyBin, identityMapper)
            .asFlow().mapToList()
    }

    fun getIdentityByID(ID: String): Flow<List<Identity>> {
        return database.dbIdentityQueries.getIdentityByID(ID, identityMapper)
            .asFlow().mapToList()
    }

    fun addIdentity(identity: Identity) {
        database.dbIdentityQueries.addIdentity(
            identity.id,
            identity.publicKey.keyToBin(),
            identity.content.givenNames,
            identity.content.surname,
            if (identity.content.gender == "Male") 0L else if (identity.content.gender == "Female") 1L else 2L,
            identity.content.dateOfBirth.time,
            identity.content.placeOfBirth,
            identity.content.nationality,
            identity.content.personalNumber,
            identity.content.documentNumber,
            identity.added.time,
            identity.modified.time
        )
    }

    fun editIdentity(identity: Identity) {
        database.dbIdentityQueries.updateIdentity(
            identity.publicKey.keyToBin(),
            identity.content.givenNames,
            identity.content.surname,
            if(identity.content.gender == "Male") 0L else if (identity.content.gender == "Female") 1L else 2L,
            identity.content.dateOfBirth.time,
            identity.content.placeOfBirth,
            identity.content.nationality,
            identity.content.personalNumber,
            identity.content.documentNumber,
            Date().time,
            identity.id
        )
    }

    fun deleteIdentity(identity: Identity) {
        database.dbIdentityQueries.deleteIdentity(identity.id)
    }

    fun deleteIdentityByPublicKey(identity: Identity) {
        database.dbIdentityQueries.deleteIdentityByPublicKey(identity.publicKey.keyToBin())
    }

    fun createAttributesTable() {
        return database.dbAttributeQueries.createAttributesTable()
    }

    fun getAllAttributes(): Flow<List<Attribute>> {
        return database.dbAttributeQueries.getAllAttributes(attributeMapper)
            .asFlow().mapToList()
    }

    fun getAttributeNames(): List<String> {
        return database.dbAttributeQueries.getAttributeNames().executeAsList()
    }

    fun addAttribute(attribute: Attribute) {
        database.dbAttributeQueries.addAttribute(
            attribute.id,
            attribute.name,
            attribute.value,
            attribute.added.time,
            attribute.modified.time,
        )
    }

    fun editAttribute(attribute: Attribute) {
        database.dbAttributeQueries.updateAttribute(
            attribute.value,
            Date().time,
            attribute.id
        )
    }

    fun deleteAttribute(attribute: Attribute) {
        database.dbAttributeQueries.deleteAttribute(attribute.id)
    }

    fun deleteAllAttributes() {
        database.dbAttributeQueries.deleteAllAttributes()
    }

    companion object {
        private lateinit var instance: IdentityStore
        fun getInstance(context: Context): IdentityStore {
            if (!::instance.isInitialized) {
                instance = IdentityStore(context)
            }
            return instance
        }
    }
}
