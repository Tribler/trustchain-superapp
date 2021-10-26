package nl.tudelft.trustchain.valuetransfer.ui.identity

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute

data class IdentityAttributeItem(
    val attribute: IdentityAttribute
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is IdentityAttributeItem && attribute.id == other.attribute.id
    }
}
