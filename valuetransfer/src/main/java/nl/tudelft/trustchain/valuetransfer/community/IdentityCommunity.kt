package nl.tudelft.trustchain.valuetransfer.community

import android.content.Context
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.PersonalIdentity
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

    fun createIdentity(
        givenNames: String,
        surname: String,
        placeOfBirth: String,
        dateOfBirth: Long,
        nationality: String,
        gender: String,
        personalNumber: Long,
        documentNumber: String
    ): Identity {
        val id = UUID.randomUUID().toString()

        var content = PersonalIdentity(
            givenNames,
            surname,
            gender,
            Date(dateOfBirth),
            placeOfBirth,
            nationality,
            personalNumber,
            documentNumber
        )

        return Identity(
            id = id,
            publicKey = myPeer.publicKey,
            content = content,
            added = Date(),
            modified = Date()
        )
    }

    fun createAttribute(name: String, value: String): IdentityAttribute {
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

    fun deleteDatabase(context: Context) {
        context.deleteDatabase("identities-vt.db")
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
