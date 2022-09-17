package nl.tudelft.trustchain.datavault.accesscontrol

import nl.tudelft.trustchain.common.util.TimingUtils

class SessionTokenCache {
    private val cache = mutableMapOf<String, SessionToken>()

    init {
        // TODO
        /*CoroutineScope(Dispatchers.Unconfined).launch {
            while (isActive) {
                // Delete exprired sessions
            }
        }*/
    }

    fun set(key: String, sessionToken: SessionToken) {
        sessionToken.extend()
        cache[key] = sessionToken
    }

    fun get(key: String): SessionToken? {
        return cache[key]
    }

    fun remove(key: String): SessionToken? {
        return cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }
}
