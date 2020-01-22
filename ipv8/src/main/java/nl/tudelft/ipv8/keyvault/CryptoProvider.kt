package nl.tudelft.ipv8.keyvault

interface CryptoProvider {
    fun generateKey(): PrivateKey
    fun keyFromPublicBin(bin: ByteArray): PublicKey
}
