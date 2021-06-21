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
import nl.tudelft.trustchain.valuetransfer.entity.BusinessIdentity
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.IdentityContent
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
            type: Long,
            name: String?,
            surname: String?,
            gender: Long?,
            dateOfBirth: Long?,
            placeOfBirth: String?,
            nationality: String?,
            personalNumber: Long?,
            documentNumber: String?,
            residence: String?,
            areaOfExpertise: String?,
            added: Long,
            modified: Long
            ->
                Identity(
                    id,
                    defaultCryptoProvider.keyFromPublicBin(publicKey),
                    if (type == 0L) "Personal" else "Business",
                    when (type) {
                        0L -> PersonalIdentity(
                            name!!,
                            surname!!,
                            if (gender == 0L) "Male" else if (gender == 1L) "Female" else "Neutral",
                            Date(dateOfBirth!!),
                            placeOfBirth!!,
                            nationality!!,
                            personalNumber!!,
                            documentNumber!!
                        )
                        1L -> BusinessIdentity(
                            name!!,
                            Date(dateOfBirth!!),
                            residence!!,
                            areaOfExpertise!!
                        )
                        else -> throw IllegalStateException("Type must be either personal or business")
                    },
                    Date(added),
                    Date(modified)
                )
    }

    fun getAllIdentities(): Flow<List<Identity>> {
        return database.dbIdentityQueries.getAll(identityMapper)
            .asFlow().mapToList()
    }

    fun hasPersonalIdentity(): Boolean {
        if (database.dbIdentityQueries.getPersonalIdentity(identityMapper).executeAsOneOrNull() == null) {
            return false
        }
        return true
    }

    fun hasBusinessIdentity(): Boolean {
        return database.dbIdentityQueries.getAllBusinessIdentities(identityMapper).executeAsList()
            .isNotEmpty()
    }

    fun getPersonalIdentity(): Identity {
        return database.dbIdentityQueries.getPersonalIdentity(identityMapper).executeAsOne()
    }

    fun getAllPersonalIdentities(): Flow<List<Identity>> {
        return database.dbIdentityQueries.getAllPersonalIdentities(identityMapper)
            .asFlow().mapToList()
    }

    fun getAllBusinessIdentities(): Flow<List<Identity>> {
        return database.dbIdentityQueries.getAllBusinessIdentities(identityMapper)
            .asFlow().mapToList()
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
            if (identity.type == "Personal") 0L else 1L,
            if (identity.type == "Personal") (identity.content as PersonalIdentity).givenNames else (identity.content as BusinessIdentity).companyName,
            if (identity.type == "Personal") (identity.content as PersonalIdentity).surname else null,
            if (identity.type == "Personal") {
                if ((identity.content as PersonalIdentity).gender == "Male") 0L else if (identity.content.gender == "Female") 1L else 2L
            } else {
                null
            },
            if (identity.type == "Personal") (identity.content as PersonalIdentity).dateOfBirth.time else (identity.content as BusinessIdentity).dateOfBirth.time,
            if (identity.type == "Personal") (identity.content as PersonalIdentity).placeOfBirth else null,
            if (identity.type == "Personal") (identity.content as PersonalIdentity).nationality else null,
            if (identity.type == "Personal") (identity.content as PersonalIdentity).personalNumber else null,
            if (identity.type == "Personal") (identity.content as PersonalIdentity).documentNumber else null,
            if (identity.type == "Business") (identity.content as BusinessIdentity).residence else null,
            if (identity.type == "Business") (identity.content as BusinessIdentity).areaOfExpertise else null,
            identity.added.time,
            identity.modified.time
        )
    }

    fun editPersonalIdentity(identity: Identity) {
        database.dbIdentityQueries.updatePersonalIdentity(
            identity.publicKey.keyToBin(),
            (identity.content as PersonalIdentity).givenNames,
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

    fun editBusinessIdentity(identity: Identity) {
        database.dbIdentityQueries.updateBusinessIdentity(
            identity.publicKey.keyToBin(),
            (identity.content as BusinessIdentity).companyName,
            identity.content.dateOfBirth.time,
            identity.content.residence,
            identity.content.areaOfExpertise,
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
