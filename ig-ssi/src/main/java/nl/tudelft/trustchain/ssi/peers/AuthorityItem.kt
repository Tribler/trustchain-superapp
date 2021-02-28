package nl.tudelft.trustchain.ssi.peers

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.messaging.Address
import java.util.*

class AuthorityItem(
    val publicKey: PublicKey,
    val publicKeyHash: String,
    val name: String
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is AuthorityItem && other.publicKey == publicKey && other.publicKeyHash == publicKeyHash && other.name == name
    }
}
