package nl.tudelft.trustchain.datavault.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import nl.tudelft.trustchain.datavault.R
import nl.tudelft.trustchain.datavault.accesscontrol.AccessControlList
import java.io.File
import java.lang.IllegalArgumentException

class LocalVaultFileItem(
    val context: Context,
    override val file: File,
    val accessControlList: AccessControlList?
): VaultFileItem() {
    override val name: String get() {
        return file.name
    }

    fun getImage(inBitmap: Bitmap?): Bitmap? {
        if (isDirectory()) return null

        return try{
            val options = BitmapFactory.Options().also {
                it.outWidth = ImageViewHolder.IMAGE_WIDTH
                it.inBitmap = inBitmap
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: IllegalArgumentException) {
            // Try without inBitmap
            val options = BitmapFactory.Options().also {
                it.outWidth = ImageViewHolder.IMAGE_WIDTH
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        }
    }
}
