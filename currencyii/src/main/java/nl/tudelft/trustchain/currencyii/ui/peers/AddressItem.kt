package nl.tudelft.trustchain.currencyii.ui.peers

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.IPv4Address
import java.util.*

class AddressItem(
    val address: IPv4Address,
    val discovered: Date?,
    val contacted: Date?
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is AddressItem && other.address == address
    }
}
