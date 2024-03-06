package nl.tudelft.trustchain.foc.util

class ExtensionUtils {
    companion object {
        const val APK_EXTENSION = "apk"
        const val JAR_EXTENSION = "jar"
        const val TORRENT_EXTENSION = "torrent"
        const val DEX_EXTENSION = "dex"

        const val APK_DOT_EXTENSION = ".$APK_EXTENSION"
        const val TORRENT_DOT_EXTENSION = ".$TORRENT_EXTENSION"
        const val DATA_DOT_EXTENSION = ".dat"

        val supportedAppExtensions = arrayListOf(APK_EXTENSION, JAR_EXTENSION)
    }
}
