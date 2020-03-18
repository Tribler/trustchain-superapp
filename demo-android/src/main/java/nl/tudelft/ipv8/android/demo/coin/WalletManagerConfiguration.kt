package nl.tudelft.ipv8.android.demo.coin

data class SerializedDeterminsticKey(
    val seed: String,
    val creationTime: Long
)

enum class BitcoinNetworkOptions {
    PRODUCTION, TEST_NET
}

class WalletManagerConfiguration(
    val network: BitcoinNetworkOptions = BitcoinNetworkOptions.TEST_NET,
    val key: SerializedDeterminsticKey? = null
)
