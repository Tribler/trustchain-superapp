package nl.tudelft.trustchain.FOC.util

class ExtensionUtils {
    companion object {
        const val apkExtension = "apk"
        const val jarExtension = "jar"
        const val torrentExtension = "torrent"
        const val dexExtension = "dex"

        const val apkDotExtension = ".$apkExtension"
        const val torrentDotExtension = ".$torrentExtension"
        const val dataDotExtension = ".dat"

        val supportedAppExtensions = arrayListOf(apkExtension, jarExtension)
    }
}
