package nl.tudelft.trustchain.valuetransfer.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.*

fun saveFile(context: Context, file: File, fileName: String) {
    val folderName = "TrustChain"
    val documents = "Documents"

    @Suppress("DEPRECATION")
    val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
    createDirectory(downloadsPath)

    val trustChainPath = downloadsPath + File.separator + folderName
    createDirectory(trustChainPath)

    val documentsPath = trustChainPath + File.separator + documents
    createDirectory(documentsPath)

    val outputFile = File(documentsPath, fileName)

    try {
        file.copyTo(outputFile, true)

        Toast.makeText(context, "Document successfully exported!", Toast.LENGTH_SHORT).show()
        Log.d("VTLOG", "Document successfully exported!")
    } catch (e: Exception) {
        e.printStackTrace()

        Toast.makeText(context, "Document export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        Log.d("VTLOG", "Document export failed: ${e.localizedMessage}")
    }
}

fun createDirectory(path: String) {
    val file = File(path)
    if (!file.exists()) file.mkdir()
}

fun saveImage(context: Context, bitmap: Bitmap, fileName: String) {
    val folderName = "TrustChain"
    val fileNameWithExtension = "$fileName.jpg"

    @Suppress("DEPRECATION")
    val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
    createDirectory(downloadsPath)

    val trustChainPath = downloadsPath + File.separator + folderName
    createDirectory(trustChainPath)

    val documentsPath = trustChainPath + File.separator + "Images"
    createDirectory(documentsPath)

    val file = File(documentsPath, fileNameWithExtension)

    val fileOutputStream: FileOutputStream
    try {
        fileOutputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)

        Toast.makeText(context, "Image successfully exported!", Toast.LENGTH_SHORT).show()
        Log.d("VTLOG", "Image successfully exported!")

        fileOutputStream.close()
    } catch (e: FileNotFoundException) {
        Log.d("VTLOG", "Image export failed: ${e.localizedMessage}")
        Toast.makeText(context, "Image export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

fun storageIsWritable(): Boolean {
    return isExternalStorageWritable() && !isExternalStorageReadOnly() && isExternalStorageAvailable()
}

fun isExternalStorageWritable(): Boolean {
    return Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
}

fun isExternalStorageReadOnly(): Boolean {
    return Environment.MEDIA_MOUNTED_READ_ONLY == Environment.getExternalStorageState()
}

fun isExternalStorageAvailable(): Boolean {
    return Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
}
