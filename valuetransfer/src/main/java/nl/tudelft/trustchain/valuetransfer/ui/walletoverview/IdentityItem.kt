package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import com.mattskala.itemadapter.Item
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.valuetransfer.entity.Identity

data class IdentityItem(
    val identity: Identity
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is IdentityItem && identity.id == other.identity.id
    }
}
