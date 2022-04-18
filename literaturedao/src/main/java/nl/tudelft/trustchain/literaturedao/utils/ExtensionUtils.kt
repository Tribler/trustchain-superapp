package nl.tudelft.trustchain.literaturedao.utils

class ExtensionUtils {
    companion object {
        const val pdfExtenstion = "pdf"
        const val epubExtenstion = "epub"
        const val torrentExtension = "torrent"

        val supportedAppExtensions = arrayListOf(pdfExtenstion, epubExtenstion)
    }
}
