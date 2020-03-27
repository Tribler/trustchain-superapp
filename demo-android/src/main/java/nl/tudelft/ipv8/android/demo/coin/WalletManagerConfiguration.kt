package nl.tudelft.ipv8.android.demo.coin

data class SerializedDeterministicKey(
    val seed: String,
    val creationTime: Long
)

data class AddressPrivateKeyPair(
    val address: String,
    val privateKey: String
)

enum class BitcoinNetworkOptions {
    PRODUCTION, TEST_NET
}

class WalletManagerConfiguration(
    val network: BitcoinNetworkOptions = BitcoinNetworkOptions.TEST_NET,
    val key: SerializedDeterministicKey? = null,
    val addressPrivateKeyPair: AddressPrivateKeyPair? = null
)
