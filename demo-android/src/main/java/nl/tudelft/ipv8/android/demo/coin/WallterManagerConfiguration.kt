package nl.tudelft.ipv8.android.demo.coin

enum class BitcoinNetworkOptions {
    PRODUCTION, TEST_NET
}

class WalletManagerConfiguration(
    val network: BitcoinNetworkOptions = BitcoinNetworkOptions.TEST_NET
)
