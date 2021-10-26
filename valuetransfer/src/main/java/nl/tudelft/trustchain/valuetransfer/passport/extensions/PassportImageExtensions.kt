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
fun decodePassportImage(imageLength: Int, mimeType: String, inputStream: InputStream): Bitmap? {
    var input = inputStream

    synchronized(input) {
        val dataIn = DataInputStream(input)
        val bytes = ByteArray(imageLength)
        dataIn.readFully(bytes)
        input = ByteArrayInputStream(bytes)
    }

    return if (MIME_IMAGE_JP2.equals(mimeType, ignoreCase = true) || MIME_IMAGE_JPEG2000.equals(mimeType, ignoreCase = true)) {
        val bitmap = org.jmrtd.jj2000.JJ2000Decoder.decode(input)
        val intData = bitmap.pixels

        Bitmap.createBitmap(intData, 0, bitmap.width, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    } else if (MIME_IMAGE_WSQ.equals(mimeType, ignoreCase = true)) {
        val wsqDecoder = WsqDecoder()
        val bitmap = wsqDecoder.decode(input.readBytes())
        val byteData = bitmap.pixels
        val intData = IntArray(byteData.size)
        for (j in byteData.indices) {
            intData[j] = -0x1000000 or ((byteData[j].toInt() and 0xFF) shl 16) or ((byteData[j].toInt() and 0xFF) shl 8) or (byteData[j].toInt() and 0xFF)
        }
        Bitmap.createBitmap(intData, 0, bitmap.width, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    } else {
        return BitmapFactory.decodeStream(input)
    }

//    return if (mimeType.equals(IMAGE_JP2, ignoreCase = true) || mimeType.equals(IMAGE_JPEG2000, ignoreCase = true)) {

//        // Save jp2 file
//        val output = FileOutputStream(File(context.cacheDir, TEMP_JP2))
//        val buffer = ByteArray(1024)
//
//        var read: Int
//        while((inputStream.read(buffer).also { read = it }) != -1) {
//            output.write(buffer, 0, read)
//        }
//        output.close()
//
//        // Decode jp2 file
//        val pinfo = Decoder.getAllParameters()
//        var defaults = ParameterList()
//
//        for (i in pinfo.indices.reversed()) {
//            if (pinfo[i][3] != null) {
//                defaults[pinfo[i][0]] = pinfo[i][3]
//            }
//        }
//
//        val parameters = ParameterList(defaults).apply {
//            setProperty(PROPERTY_RATE, PROPERTY_VALUE_THREE)
//            setProperty(PROPERTY_O, context.cacheDir.toString() + TEMP_PATH_PPM)
//            setProperty(PROPERTY_DEBUG, PROPERTY_VALUE_ON)
//            setProperty(PROPERTY_I, context.cacheDir.toString() + TEMP_PATH_JP2)
//        }
//
//        Decoder(parameters).run()
//
//        // Read ppm file
//        val reader = BufferedInputStream(
//            FileInputStream(File(context.cacheDir.toString() + TEMP_PATH_PPM))
//        )
//
//        if (reader.read().toChar() != 'P' || reader.read().toChar() != '6') return null
//
//        reader.read()
//
//        var widths = ""
//        var heights = ""
//        var temp: Char = reader.read().toChar()
//
//        while (temp != ' ') {
//            widths += temp
//            temp = reader.read().toChar()
//        }
//
//        temp = reader.read().toChar()
//
//        while (temp in '0'..'9') {
//            heights += temp
//            temp = reader.read().toChar()
//        }
//
//        if (reader.read().toChar() != '2' || reader.read().toChar() != '5' || reader.read().toChar() != '5') return null
//
//        reader.read()
//
//        val width = Integer.valueOf(widths)
//        val height = Integer.valueOf(heights)
//
//        val colors = IntArray(width * height)
//
//        val pixel = ByteArray(3)
//        var len = reader.read(pixel)
//        var cnt = 0
//        var total = 0
//        val rgb = IntArray(3)
//
//        while (len > 0) {
//            for (i in 0 until len) {
//                rgb[cnt] = if (pixel[i] >= 0) pixel[i].toInt() else pixel[i] + 255
//                if (++cnt == 3) {
//                    cnt = 0
//                    colors[total++] = Color.rgb(rgb[0], rgb[1], rgb[2])
//                }
//            }
//            len = reader.read(pixel)
//        }
//
//        Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)

//        val bitmap = org.jmrtd.jj2000.JJ2000Decoder.decode(inputStream)
//        val intData = bitmap.pixels
//        Bitmap.createBitmap(intData, 0, bitmap.width, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
//
//    } else if (mimeType.equals(IMAGE_WSQ, ignoreCase = true)) {
//        val wsqDecoder = WsqDecoder()
//        val bitmap: org.jnbis.Bitmap = wsqDecoder.decode(inputStream)
//        val byteData = bitmap.pixels
//        var intData = IntArray(byteData.size)
//
//        for (j in byteData.indices) {
//            intData[j] =
//                -0x1000000 or (byteData[j] and 0xFF.toByte() shl 16) or (byteData[j] and 0xFF.toByte() shl 8) or (byteData[j] and 0xFF.toByte()).toInt()
//        }
//
//        Bitmap.createBitmap(intData, 0, bitmap.width, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
//
//    } else {
//        BitmapFactory.decodeStream(inputStream)
//    }
}

