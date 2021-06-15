package nl.tudelft.trustchain.valuetransfer.community

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.BusinessIdentity
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.entity.IdentityContent
import nl.tudelft.trustchain.valuetransfer.entity.PersonalIdentity
import java.util.*

class IdentityCommunity(
    private val store: IdentityStore,
    private val context: Context
) : Community() {
    override val serviceId = "c86a7db45eb3563ae047639817baec4db2bc7c44"

//    override fun load() {
//        super.load()
//
//        scope.launch {
//            while (isActive) {
//                try {
//                    Log.d("VALUETRANSFER", "COMMUNITY");
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//                delay(30000L)
//            }
//        }
//    }

    fun getPersonalIdentity() : Identity {
        return store.getPersonalIdentity()
    }

    fun createIdentity(type: String) : Identity {
        val id = UUID.randomUUID().toString()

        var content = when(type) {
            "Personal" -> PersonalIdentity(
                    givenNames = "John Michael",
                    surname = "Doe",
                    gender = "Male",
                    dateOfBirth = Date(),
                    placeOfBirth = "Sillicon Valley",
                    nationality = "American",
                    personalNumber = 149720602,
                    documentNumber = "2NH98IU7FG",
                )
            "Business" -> BusinessIdentity(
                    companyName = "Alpha Chips Industries",
                    dateOfBirth = Date(),
                    residence = "Los Angeles",
                    areaOfExpertise = "Electronics",
                )
            else -> throw IllegalArgumentException("Only personal or business identities allowed")
        }

        return Identity(
            id = id,
            publicKey = myPeer.publicKey,
            type = type,
            content = content,
            added = Date(),
            modified = Date(),
        )

    }

    fun deleteDatabase(context: Context) {
        context.deleteDatabase("identities-vt.db")
    }

    fun testCommunity() {
        Log.d("IDENTITYCOMMUNITY", "test executed")
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
