package nl.tudelft.ipv8.keyvault

interface CryptoProvider {
    fun generateKey(): PrivateKey
    fun keyFromPublicBin(bin: ByteArray): PublicKey
    fun keyFromPrivateBin(bin: ByteArray): PrivateKey
    fun isValidPublicBin(bin: ByteArray): Boolean {
        return try {
            keyFromPublicBin(bin)
            true
        } catch (e: Exception) {
            false
        }
    }
}

var defaultCryptoProvider: CryptoProvider = JavaCryptoProvider
