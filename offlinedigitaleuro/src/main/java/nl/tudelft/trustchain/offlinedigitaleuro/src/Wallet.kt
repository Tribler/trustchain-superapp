package nl.tudelft.trustchain.offlinedigitaleuro.src

import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey

class Wallet {
    val privateKey: PrivateKey = defaultCryptoProvider.generateKey()
    val publicKey: PublicKey = privateKey.pub()

    object CentralAuthority {
        internal val privateKey: PrivateKey = JavaCryptoProvider.keyFromPrivateBin(
            byteArrayOf(
                76, 105, 98, 78, 97, 67, 76, 83, 75, 58, -29, -114, 126, -47, -39, -5, 22, 89,
                94, 71, -1, 118, -30, 120, -8, -75, 2, 102, 99, -21, 57, -95, 124, 126, -30, 33,
                -99, 37, -125, -105, 20, -45, 94, 2, -109, 125, 98, -52, 84, -54, -47, 13, 15, 75,
                73, 11, -128, 5, -4, -101, 102, -1, -95, 33, -107, -77, -41, 89, 102, 44, 71, 107, 1, 107
            )
        )
        internal val publicKey = privateKey.pub()
    }
}
