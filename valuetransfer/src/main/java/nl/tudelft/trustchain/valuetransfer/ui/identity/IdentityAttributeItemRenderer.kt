package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.ItemContactIdentityAttributeBinding
import nl.tudelft.trustchain.valuetransfer.databinding.ItemIdentityAttributeBinding
import java.text.SimpleDateFormat
import java.util.Locale

class IdentityAttributeItemRenderer(
    private val layoutType: Int,
    private val onOptionsClick: (IdentityAttribute) -> Unit,
) : ItemLayoutRenderer<IdentityAttributeItem, View>(
    IdentityAttributeItem::class.java
) {
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)

    override fun bindView(item: IdentityAttributeItem, view: View) = with(view) {

        if (layoutType == 1) {
            val identityBinding = ItemIdentityAttributeBinding.bind(view)
            identityBinding.tvAttributeName.text = item.attribute.name
            identityBinding.tvAttributeValue.text = item.attribute.value
            identityBinding.tvAttributeDate.text = dateFormat.format(item.attribute.added)

            identityBinding.ivAttributeOptionsButton.setOnClickListener {
                onOptionsClick(item.attribute)
            }
        } else {
            val contactBinding = ItemContactIdentityAttributeBinding.bind(view)
            contactBinding.tvIdentityAttributeName.text = item.attribute.name
            contactBinding.tvIdentityAttributeValue.text = item.attribute.value
            contactBinding.tvIdentityAttributeDate.text = dateFormat.format(item.attribute.added)

            contactBinding.ivIdentityAttributeCopy.setOnClickListener {
                onOptionsClick(item.attribute)
            }
        }
    }

    override fun getLayoutResourceId(): Int {
        return if (layoutType == 1) {
            R.layout.item_identity_attribute
        } else {
            R.layout.item_contact_identity_attribute
        }
    }
}
