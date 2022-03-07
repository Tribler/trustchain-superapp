package nl.tudelft.trustchain.datavault.tools

import java.io.File
import android.graphics.BitmapFactory

fun File.isImage(): Boolean {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(absolutePath, options)
    return options.outWidth != -1 && options.outHeight != -1
}
