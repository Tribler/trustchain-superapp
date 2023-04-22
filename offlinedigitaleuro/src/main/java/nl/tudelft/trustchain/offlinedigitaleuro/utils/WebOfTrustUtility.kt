package nl.tudelft.trustchain.offlinedigitaleuro.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.offlinedigitaleuro.db.OfflineMoneyRoomDatabase
import nl.tudelft.trustchain.offlinedigitaleuro.db.WebOfTrust
import kotlin.math.max
import kotlin.math.min

class WebOfTrustUtility {

companion object {
    fun getTrustOfPeer(peer: PublicKey, db: OfflineMoneyRoomDatabase) : Int? {
        val trustScore: Int?

        runBlocking(Dispatchers.IO) {
            trustScore = db.webOfTrustDao().getUserTrustScore(peer.keyToBin().toHex())
        }

        return trustScore
    }

    fun addNewPeer(peer: PublicKey, trust: Int = 0, db: OfflineMoneyRoomDatabase) : Pair<Boolean, String> {
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

    fun updateUserTrust(peer: PublicKey, trustModifier: Int, db: OfflineMoneyRoomDatabase) : Pair<Boolean, String> {
        val maybePrevTrust: Int? = getTrustOfPeer(peer, db)
        if (maybePrevTrust == null) {
            val errMsg = "Error: user not in the database"
            return Pair(false, errMsg)
        }

        val prevTrust: Int = maybePrevTrust
        val newTrust: Int = max(-100, min(100, prevTrust + trustModifier))

        runBlocking(Dispatchers.IO) {
            db.webOfTrustDao().updateUserScore(peer.keyToBin().toHex(), newTrust)
        }

        return Pair(true, "")
    }

} // companion object
}
