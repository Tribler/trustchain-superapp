package nl.tudelft.trustchain.valuetransfer.ui.trust

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.keyvault.PublicKey

data class TrustItem (val key: PublicKey, val score: Float): Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is TrustItem && key == other.key
    }
}
