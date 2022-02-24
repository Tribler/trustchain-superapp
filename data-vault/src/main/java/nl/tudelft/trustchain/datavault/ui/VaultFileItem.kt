package nl.tudelft.trustchain.datavault.ui

import android.graphics.Bitmap
import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.datavault.accesscontrol.AccessControlList
import java.io.File

abstract class VaultFileItem(): Item() {
    abstract val file: File
    abstract val name: String
    abstract fun isDirectory(): Boolean

    companion object {
        const val IMAGE_WIDTH = 300
    }
}
