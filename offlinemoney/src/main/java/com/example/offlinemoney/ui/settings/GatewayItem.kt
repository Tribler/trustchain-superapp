package com.example.offlinemoney.ui.settings

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.common.eurotoken.Gateway

data class GatewayItem(
    val gateway: Gateway
) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is GatewayItem && gateway.mid == other.gateway.mid
    }
}
