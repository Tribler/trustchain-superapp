package nl.tudelft.trustchain.offlinedigitaleuro.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.offlinedigitaleuro.db.OfflineDigitalEuroRoomDatabase
import nl.tudelft.trustchain.offlinedigitaleuro.db.WebOfTrust
import kotlin.math.max
import kotlin.math.min

class WebOfTrustUtility {

companion object {
    const val TRUST_NEUTRAL: Int = 0
    const val TRUST_INCREASE: Int = +10
    const val TRUST_MAX: Int = +100
    const val TRUST_MIN: Int = -100

    fun getTrustOfPeer(peer: PublicKey, db: OfflineDigitalEuroRoomDatabase) : Int? {
        val trustScore: Int?

        runBlocking(Dispatchers.IO) {
            trustScore = db.webOfTrustDao().getUserTrustScore(peer.keyToBin().toHex())
        }

        return trustScore
    }

    //    Adds the peer in the web of trust DB if does not exist and depending on the absolute field,
    // setting the score to the passed value (absolute true) or modifying by it (absolute false)
    // returns on NO error Pair<Boolean, ""> -> true if added a new peer or false if updated a peer
    // returns on error Pair<null, errorMessage>
    fun addOrUpdatePeer(peer: PublicKey, trust: Int = 0, db: OfflineDigitalEuroRoomDatabase, absolute: Boolean = false): Pair<Boolean?, String> {
        val (_, _) = addNewPeer(peer, 0, db)

        val (resultUpdate, _) = updateUserTrust(peer, trust, db, absolute)
        if (resultUpdate) {
            return Pair(false, "")
        }

        // should have not reached here, there is a BUG
        return Pair(null, "BUG: peer is and is not at the same time in the DB")
    }

    // returns Pair<true, ""> on success
    // returns Pair<false, errorMessage> on failure to add
    fun addNewPeer(peer: PublicKey, trust: Int = 0, db: OfflineDigitalEuroRoomDatabase) : Pair<Boolean, String> {
        if (getTrustOfPeer(peer, db) != null) {
            val errMsg = "Error, user already in the database"
            return Pair(false, errMsg)
        }

        val wotUser = WebOfTrust(peer.keyToBin().toHex(), trust)

        runBlocking(Dispatchers.IO) {
            db.webOfTrustDao().insertUserTrustScore(wotUser)
        }

        return Pair(true, "")
    }

    // returns Pair<true, ""> on success
    // returns Pair<false, errorMessage> on failure to update
    fun updateUserTrust(peer: PublicKey, trust: Int, db: OfflineDigitalEuroRoomDatabase, absolute: Boolean = false) : Pair<Boolean, String> {
        val maybePrevTrust: Int? = getTrustOfPeer(peer, db)
        if (maybePrevTrust == null) {
            val errMsg = "Error: user not in the database"
            return Pair(false, errMsg)
        }

        val prevTrust: Int = maybePrevTrust
        var newTrust: Int = if (absolute) { trust } else { prevTrust + trust }
        newTrust = max(TRUST_MIN, min(TRUST_MAX, newTrust))

        runBlocking(Dispatchers.IO) {
            db.webOfTrustDao().setUserScore(peer.keyToBin().toHex(), newTrust)
        }

        return Pair(true, "")
    }

} // companion object
}
