package nl.tudelft.trustchain.common.eurotoken

import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex

data class Gateway(
    val name: String,
    val publicKey: PublicKey,
    val ip: String,
    val port: Long,
    val preferred: Boolean
) {
    val mid = publicKey.keyToHash().toHex()

    val peer by lazy {
        Peer(publicKey, IPv4Address(ip, port.toInt()))
    }

    val connInfo by lazy {
        "$ip:$port"
    }

    override fun equals(other: Any?): Boolean {
        return other is Gateway &&
            publicKey.keyToBin().contentEquals(other.publicKey.keyToBin()) &&
            name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + publicKey.hashCode()
        return result
    }
}
