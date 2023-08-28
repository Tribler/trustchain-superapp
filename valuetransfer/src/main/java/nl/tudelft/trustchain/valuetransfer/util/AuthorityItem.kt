package nl.tudelft.trustchain.valuetransfer.util

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.keyvault.PublicKey

class AuthorityItem(
    val publicKey: PublicKey,
    val publicKeyHash: String,
    val name: String
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is AuthorityItem && other.publicKey == publicKey && other.publicKeyHash == publicKeyHash && other.name == name
    }
}
