package nl.tudelft.trustchain.common.util

import java.time.Instant

object TimingUtils {

    fun getTimestamp(): Long {
        return Instant.now().toEpochMilli()
    }
}
