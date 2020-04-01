package nl.tudelft.trustchain.currencyii

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import java.util.*

class DemoCommunity : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)

        discoveredAddressesContacted[address] = Date()
    }

    override fun load() {
        super.load()
    }

    override fun unload() {
        super.unload()
    }
}
