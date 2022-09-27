package nl.tudelft.trustchain.valuetransfer.community

import android.content.Context
import nl.tudelft.ipv8.Community // OK4
import nl.tudelft.ipv8.Overlay // OK4
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityInfo
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.PersonalIdentity
import nl.tudelft.trustchain.valuetransfer.util.getInitials
import java.util.*

class IdentityCommunity(
    private val store: IdentityStore,
    private val context: Context
) : Community() {
    override val serviceId = "c86a7db45eb3563ae047639817baec4db2bc7c44"

    fun createIdentitiesTable() {
        store.createIdentitiesTable()
    }

    fun createAttributesTable() {
        store.createAttributesTable()
    }

    fun getIdentity(): Identity? {
        return store.getIdentity()
    }

    fun hasIdentity(): Boolean {
        return store.hasIdentity()
    }

    fun isVerified(): Boolean {
        return store.isVerified()
    }

    fun addIdentity(identity: Identity) {
        store.addIdentity(identity)
    }

    fun createIdentity(
        givenNames: String,
        surname: String,
        placeOfBirth: String,
        dateOfBirth: Long,
        nationality: String,
        gender: String,
        personalNumber: Long,
        documentNumber: String,
        verified: Boolean,
        dateOfExpiry: Long,
    ): Identity {
        val id = UUID.randomUUID().toString()

        return Identity(
            id = id,
            publicKey = myPeer.publicKey,
            content = PersonalIdentity(
                givenNames,
                surname,
                gender,
                Date(dateOfBirth),
                placeOfBirth,
                nationality,
                personalNumber,
                documentNumber,
                verified,
                Date(dateOfExpiry)
            ),
            added = Date(),
            modified = Date()
        )
    }

    fun deleteIdentity() {
        deleteAllAttributes()
        store.deleteIdentity()
    }

    fun createIdentityAttribute(name: String, value: String): IdentityAttribute {
        val id = UUID.randomUUID().toString()

        return IdentityAttribute(
            id = id,
            name = name,
            value = value,
            added = Date(),
            modified = Date()
        )
    }

    fun getUnusedAttributeNames(): List<String> {
        val attributes = IdentityAttribute.IDENTITY_ATTRIBUTES
        val currentAttributeNames = store.getAttributeNames()

        return attributes.filter { name ->
            !currentAttributeNames.contains(name)
        }
    }

    fun deleteAllAttributes() {
        store.deleteAllAttributes()
    }

    fun deleteDatabase(context: Context) {
        context.deleteDatabase("identities-vt.db")
    }

    fun getIdentityInfo(imageHash: String?): IdentityInfo? {
        if (!hasIdentity()) {
            return null
        }

        return IdentityInfo(
            getIdentity()?.content?.givenNames?.getInitials(),
            getIdentity()?.content?.surname,
            isVerified(),
            imageHash
        )
    }

    class Factory(
        private val store: IdentityStore,
        private val context: Context
    ) : Overlay.Factory<IdentityCommunity>(IdentityCommunity::class.java) {
        override fun create(): IdentityCommunity {
            return IdentityCommunity(store, context)
        }
    }
}
