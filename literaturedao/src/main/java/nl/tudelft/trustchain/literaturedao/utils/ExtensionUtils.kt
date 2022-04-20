package nl.tudelft.trustchain.literaturedao.utils

class ExtensionUtils {
    companion object {
        const val pdfExtension = "pdf"
        const val epubExtension = "epub"
        const val torrentExtension = "torrent"

        const val pdfDotExtension = ".$pdfExtension"
        const val torrentDotExtension = ".$torrentExtension"

        val supportedAppExtensions = arrayListOf(pdfExtension, epubExtension)
    }
}
