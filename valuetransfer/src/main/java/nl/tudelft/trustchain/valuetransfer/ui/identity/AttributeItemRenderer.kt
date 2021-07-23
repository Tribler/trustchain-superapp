package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_identity_attribute.view.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.entity.Attribute

class AttributeItemRenderer(
    private val onEditClick: (Attribute) -> Unit,
    private val onShareClick: (Attribute) -> Unit
) : ItemLayoutRenderer<AttributeItem, View>(
    AttributeItem::class.java
) {
    override fun bindView(item: AttributeItem, view: View) = with(view) {
        tvAttributeName.text = item.attribute.name
        tvAttributeValue.text = item.attribute.value

        ivAttributeEditButton.setOnClickListener {
            onEditClick(item.attribute)
        }

        ivAttributeShareButton.setOnClickListener {
            onShareClick(item.attribute)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_identity_attribute
    }
}
