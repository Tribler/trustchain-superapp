package nl.tudelft.trustchain.valuetransfer.util

import android.content.Context
import android.net.Uri
import nl.tudelft.ipv8.util.sha256
import nl.tudelft.ipv8.util.toHex
import java.io.File
import java.io.FileOutputStream

fun saveFile(context: Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)
    val bytes = inputStream!!.readBytes()
    return saveFile(context, bytes)
}

fun saveFile(context: Context, bytes: ByteArray): File {
    val id = sha256(bytes).toHex()
    val file = MessageAttachment.getFile(context, id)
    val outputStream = FileOutputStream(file)
    outputStream.write(bytes)
    outputStream.close()
    return file
}
