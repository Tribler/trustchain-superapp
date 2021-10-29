package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contact_identity_attribute.view.*
import kotlinx.android.synthetic.main.item_identity_attribute.view.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import java.text.SimpleDateFormat
import java.util.*

class IdentityAttributeItemRenderer(
    private val layoutType: Int,
    private val onOptionsClick: (IdentityAttribute) -> Unit,
) : ItemLayoutRenderer<IdentityAttributeItem, View>(
    IdentityAttributeItem::class.java
) {
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)

    override fun bindView(item: IdentityAttributeItem, view: View) = with(view) {
        if (layoutType == 1) {
            tvAttributeName.text = item.attribute.name
            tvAttributeValue.text = item.attribute.value
            tvAttributeDate.text = dateFormat.format(item.attribute.added)

            ivAttributeOptionsButton.setOnClickListener {
                onOptionsClick(item.attribute)
            }
        } else {
            tvIdentityAttributeName.text = item.attribute.name
            tvIdentityAttributeValue.text = item.attribute.value
            tvIdentityAttributeDate.text = dateFormat.format(item.attribute.added)

            ivIdentityAttributeCopy.setOnClickListener {
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
