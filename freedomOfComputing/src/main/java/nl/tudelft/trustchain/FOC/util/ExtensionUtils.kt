package nl.tudelft.trustchain.FOC.util

class ExtensionUtils {
    companion object {
        const val apkExtenstion = "apk"
        const val jarExtenstion = "jar"
        const val torrentExtension = "torrent"

        val supportedAppExtensions = arrayListOf(apkExtenstion, jarExtenstion)
    }
}
