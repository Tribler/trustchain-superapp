package nl.tudelft.trustchain.literaturedao.utils

import com.frostwire.jlibtorrent.Sha1Hash

class MagnetUtils {
    companion object {
        const val magnetHeaderString = "magnet:"
        const val preHashString = "$magnetHeaderString?xt=urn:btih:"
        const val displayNameAppender = "&dn="
        const val addressTrackerAppender = "&tr="
        const val addressTracker = "&tr"

        fun constructMagnetLink(infoHash: Sha1Hash, displayName: String): String {
            return preHashString + infoHash + displayNameAppender + displayName
        }
    }
}
