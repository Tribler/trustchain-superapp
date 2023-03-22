package nl.tudelft.trustchain.offlinemoney.src

import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey

class Wallet {
    val privateKey: PrivateKey = defaultCryptoProvider.generateKey()
    val publicKey: PublicKey = privateKey.pub()
}
