package nl.tudelft.trustchain.valuetransfer.ui.identity

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.valuetransfer.entity.Attribute
import nl.tudelft.trustchain.valuetransfer.entity.Identity

data class AttributeItem(
    val attribute: Attribute
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is AttributeItem && attribute.id == other.attribute.id
    }
}
