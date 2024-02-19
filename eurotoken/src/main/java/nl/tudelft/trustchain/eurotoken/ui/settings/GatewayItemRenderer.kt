package nl.tudelft.trustchain.eurotoken.ui.settings

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.trustchain.common.eurotoken.Gateway
import nl.tudelft.trustchain.eurotoken.R
import nl.tudelft.trustchain.eurotoken.databinding.ItemGatewayBinding

class GatewayItemRenderer(
    private val onItemLongClick: (Gateway) -> Unit
) : ItemLayoutRenderer<GatewayItem, View>(
        GatewayItem::class.java
    ) {
    override fun bindView(
        item: GatewayItem,
        view: View
    ) = with(view) {
        val binding = ItemGatewayBinding.bind(view)

        binding.txtName.text = item.gateway.name
        binding.txtPeerId.text = item.gateway.mid
        binding.txtAddress.text = item.gateway.connInfo
        if (item.gateway.preferred) {
            binding.txtPref.visibility = View.VISIBLE
        } else {
            binding.txtPref.visibility = View.GONE
        }
        setOnLongClickListener {
            onItemLongClick(item.gateway)
            true
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_gateway
    }
}
