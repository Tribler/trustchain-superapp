package nl.tudelft.trustchain.FOC.util

class ExtensionUtils {
    companion object {
        const val apkExtenstion = "apk"
        const val jarExtenstion = "jar"
        const val torrentExtension = "torrent"
        const val dexExtension = "dex"

        const val apkDotExtension = ".$apkExtenstion"
        const val torrentDotExtension = ".$torrentExtension"
        const val dataDotExtension = ".dat"

        val supportedAppExtensions = arrayListOf(apkExtenstion, jarExtenstion)
    }
}
