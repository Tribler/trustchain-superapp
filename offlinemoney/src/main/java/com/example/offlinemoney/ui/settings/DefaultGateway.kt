package com.example.offlinemoney.ui.settings

import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.BuildConfig
import nl.tudelft.trustchain.common.eurotoken.GatewayStore

object DefaultGateway {
    fun addGateway(store: GatewayStore) {
        val key =
            defaultCryptoProvider.keyFromPublicBin(BuildConfig.DEFAULT_GATEWAY_PK.hexToBytes())
        store.addGateway(
            key,
            BuildConfig.DEFAULT_GATEWAY_NAME,
            BuildConfig.DEFAULT_GATEWAY_IP,
            BuildConfig.DEFAULT_GATEWAY_PORT.toLong(),
            true
        )
    }
}
