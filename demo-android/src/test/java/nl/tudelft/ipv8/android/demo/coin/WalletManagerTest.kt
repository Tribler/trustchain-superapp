package nl.tudelft.ipv8.android.demo.coin

import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import org.bitcoinj.script.Script
import org.junit.BeforeClass
import org.junit.Test
import java.io.File


class WalletManagerTest {

    companion object {
        lateinit var walletManager: WalletManager

        @BeforeClass
        @JvmStatic
        fun setup() {
            val config = WalletManagerConfiguration(BitcoinNetworkOptions.PRODUCTION)
            walletManager = WalletManager(
                config,
                File(".")
            )
        }
    }

    @Test
    fun testCreateMultisignatureWallet() {
        walletManager.printWalletInfo()
        val ECKey_1: ECKey =
            WalletManager.privateKeyStringToECKey("5KffUB9YUsvoGjrcn76PjVnC61PcWLzws4QPfrT9RFNd85utCkZ")
        val ECKey_2: ECKey =
            WalletManager.privateKeyStringToECKey("5KawqZHB1H6Af12ZhgTBXwQUY1jACgvGMywET7NF5bdYYzCxomY")
        val ECKey_3: ECKey =
            WalletManager.privateKeyStringToECKey("5KiDGG8sfmTNnzKDmm1MteWHV2TQQaUBbaEY3huVLwVz1i6i5be")

        println("Keys used for 2-3 MultiSig:")
        println(ECKey_1.privateKeyAsHex)
        println(ECKey_2.privateKeyAsHex)
        println(ECKey_3.privateKeyAsHex)

        val contract: Transaction =
            WalletManager.createMultiSignatureWallet(ECKey_1, listOf(ECKey_2, ECKey_3), 2)

        val scriptPubKey: Script = contract.outputs[0].scriptPubKey
        val scriptPubKeyBytes: ByteArray = contract.outputs[0].scriptBytes

    }


}
