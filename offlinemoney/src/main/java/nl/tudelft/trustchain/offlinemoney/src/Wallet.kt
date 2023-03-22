package nl.tudelft.trustchain.offlinemoney.src

import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey

class Wallet {
    val privateKey: PrivateKey = JavaCryptoProvider.generateKey()
    val publicKey: PublicKey = privateKey.pub()
}
