package nl.tudelft.trustchain.currencyii.coin

data class SerializedDeterministicKey(
    val seed: String,
    val creationTime: Long
)

data class AddressPrivateKeyPair(
    val address: String,
    val privateKey: String
)

enum class BitcoinNetworkOptions {
    PRODUCTION, REG_TEST, TEST_NET
}

class WalletManagerConfiguration(
    val network: BitcoinNetworkOptions,
    val key: SerializedDeterministicKey? = null,
    val addressPrivateKeyPair: AddressPrivateKeyPair? = null
)
