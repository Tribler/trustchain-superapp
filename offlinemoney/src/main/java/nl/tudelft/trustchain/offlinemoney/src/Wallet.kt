package nl.tudelft.trustchain.offlinemoney.src

import mu.KotlinLogging
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import java.security.SecureRandom

class Wallet {
    val privateKey: PrivateKey = defaultCryptoProvider.generateKey()
    val publicKey: PublicKey = privateKey.pub()

    companion object {
        // change this to a fixed value
        val authority_wallet = Wallet();
    }
}
