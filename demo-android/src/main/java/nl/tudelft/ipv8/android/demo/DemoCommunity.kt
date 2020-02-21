package nl.tudelft.ipv8.android.demo

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Community
import java.util.*

class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    val discoveredAddressesContacted: MutableMap<Address, Date> = mutableMapOf()

    override fun walkTo(address: Address) {
        super.walkTo(address)

        discoveredAddressesContacted[address] = Date()
    }
}
