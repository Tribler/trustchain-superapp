package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.os.Handler
import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contact_identity_attribute.view.*
import kotlinx.android.synthetic.main.item_identity_attribute.view.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.common.valuetransfer.extensions.viewEnterFromRight
import nl.tudelft.trustchain.common.valuetransfer.extensions.viewExitToRight
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class IdentityAttributeItemRenderer(
    private val layoutType: Int,
    private val onDeleteClick: (IdentityAttribute) -> Unit,
    private val onEditClick: (IdentityAttribute) -> Unit,
    private val onShareClick: (IdentityAttribute) -> Unit,
) : ItemLayoutRenderer<IdentityAttributeItem, View>(
    IdentityAttributeItem::class.java
) {
    private var isExpanded = HashMap<String, Boolean>()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)

    override fun bindView(item: IdentityAttributeItem, view: View) = with(view) {
        if (layoutType == 1) {
            tvAttributeName.text = item.attribute.name
            tvAttributeValue.text = item.attribute.value
            tvAttributeDate.text = dateFormat.format(item.attribute.added)

            ivAttributeDeleteButton.setOnClickListener {
                contractOptions(view, item)
                onDeleteClick(item.attribute)
            }

            ivAttributeEditButton.setOnClickListener {
                contractOptions(view, item)
                onEditClick(item.attribute)
            }

            ivAttributeShareButton.setOnClickListener {
                contractOptions(view, item)
                onShareClick(item.attribute)
            }

            ivAttributeOptionsButton.setOnClickListener {
                expandOptions(view, item)
            }
        } else {
            tvIdentityAttributeName.text = item.attribute.name
            tvIdentityAttributeValue.text = item.attribute.value
            tvIdentityAttributeDate.text = dateFormat.format(item.attribute.added)

            ivIdentityAttributeCopy.setOnClickListener {
                onShareClick(item.attribute)
            }
        }
    }

    private fun expandOptions(view: View, item: IdentityAttributeItem) {
        view.ivAttributeOptionsButton.isVisible = false
        view.clAttributeOptions.viewEnterFromRight(view.context, 1000)
        isExpanded[item.attribute.name] = true

        Handler().postDelayed(
            Runnable {
                if (isExpanded[item.attribute.name] == true) {
                    contractOptions(view, item)
                }
            },
            5000
        )
    }

    private fun contractOptions(view: View, item: IdentityAttributeItem) {
        view.clAttributeOptions.viewExitToRight(view.context, 1000)
        view.ivAttributeOptionsButton.isVisible = true
        isExpanded[item.attribute.name] = false
    }

    override fun getLayoutResourceId(): Int {
        return if (layoutType == 1) {
            R.layout.item_identity_attribute
        } else {
            R.layout.item_contact_identity_attribute
        }
    }
}
