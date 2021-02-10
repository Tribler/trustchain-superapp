package nl.tudelft.trustchain.eurotoken.ui.settings

import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import java.net.InetAddress

object DefaultGateway {
    val ip = "gateway.euro-token.nl"
    val port = 8090
    val publicKey = "4c69624e61434c504b3a035fd325276e03b9d0d106a91353cdd00f7a21aa861be79226224809cfedf80cbcc0e210c2ddc2f91a1fbc3e1e3cd0622e32027a27a8be7f5d28a73b42c0369f"
    val name = "Demo gateway"


    fun addGateway(store: GatewayStore){
        val key = defaultCryptoProvider.keyFromPublicBin(publicKey.hexToBytes())
        store.addGateway(key, name, ip, port.toLong(), true)
    }
}
