package nl.tudelft.ipv8.messaging

import nl.tudelft.ipv8.Address

class Packet(
    val source: Address,
    val data: ByteArray
)
