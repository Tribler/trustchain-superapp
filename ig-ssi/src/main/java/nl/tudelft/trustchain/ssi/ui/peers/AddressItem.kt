package nl.tudelft.trustchain.ssi.peers

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.messaging.Address
import java.util.*

class AddressItem(
    val address: Address,
    val discovered: Date?,
    val contacted: Date?
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is AddressItem && other.address == address
    }
}
