package com.example.offlinemoney.ui.settings

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_gateway.view.*
import nl.tudelft.trustchain.common.eurotoken.Gateway
import nl.tudelft.trustchain.eurotoken.R

class GatewayItemRenderer(
    private val onItemLongClick: (Gateway) -> Unit
) : ItemLayoutRenderer<GatewayItem, View>(
    GatewayItem::class.java
) {

    override fun bindView(item: GatewayItem, view: View) = with(view) {
        txtName.text = item.gateway.name
        txtPeerId.text = item.gateway.mid
        txtAddress.text = item.gateway.connInfo
        if (item.gateway.preferred) {
            txtPref.visibility = View.VISIBLE
        } else {
            txtPref.visibility = View.GONE
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
