package nl.tudelft.trustchain.common.valuetransfer.extensions

import android.graphics.*
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
fun imageBytes(bitmap: Bitmap, quality: Int = 100): ByteArray? {
    val baos = ByteArrayOutputStream()

    return try {
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        baos.toByteArray()
    } catch (e: Exception) {
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
fun encodeImage(bitmap: Bitmap, quality: Int = 100): String? {
    val baos = ByteArrayOutputStream()

    return try {
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
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
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Converts a bitmap to a squared bitmap
 */
fun Bitmap.toSquare(): Bitmap {
    val side = minOf(width, height)

    val xOffset = (width - side) / 2
    val yOffset = (height - side) / 2

    return Bitmap.createBitmap(this, xOffset, yOffset, side, side)
}

/**
 * Returns a new Bitmap with provided background color and recycles the current one.
 */
fun Bitmap.changeBackgroundColor(color: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, config)
    val canvas = Canvas(bitmap)
    canvas.drawColor(color)
    canvas.drawBitmap(this, 0F, 0F, null)
    recycle()
    return bitmap
}

/**
 * Returns a bitmap with padding
 */
fun Bitmap.setPadding(size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width + 2 * size, height + 2 * size, config)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(this, size.toFloat(), size.toFloat(), null)

    return bitmap
}
