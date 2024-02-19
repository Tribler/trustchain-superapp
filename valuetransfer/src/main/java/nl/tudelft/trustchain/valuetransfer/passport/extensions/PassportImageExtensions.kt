package nl.tudelft.trustchain.valuetransfer.passport.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.jmrtd.lds.AbstractImageInfo
import org.jnbis.WsqDecoder
import java.io.*

const val MIME_IMAGE_JP2 = "image/jp2"
const val MIME_IMAGE_JPEG2000 = "image/jpeg2000"
const val MIME_IMAGE_WSQ = "image/x-wsq"

/**
 * Get passport image inputstream
 */
fun getPassportImage(imageInfo: AbstractImageInfo): Bitmap? {
    val imageLength = imageInfo.imageLength
    val dataInputStream = DataInputStream(imageInfo.imageInputStream)
    val buffer = ByteArray(imageLength)

    return try {
        dataInputStream.readFully(buffer, 0, imageLength)
        val inputStream = ByteArrayInputStream(buffer, 0, imageLength)

        decodePassportImage(imageLength, imageInfo.mimeType, inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Decode passport image
 */
@Throws(IOException::class)
fun decodePassportImage(
    imageLength: Int,
    mimeType: String,
    inputStream: InputStream
): Bitmap? {
    var input = inputStream

    synchronized(input) {
        val dataIn = DataInputStream(input)
        val bytes = ByteArray(imageLength)
        dataIn.readFully(bytes)
        input = ByteArrayInputStream(bytes)
    }

    return if (MIME_IMAGE_JP2.equals(mimeType, ignoreCase = true) ||
        MIME_IMAGE_JPEG2000.equals(
            mimeType,
            ignoreCase = true
        )
    ) {
        val bitmap = org.jmrtd.jj2000.JJ2000Decoder.decode(input)
        val intData = bitmap.pixels

        Bitmap.createBitmap(
            intData,
            0,
            bitmap.width,
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
    } else if (MIME_IMAGE_WSQ.equals(mimeType, ignoreCase = true)) {
        val wsqDecoder = WsqDecoder()
        val bitmap = wsqDecoder.decode(input.readBytes())
        val byteData = bitmap.pixels
        val intData = IntArray(byteData.size)
        for (j in byteData.indices) {
            intData[j] = -0x1000000 or
                ((byteData[j].toInt() and 0xFF) shl 16) or
                ((byteData[j].toInt() and 0xFF) shl 8) or
                (byteData[j].toInt() and 0xFF)
        }
        Bitmap.createBitmap(
            intData,
            0,
            bitmap.width,
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
    } else {
        return BitmapFactory.decodeStream(input)
    }
}
