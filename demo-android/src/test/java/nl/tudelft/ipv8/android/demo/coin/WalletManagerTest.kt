package nl.tudelft.ipv8.android.demo.coin

import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import org.bitcoinj.script.Script
import org.junit.BeforeClass
import nl.tudelft.ipv8.util.hexToBytes
import org.bitcoinj.core.*
import org.bitcoinj.params.MainNetParams
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
            WalletManager.createMultiSignatureWallet(listOf(ECKey_1, ECKey_2, ECKey_3), 2)

        val scriptPubKey: Script = contract.outputs[0].scriptPubKey
        val scriptPubKeyBytes: ByteArray = contract.outputs[0].scriptBytes

    }

    @Test
    fun testMultiSigFromECDSA() {
        val params = MainNetParams.get()

        val key1: ECKey = ecdsaPubKeyToECKey(params, "04A97B658C114D77DC5F71736AB78FBE408CE632ED1478D7EAA106EEF67C55D58A91C6449DE4858FAF11721E85FE09EC850C6578432EB4BE9A69C76232AC593C3B")
        val key2: ECKey = ecdsaPubKeyToECKey(params, "04019EF04A316792F0ECBE5AB1718C833C3964DEE3626CFABE19D97745DBCAA5198919081B456E8EEEA5898AFA0E36D5C17AB693A80D728721128ED8C5F38CDBA0")
        val key3: ECKey = ecdsaPubKeyToECKey(params, "04A04F29F308160E6F945B33D943304B1B471ED8F9EACEEB5412C04E60A0FAB0376871D9D1108948B67CAFBC703E565A18F8351FB8558FD7C7482D7027EECD687C")

        val keys = listOf(key1, key2, key3)

        val contract = WalletManager.createMultiSignatureWallet(keys, 2)

        println("Inputs:")
        for (input in contract.inputs) {
            println(input)
        }
        println("Outputs:")
        for (output in contract.outputs) {
            println(output)
        }
    }

    fun ecdsaPubKeyToAddress(params: NetworkParameters, ecdsaPubKey: String): Address {
        val ecKey = ECKey.fromPublicOnly(ecdsaPubKey.hexToBytes())
        return LegacyAddress.fromKey(params, ecKey)
    }

    fun ecdsaPubKeyToECKey(params: NetworkParameters, ecdsaPubKey: String): ECKey {
        val ecKey = ECKey.fromPublicOnly(ecdsaPubKey.hexToBytes())
        return ecKey
    }

}
