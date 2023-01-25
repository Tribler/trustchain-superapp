package nl.tudelft.trustchain.valuetransfer.db

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.PersonalIdentity
import nl.tudelft.valuetransfer.sqldelight.Database
import java.util.*

class IdentityStore(context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "identities-vt.db")
    private val database = Database(driver)

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
            modified: Long,
            verified: Long,
            dateOfExpiry: Long?
        ->
        Identity(
            id,
            defaultCryptoProvider.keyFromPublicBin(publicKey),
            PersonalIdentity(
                name!!,
                surname!!,
                if (gender == 0L) GENDER_MALE else if (gender == 1L) GENDER_FEMALE else GENDER_NEUTRAL,
                Date(dateOfBirth!!),
                placeOfBirth!!,
                nationality!!,
                personalNumber!!,
                documentNumber!!,
                verified == 1L,
                if (dateOfExpiry == null) Date() else Date(dateOfExpiry)
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
        ->
        IdentityAttribute(
            id,
            name,
            value,
            Date(added),
            Date(modified)
        )
    }

    fun createIdentitiesTable() {
        database.dbIdentityQueries.createIdentitiesTable()
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

    fun isVerified(): Boolean {
        return database.dbIdentityQueries.isVerified().executeAsOneOrNull() == 1L
    }

    fun getIdentity(): Identity? {
        return database.dbIdentityQueries.getIdentity(identityMapper).executeAsOneOrNull()
    }

    fun addIdentity(identity: Identity) {
        database.dbIdentityQueries.addIdentity(
            identity.id,
            identity.publicKey.keyToBin(),
            identity.content.givenNames,
            identity.content.surname,
            if (identity.content.gender == GENDER_MALE) 0L else if (identity.content.gender == GENDER_FEMALE) 1L else 2L,
            identity.content.dateOfBirth.time,
            identity.content.placeOfBirth,
            identity.content.nationality,
            identity.content.personalNumber,
            identity.content.documentNumber,
            identity.added.time,
            identity.modified.time,
            if (identity.content.verified) 1L else 0L,
            identity.content.dateOfExpiry.time
        )
    }

    fun editIdentity(identity: Identity) {
        database.dbIdentityQueries.updateIdentity(
            identity.publicKey.keyToBin(),
            identity.content.givenNames,
            identity.content.surname,
            if (identity.content.gender == GENDER_MALE) 0L else if (identity.content.gender == GENDER_FEMALE) 1L else 2L,
            identity.content.dateOfBirth.time,
            identity.content.placeOfBirth,
            identity.content.nationality,
            identity.content.personalNumber,
            identity.content.documentNumber,
            Date().time,
            if (identity.content.verified) 1L else 0L,
            identity.content.dateOfExpiry.time,
            identity.id
        )
    }

    fun deleteIdentity() {
        database.dbIdentityQueries.deleteIdentity()
    }

    fun createAttributesTable() {
        return database.dbAttributeQueries.createAttributesTable()
    }

    fun getAllAttributes(): Flow<List<IdentityAttribute>> {
        return database.dbAttributeQueries.getAllAttributes(attributeMapper)
            .asFlow().mapToList()
    }

    fun getAttributeNames(): List<String> {
        return database.dbAttributeQueries.getAttributeNames().executeAsList()
    }

    fun addAttribute(identityAttribute: IdentityAttribute) {
        database.dbAttributeQueries.addAttribute(
            identityAttribute.id,
            identityAttribute.name,
            identityAttribute.value,
            identityAttribute.added.time,
            identityAttribute.modified.time,
        )
    }

    fun editAttribute(identityAttribute: IdentityAttribute) {
        database.dbAttributeQueries.updateAttribute(
            identityAttribute.value,
            Date().time,
            identityAttribute.id
        )
    }

    fun deleteAttribute(identityAttribute: IdentityAttribute) {
        database.dbAttributeQueries.deleteAttribute(identityAttribute.id)
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

        const val GENDER_MALE = "M"
        const val GENDER_FEMALE = "F"
        const val GENDER_NEUTRAL = "X"
    }
}
