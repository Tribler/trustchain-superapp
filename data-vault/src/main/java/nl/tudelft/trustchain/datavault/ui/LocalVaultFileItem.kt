package nl.tudelft.trustchain.datavault.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import nl.tudelft.trustchain.datavault.R
import nl.tudelft.trustchain.datavault.accesscontrol.AccessControlList
import java.io.File

class LocalVaultFileItem(
    val context: Context,
    override val file: File,
    val accessControlList: AccessControlList?
): VaultFileItem() {
    override val name: String get() {
        return file.name
    }

    override fun isDirectory(): Boolean {
        return file.isDirectory
    }

    override fun toString(): String {
        return "VaultFile(${file.path})"
    }

    fun getImage(inBitmap: Bitmap?): Bitmap {
        val options = BitmapFactory.Options().also {
            it.outWidth = IMAGE_WIDTH
        }
        inBitmap?.also {
            //options.inBitmap = inBitmap
        }
        return if (isDirectory()) {
            BitmapFactory.decodeResource(context.resources, R.mipmap.folder_icon, options)
        } else {
            BitmapFactory.decodeFile(file.absolutePath, options)
        }
    }
}
