package nl.tudelft.trustchain.foc.util

import com.frostwire.jlibtorrent.Sha1Hash

class MagnetUtils {
    companion object {
        const val MAGNET_HEADER_STRING = "magnet:"
        const val PRE_HASH_STRING = "$MAGNET_HEADER_STRING?xt=urn:btih:"
        const val DISPLAY_NAME_APPENDER = "&dn="
        const val ADDRESS_TRACKER_APPENDER = "&tr="
        const val ADDRESS_TRACKER = "&tr"

        fun constructMagnetLink(
            infoHash: Sha1Hash,
            displayName: String
        ): String {
            return PRE_HASH_STRING + infoHash + DISPLAY_NAME_APPENDER + displayName
        }
    }
}
