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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    fun createPersonalIdentity(givenNames: String, surname: String, placeOfBirth: String, dateOfBirth: Long, nationality: String, gender: String, personalNumber: Long, documentNumber: String) : Identity{
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
            type = "Personal",
            content = content,
            added = Date(),
            modified = Date()
        )
    }

    fun createBusinessIdentity(companyName: String, established: Long, residence: String, areaOfExpertise: String) : Identity{
        val id = UUID.randomUUID().toString()

        var content = BusinessIdentity(
            companyName,
            Date(established),
            residence,
            areaOfExpertise
        )

        return Identity(
            id = id,
            publicKey = myPeer.publicKey,
            type = "Business",
            content = content,
            added = Date(),
            modified = Date()
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
