package nl.tudelft.trustchain.datavault.ui

import android.graphics.Bitmap
import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.datavault.accesscontrol.AccessControlList
import java.io.File

abstract class VaultFileItem(): Item() {
    abstract val file: File
    abstract val name: String

    open fun isDirectory(): Boolean {
        return file.isDirectory
    }

    override fun toString(): String {
        return "VaultFile(${file.path})"
    }

    override fun equals(other: Any?): Boolean {
        if (other is VaultFileItem) {
            return file.path == other.file.path
        }

        return false
    }
}
