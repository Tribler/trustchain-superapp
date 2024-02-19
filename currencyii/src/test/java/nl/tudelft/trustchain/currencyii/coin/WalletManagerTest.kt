package nl.tudelft.trustchain.currencyii.coin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class WalletManagerTest {
    companion object {
        lateinit var walletManager: WalletManager

        @BeforeAll
        @JvmStatic
        fun setup() {
            val config = WalletManagerConfiguration(BitcoinNetworkOptions.REG_TEST)
            walletManager =
                WalletManager(
                    config,
                    File(".")
                )
        }
    }

    /**
     * Wallet address should be correct format (same regex as used on regtest server).
     */
    @Test
    fun testProtocolAddress() {
        val addressRegex = Regex("[a-km-zA-HJ-NP-Z1-9]{25,50}$")
        val actual = walletManager.protocolAddress()
        val matches: Boolean = actual.toString().matches(addressRegex)
        assertTrue(matches)
    }
}
