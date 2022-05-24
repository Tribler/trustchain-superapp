package nl.tudelft.trustchain.literaturedao.controllers


import android.app.Activity
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.*


class PdfController : Activity() {
    fun stripText(file: InputStream): String {
        var parsedText = ""
        var document: PDDocument? = null

        try {
            document = PDDocument.load(file)
        } catch (e: IOException) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while loading document to strip", e)
        }

        try {
            val pdfStripper = PDFTextStripper()
            parsedText = pdfStripper.getText(document)
        } catch (e: IOException) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while stripping text", e)
        } finally {
            try {
                document?.close()
            } catch (e: IOException) {
                Log.e("PdfBox-Android-Sample", "Exception thrown while closing document", e)
            }
        }

        return parsedText
    }

}
