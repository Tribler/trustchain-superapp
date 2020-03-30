package nl.tudelft.ipv8.android.demo.coin

import nl.tudelft.ipv8.util.hexToBytes
import org.bitcoinj.core.*
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptPattern
import org.junit.Assert.*
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
            WalletManager.createMultiSignatureWallet(
                listOf(ECKey_1, ECKey_2, ECKey_3),
                Coin.ZERO,
                2,
                TestNet3Params.get()
            )

        val scriptPubKey: Script = contract.outputs[0].scriptPubKey
        val scriptPubKeyBytes: ByteArray = contract.outputs[0].scriptBytes

    }

    @Test
    fun testMultiSigFromECDSA() {
        val params = MainNetParams.get()

        val key1: ECKey = ecdsaPubKeyToECKey(
            params,
            "04A97B658C114D77DC5F71736AB78FBE408CE632ED1478D7EAA106EEF67C55D58A91C6449DE4858FAF11721E85FE09EC850C6578432EB4BE9A69C76232AC593C3B"
        )
        val key2: ECKey = ecdsaPubKeyToECKey(
            params,
            "04019EF04A316792F0ECBE5AB1718C833C3964DEE3626CFABE19D97745DBCAA5198919081B456E8EEEA5898AFA0E36D5C17AB693A80D728721128ED8C5F38CDBA0"
        )
        val key3: ECKey = ecdsaPubKeyToECKey(
            params,
            "04A04F29F308160E6F945B33D943304B1B471ED8F9EACEEB5412C04E60A0FAB0376871D9D1108948B67CAFBC703E565A18F8351FB8558FD7C7482D7027EECD687C"
        )

        val keys = listOf(key1, key2, key3)

        val contract = WalletManager.createMultiSignatureWallet(keys, Coin.ZERO, 2, params)

        println("Inputs:")
        for (input in contract.inputs) {
            println(input)
        }
        println("Outputs:")
        for (output in contract.outputs) {
            println(output)
        }
    }

    @Test
    fun testEntranceFeeTransactionWithWitnessTx() {
        val params = MainNetParams.get()

        // Transaction from exchange to our wallet for testing, invalid, since the input uses witnesses to send money, instead of a one-on-one transaction
        val userBitcoinPk =
            LegacyAddress.fromString(params, "bc1qxtk5uxp2ka68za6jxzz7e8xnq5wh5tqxd02fx4")
        val transactionHash =
            Sha256Hash.wrap("a7e93dc1c53d45dbd3bb7f342991c068065059e33fceedf6a3fc0887bd5a056a".hexToBytes())
        val sharedWalletBitcoinPk =
            LegacyAddress.fromString(params, "1K6BWqtaXpDiZ4PEiPR3pNyXb4ZveKRWqm")
        val entranceFee = 0.01831933

        val entranceFeePayed = WalletManager.checkEntranceFeeTransaction(
            userBitcoinPk,
            transactionHash,
            sharedWalletBitcoinPk,
            entranceFee
        )

        assertFalse("The entrance fee should be payed", entranceFeePayed)
    }

    @Test
    fun testEntranceFeeTransactionWithInvalidTx() {
        val params = MainNetParams.get()

        // Transaction that does not exist
        val userBitcoinPk =
            LegacyAddress.fromString(params, "bc1qxtk5uxp2ka68za6jxzz7e8xnq5wh5tqxd02fx4")
        val transactionHash =
            Sha256Hash.wrap("a7e93dc1c53d45ddd3bb7f342991c068065059e33fceedf6a3fc0887bd5a056a".hexToBytes())
        val sharedWalletBitcoinPk =
            LegacyAddress.fromString(params, "1K6BWqtaXpDiZ4PEiPR3pNyXb4ZveKRWqm")
        val entranceFee = 0.01831933

        val entranceFeePayed = WalletManager.checkEntranceFeeTransaction(
            userBitcoinPk,
            transactionHash,
            sharedWalletBitcoinPk,
            entranceFee
        )

        assertFalse("The entrance fee should be payed", entranceFeePayed)
    }

    @Test
    fun testEntranceFeeTransactionValidUnconfirmedTx() {
        val params = MainNetParams.get()

        // Transaction from our wallet to another wallet for testing
        // https://www.blockchain.com/btc/tx/c0f8fdb122ac1f2db46652ac39afa0dd0ef1d9457eff684dc7ac5699ebc34a19
        val userBitcoinPk = LegacyAddress.fromString(params, "1K6BWqtaXpDiZ4PEiPR3pNyXb4ZveKRWqm")
        val transactionHash =
            Sha256Hash.wrap("c0f8fdb122ac1f2db46652ac39afa0dd0ef1d9457eff684dc7ac5699ebc34a19".hexToBytes())
        val sharedWalletBitcoinPk =
            LegacyAddress.fromString(params, "1BoCA9Y3z7NjQuFffdjKPo4PghWGRjsR5Y")
        val entranceFee = 0.00600000

        val entranceFeePayed = WalletManager.checkEntranceFeeTransaction(
            userBitcoinPk,
            transactionHash,
            sharedWalletBitcoinPk,
            entranceFee
        )

        assertFalse("The transaction is not, and never will be, confirmed. Should not pass",
            entranceFeePayed)
    }

    @Test
    fun testEntranceFeeTransactionValid() {
        val params = MainNetParams.get()

        // Confirmed transaction found on blockchain
        // https://www.blockchain.com/btc/tx/3c5771e1c8e40056ef04d96dade01d707fade25c3847f98c93f03715accd4414
        val userBitcoinPk = LegacyAddress.fromString(params, "38tQTweJNJFSv6xM5Zb39ZWqzikk81fMdy")
        val transactionHash =
            Sha256Hash.wrap("3c5771e1c8e40056ef04d96dade01d707fade25c3847f98c93f03715accd4414".hexToBytes())
        val sharedWalletBitcoinPk =
            LegacyAddress.fromString(params, "1CbpHMm5AXRVTuXXZ7rL4wShcRKosdCQPZ")
        val entranceFee = 1.0

        val entranceFeePayed = WalletManager.checkEntranceFeeTransaction(
            userBitcoinPk,
            transactionHash,
            sharedWalletBitcoinPk,
            entranceFee
        )

        assertTrue("The entrance fee should be payed", entranceFeePayed)
    }

    @Test
    fun testCreateMultiSignatureWallet2of3MultiSigCorrect() {
        val key1 = ECKey()
        val key2 = ECKey()
        val key3 = ECKey()

        val publicKeys = listOf(key1, key2, key3)
        val entranceFee = Coin.valueOf(100000)
        val threshold = 2

        val transaction = WalletManager.createMultiSignatureWallet(publicKeys, entranceFee, threshold)

        assertTrue("Not exactly one output in transaction", transaction.outputs.size == 1)

        val output = transaction.outputs[0]
        assertTrue("Transaction output value is not equal to the entrance fee", output.value == entranceFee)

        val script = output.scriptPubKey
        assertTrue("Script is not a multisig script", ScriptPattern.isSentToMultisig(script))

        assertTrue("Amount of signatures that are needed to complete the transaction is not correct in the script",
            script.numberOfSignaturesRequiredToSpend == threshold)

        // Check whether all keys are in the script
        assertTrue("First pubkey did not match first pubkey in script",
            script.pubKeys[0].pubKey.contentEquals(key1.pubKey))
        assertTrue("Second pubkey did not match second pubkey in script",
            script.pubKeys[1].pubKey.contentEquals(key2.pubKey))
        assertTrue("Third pubkey did not match third pubkey in script",
            script.pubKeys[2].pubKey.contentEquals(key3.pubKey))
    }

    @Test
    fun testCreateMultiSignatureWallet2of3MultiSigThresholdTooHigh() {
        val key1 = ECKey()
        val key2 = ECKey()
        val key3 = ECKey()

        val publicKeys = listOf(key1, key2, key3)
        val entranceFee = Coin.valueOf(100000)
        val threshold = 10

        val transaction = WalletManager.createMultiSignatureWallet(publicKeys, entranceFee, threshold)

        assertTrue("Not exactly one output in transaction", transaction.outputs.size == 1)

        val output = transaction.outputs[0]
        assertTrue("Transaction output value is not equal to the entrance fee", output.value == entranceFee)

        val script = output.scriptPubKey
        assertTrue("Script is not a multisig script", ScriptPattern.isSentToMultisig(script))

        // The threshold is set to an amount greater than the amount of keys, the method should
        // make sure the script sets the threshold to the amount of keys, so n-of-n
        assertTrue("Amount of signatures that are needed to complete the transaction is not correct in the script, " +
            "amount of signatures needed is not clamped to the amount of keys",
            script.numberOfSignaturesRequiredToSpend == publicKeys.size)

        // Check whether all keys are in the script
        assertTrue("First pubkey did not match first pubkey in script",
            script.pubKeys[0].pubKey.contentEquals(key1.pubKey))
        assertTrue("Second pubkey did not match second pubkey in script",
            script.pubKeys[1].pubKey.contentEquals(key2.pubKey))
        assertTrue("Third pubkey did not match third pubkey in script",
            script.pubKeys[2].pubKey.contentEquals(key3.pubKey))
    }

    @Test
    fun testCreateMultiSignatureWallet2of3MultiSigNegativeThreshold() {
        val key1 = ECKey()
        val key2 = ECKey()
        val key3 = ECKey()

        val publicKeys = listOf(key1, key2, key3)
        val entranceFee = Coin.valueOf(100000)
        val threshold = -1

        val transaction = WalletManager.createMultiSignatureWallet(publicKeys, entranceFee, threshold)

        assertTrue("Not exactly one output in transaction", transaction.outputs.size == 1)

        val output = transaction.outputs[0]
        assertTrue("Transaction output value is not equal to the entrance fee", output.value == entranceFee)

        val script = output.scriptPubKey
        assertTrue("Script is not a multisig script", ScriptPattern.isSentToMultisig(script))

        // The threshold is set to -1, the method should make sure the script sets the threshold to 1, so 1-of-n
        assertTrue("Amount of signatures that are needed to complete the transaction is not correct in the script" +
            "amount of signatures needed is not set to 1 because of negative threshold",
            script.numberOfSignaturesRequiredToSpend == 1)

        // Check whether all keys are in the script
        assertTrue("First pubkey did not match first pubkey in script",
            script.pubKeys[0].pubKey.contentEquals(key1.pubKey))
        assertTrue("Second pubkey did not match second pubkey in script",
            script.pubKeys[1].pubKey.contentEquals(key2.pubKey))
        assertTrue("Third pubkey did not match third pubkey in script",
            script.pubKeys[2].pubKey.contentEquals(key3.pubKey))
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
