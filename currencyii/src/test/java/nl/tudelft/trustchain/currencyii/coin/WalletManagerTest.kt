package nl.tudelft.trustchain.currencyii.coin

import nl.tudelft.ipv8.util.hexToBytes
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.ScriptPattern
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

//    @Test
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

//    @Test
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

//    @Test
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

        assertFalse(
            "The transaction is not, and never will be, confirmed. Should not pass",
            entranceFeePayed
        )
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

        val transaction =
            WalletManager.createMultiSignatureWallet(publicKeys, entranceFee, threshold)

        assertTrue("Not exactly one output in transaction", transaction.outputs.size == 1)

        val output = transaction.outputs[0]
        assertTrue(
            "Transaction output value is not equal to the entrance fee",
            output.value == entranceFee
        )

        val script = output.scriptPubKey
        assertTrue("Script is not a multisig script", ScriptPattern.isSentToMultisig(script))

        assertTrue(
            "Amount of signatures that are needed to complete the transaction is not correct in the script",
            script.numberOfSignaturesRequiredToSpend == threshold
        )

        // Check whether all keys are in the script
        assertTrue(
            "First pubkey did not match first pubkey in script",
            script.pubKeys[0].pubKey.contentEquals(key1.pubKey)
        )
        assertTrue(
            "Second pubkey did not match second pubkey in script",
            script.pubKeys[1].pubKey.contentEquals(key2.pubKey)
        )
        assertTrue(
            "Third pubkey did not match third pubkey in script",
            script.pubKeys[2].pubKey.contentEquals(key3.pubKey)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateMultiSignatureWallet2of3MultiSigThresholdTooHigh() {
        val key1 = ECKey()
        val key2 = ECKey()
        val key3 = ECKey()

        val publicKeys = listOf(key1, key2, key3)
        val entranceFee = Coin.valueOf(100000)
        val threshold = 10

        WalletManager.createMultiSignatureWallet(publicKeys, entranceFee, threshold)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateMultiSignatureWallet2of3MultiSigNegativeThreshold() {
        val key1 = ECKey()
        val key2 = ECKey()
        val key3 = ECKey()

        val publicKeys = listOf(key1, key2, key3)
        val entranceFee = Coin.valueOf(100000)
        val threshold = -1

        WalletManager.createMultiSignatureWallet(publicKeys, entranceFee, threshold)
    }
}
