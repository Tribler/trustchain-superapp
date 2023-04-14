@file:Suppress("DEPRECATION") // TODO: Use new method of handing intent results.

package nl.tudelft.trustchain.common.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.integration.android.IntentIntegrator
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.common.ui.QRCodeActivityPortrait

/**
 * Helper class for creating
 */
class QRCodeUtils(private val context: Context) {

    /**
     * Start the QR scanner, which if successful, calls onActivityResult() on the fragment
     */
    fun startQRScanner(fragment: Fragment, promptText: String? = null, vertical: Boolean = false) {
        run {
            val integrator = FragmentIntentIntegrator(fragment)
                .setPrompt(promptText ?: "Scan QR Code")
                .setOrientationLocked(false)
                .setBeepEnabled(true)
                .setCameraId(0)
            if (vertical) {
                integrator.captureActivity = QRCodeActivityPortrait::class.java
            }
            integrator.initiateScan()
        }
    }

    fun parseActivityResult(requestCode: Int, resultCode: Int, data: Intent?): String? {
        return IntentIntegrator.parseActivityResult(requestCode, resultCode, data)?.contents
    }

    /**
     * from: https://demonuts.com/kotlin-generate-qr-code/
     * Creates a QR code from text
     */
    fun createQR(
        text: String,
        size: Int = QRCodeSize,
        pColor: Int = pixelColor,
        bColor: Int = backgroundColor
    ): Bitmap? {
        if (text.isEmpty()) {
            Toast.makeText(context, "Enter String!", Toast.LENGTH_SHORT).show()
        } else {
            try {
                return textToImageEncode(text, size, pColor, bColor)
            } catch (e: WriterException) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * Encode the text into a bitmap
     */
    @Throws(WriterException::class)
    private fun textToImageEncode(
        value: String,
        size: Int = QRCodeSize,
        pColor: Int = pixelColor,
        bColor: Int = backgroundColor
    ): Bitmap? {
        val bitMatrix: BitMatrix
        try {
            bitMatrix = MultiFormatWriter().encode(
                value,
                BarcodeFormat.QR_CODE,
                size,
                size, null
            )
        } catch (IllegalArgumentException: IllegalArgumentException) {
            return null
        }

        val bitMatrixWidth = bitMatrix.width
        val bitMatrixHeight = bitMatrix.height
        val pixels = IntArray(bitMatrixWidth * bitMatrixHeight)

        for (y in 0 until bitMatrixHeight) {
            val offset = y * bitMatrixWidth
            for (x in 0 until bitMatrixWidth) {
                pixels[offset + x] = if (bitMatrix.get(x, y))
                    ContextCompat.getColor(context, pColor)
                else
                    ContextCompat.getColor(context, bColor)
            }
        }
        val bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_8888)

        bitmap.setPixels(pixels, 0, bitMatrixWidth, 0, 0, bitMatrixWidth, bitMatrixHeight)
        return bitmap
    }

    companion object {
        const val QRCodeSize = 500
        var pixelColor = R.color.black
        var backgroundColor = R.color.white
    }
}
