package nl.tudelft.trustchain.datavault.ui

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.datavault.accesscontrol.AccessControlList
import java.io.File

class VaultFileItem(
    val file: File,
    val accessControlList: AccessControlList?
) : Item() {

    override fun toString(): String {
        return "VaultFile(${file.path})"
    }
}
