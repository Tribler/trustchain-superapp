package nl.tudelft.trustchain.datavault.ui

import com.mattskala.itemadapter.Item
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
