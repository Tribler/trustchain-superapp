package nl.tudelft.trustchain.common.valuetransfer.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Encode bytearray to string
 */
fun encodeBytes(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.DEFAULT)

/**
 * Decode string to bytearray
 */
fun decodeBytes(encoded: String): ByteArray = Base64.decode(encoded, Base64.DEFAULT)

/**
 * Convert image bitmap to bytearray
 */
fun imageBytes(bitmap: Bitmap): ByteArray? {
    val baos = ByteArrayOutputStream()

    return try {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        baos.toByteArray()
    } catch(e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Convert bytearray to image bitmap
 */
fun bytesToImage(bytes: ByteArray): Bitmap? {
    return try {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Encode image bitmap to string
 */
fun encodeImage(bitmap: Bitmap): String? {
    val baos = ByteArrayOutputStream()

    return try {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val bytes = baos.toByteArray()

        encodeBytes(bytes)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Decode string to image bitmap
 */
fun decodeImage(decoded: String): Bitmap? {
    return try {
        val bytes = decodeBytes(decoded)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Resize image to max dimension in pixels for width or height
 */
fun Bitmap.resize(maxDimension: Float): Bitmap? {
    return try {
        val scale = if (width > maxDimension || height > maxDimension) maxDimension / maxOf(width, height) else 1.0f
        val matrix = Matrix().apply {
            postScale(scale, scale)
        }

        val resized = Bitmap.createBitmap(this, 0, 0, width, height, matrix, false)
        Log.d("VTLOG", "BITMAP WIDTH: $width ${resized.width}")
        Log.d("VTLOG", "BITMAP HEIGHT: $height ${resized.height}")
        Log.d("VTLOG", "BITMAP SIZE IS: ${this.byteCount} ${resized?.byteCount}")
        resized
    } catch(e: Exception) {
        e.printStackTrace()
        null
    }
}
