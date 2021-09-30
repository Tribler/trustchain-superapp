package nl.tudelft.trustchain.datavault.ui

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.datavault.accesscontrol.AccessPolicy
import java.io.File

class VaultFileItem(
    val file: File,
    val accessPolicy: AccessPolicy?
) : Item() {
}
