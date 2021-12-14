package nl.tudelft.trustchain.valuetransfer.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.*

//fun String.getExtension(): String? {
//    return MimeTypeMap.getFileExtensionFromUrl(this)
//}
//
//fun String.getMimeTypeFromExtension(): String? {
//    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(this.getExtension())
//}

fun saveFile(context: Context, file: File, fileName: String) {
    val folderName = "TrustChain"
    val documents = "Documents"

//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//
//        val mimeType = fileName.getMimeTypeFromExtension()
//
//        val path = Environment.DIRECTORY_DOCUMENTS + File.separator + folderName + File.separator + documents
//
//        val contentResolver = context.contentResolver
//        val contentValues = ContentValues().apply {
//            put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
//            put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
//            put(MediaStore.Files.FileColumns.RELATIVE_PATH, path)
//        }
//
//        val uri = contentResolver.insert(
//            MediaStore.Files.getContentUri("external"),
//            contentValues
//        ) ?: throw IOException("Failed to create new MediaStore record")
//
//        val outputStream: OutputStream
//        try {
//            outputStream = contentResolver.openOutputStream(uri)!!
//            outputStream.write(file.readBytes())
//
//            Toast.makeText(context, "Document successfully exported!", Toast.LENGTH_SHORT).show()
//            Log.d("VTLOG", "Document successfully exported!")
//
//            outputStream.close()
//        } catch (e: FileNotFoundException) {
//            Log.d("VTLOG", "Document export failed: ${e.localizedMessage}")
//            Toast.makeText(context, "Document export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
//            e.printStackTrace()
//        }
//    } else {
        @Suppress("DEPRECATION")
        val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
        createDirectory(downloadsPath)
//        val documentPath = Environment.getExternalStoragePublicDirectory(folderName).toString()
//        createDirectory(documentPath)

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
//    }
}

fun createDirectory(path: String) {
    val file = File(path)
    if (!file.exists()) file.mkdir()
}

fun saveImage(context: Context, bitmap: Bitmap, fileName: String) {
    val folderName = "TrustChain"
    val fileNameWithExtension = "$fileName.jpg"
//    val mimeType = "image/jpg"

//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//        val path = Environment.DIRECTORY_PICTURES + File.separator + folderName
////        val path = Environment.DIRECTORY_DOWNLOADS + File.separator + folderName + File.separator + pictures
//
////        val documentPath = Environment.DIRECTORY_DOCUMENTS
////        createDirectory(documentPath)
////
////        val folderPath = documentPath + File.separator + folderName
////        createDirectory(folderPath)
//
//        val contentResolver = context.contentResolver
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, fileNameWithExtension)
//            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
//            put(MediaStore.MediaColumns.RELATIVE_PATH, path)
//        }
//
//        val imageURI = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
//            ?: throw IOException("Failed to create new MediaStore record")
//
//        val outputStream: OutputStream
//
//        try {
//            outputStream = contentResolver.openOutputStream(imageURI)!!
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//
//            Toast.makeText(context, "Image successfully exported!", Toast.LENGTH_SHORT).show()
//            Log.d("VTLOG", "Image successfully exported!")
//
//            outputStream.close()
//        } catch (e: FileNotFoundException) {
//            Log.d("VTLOG", "Image export failed: ${e.localizedMessage}")
//            Toast.makeText(context, "Image export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
//            e.printStackTrace()
//        }
//    } else {
        @Suppress("DEPRECATION")
////        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + folderName
////        val directoryPath = File(path)
////
////        if (!directoryPath.exists()) directoryPath.mkdir()

        val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
        createDirectory(downloadsPath)

        val trustChainPath = downloadsPath + File.separator + folderName
        createDirectory(trustChainPath)

        val documentsPath = trustChainPath + File.separator + "Images"
        createDirectory(documentsPath)

//        val folderPath = downloadsPath + File.separator + folderName
//        createDirectory(folderPath)

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
//    }
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
