package nl.tudelft.ipv8.keyvault

object ECCrypto {
    fun generateKey(): PrivateKey {
        return LibNaClSK.generate()
    }
}
