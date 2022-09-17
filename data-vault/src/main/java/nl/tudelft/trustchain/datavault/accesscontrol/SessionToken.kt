package nl.tudelft.trustchain.datavault.accesscontrol

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ebsi.JWTHelper
import nl.tudelft.trustchain.common.util.TimingUtils
import java.security.SecureRandom

class SessionToken(val peer: Peer) {

    val value: String = SecureRandom.getSeed(16).toHex()
    var exp: Long = TimingUtils.getTimestamp() + (1000 * EXPIRATION_TIME)

    fun isExpired(): Boolean {
        return exp < TimingUtils.getTimestamp()
    }

    fun extend() {
        exp = TimingUtils.getTimestamp() + (1000 * EXPIRATION_TIME)
    }

    companion object {
        const val EXPIRATION_TIME = 300 // seconds

        fun getAFT(sessionToken: String): String {
            return JWTHelper.getJWTPayload(sessionToken)["AFT"]!!.toString()
        }
    }
}
