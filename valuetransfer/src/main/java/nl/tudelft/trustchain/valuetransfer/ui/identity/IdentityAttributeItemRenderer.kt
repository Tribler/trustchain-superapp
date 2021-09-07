package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.os.Handler
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_identity_attribute.view.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.entity.IdentityAttribute

class IdentityAttributeItemRenderer(
    private val onDeleteClick: (IdentityAttribute) -> Unit,
    private val onEditClick: (IdentityAttribute) -> Unit,
    private val onShareClick: (IdentityAttribute) -> Unit,
) : ItemLayoutRenderer<IdentityAttributeItem, View>(
    IdentityAttributeItem::class.java
) {
    private var isExpanded = HashMap<String, Boolean>()

    override fun bindView(item: IdentityAttributeItem, view: View) = with(view) {
        tvAttributeName.text = item.attribute.name
        tvAttributeValue.text = item.attribute.value

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
    }

    private fun expandOptions(view: View, item: IdentityAttributeItem) {
        val pushInAnimation = AnimationUtils.loadAnimation(view.context, R.anim.push_left_in)
        view.ivAttributeOptionsButton.isVisible = false
        view.clAttributeOptions.isVisible = true
        view.clAttributeOptions.startAnimation(pushInAnimation)
        isExpanded[item.attribute.name] = true

        Handler().postDelayed(
            {
                if (isExpanded[item.attribute.name] == true) {
                    contractOptions(view, item)
                }
            },
            5000
        )
    }

    private fun contractOptions(view: View, item: IdentityAttributeItem) {
        val pushOutAnimation = AnimationUtils.loadAnimation(view.context, R.anim.push_right_out)
        view.clAttributeOptions.startAnimation(pushOutAnimation)
        view.clAttributeOptions.isVisible = false
        view.ivAttributeOptionsButton.isVisible = true
        isExpanded[item.attribute.name] = false
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_identity_attribute
    }
}
