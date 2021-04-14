package nl.tudelft.trustchain.eurotoken.ui.settings

import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.GatewayStore

object DefaultGateway {
    val ip = "188.166.94.39" // "gateway.euro-token.nl"
    val port = 8090
    val publicKey =
        "4c69624e61434c504b3aa57e128a49ca6f2779668b39e3d57e56ea624b57255432cfa4c693092351bb4c110d379ca5822a4e43d5863909a1bccaea18c1735027a78ae876dc9f11ca01e4"
        // "4c69624e61434c504b3a035fd325276e03b9d0d106a91353cdd00f7a21aa861be79226224809cfedf80cbcc0e210c2ddc2f91a1fbc3e1e3cd0622e32027a27a8be7f5d28a73b42c0369f"
    val name = "Demo gateway"


    fun addGateway(store: GatewayStore) {
        val key = defaultCryptoProvider.keyFromPublicBin(publicKey.hexToBytes())
        store.addGateway(key, name, ip, port.toLong(), true)
    }
}
