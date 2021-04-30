package nl.tudelft.trustchain.ssi.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import nl.tudelft.ipv8.util.defaultEncodingUtils
import java.io.ByteArrayOutputStream

@Suppress("DEPRECATION")
fun parseHtml(html: String?): Spanned? {
    return when {
        html == null -> {
            // return an empty spannable if the html is null
            SpannableString("")
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
            // FROM_HTML_MODE_LEGACY is the behaviour that was used for versions below android N
            // we are using this flag to give a consistent behaviour
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        }
        else -> {
            Html.fromHtml(html)
        }
    }
}

fun encodeB64(buffer: ByteArray): String {
    return defaultEncodingUtils.encodeBase64ToString(buffer)
}

fun decodeB64(string: String): ByteArray {
    return defaultEncodingUtils.decodeBase64FromString(string)
}

fun encodeImage(image: Bitmap): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    image.compress(Bitmap.CompressFormat.JPEG, 1, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return encodeB64(byteArray)
}

fun decodeImage(string: String): Bitmap {
    val decodedBytes = decodeB64(
        string
    )
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
}
