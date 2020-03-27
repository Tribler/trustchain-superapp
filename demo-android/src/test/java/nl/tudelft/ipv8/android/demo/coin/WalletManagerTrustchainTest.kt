package nl.tudelft.ipv8.android.demo.coin

import io.mockk.*
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.android.demo.TrustChainHelper
import org.bitcoinj.core.Coin
import org.junit.Test

class WalletManagerTrustchainTest {

    val ENTRANCE_FEE = 10000L
    val TX_ID = "14781fbecf604e5c6cf67d6383dfc9f3f7d8a163e3a82b1154b90675f0a7752c"
    val TX_SERIALIZED = "0100000001e0fd1e412759b302ef37fc00276b2ba273e17edc7fc441402637fc116aac8883000000006b483045022100ae71317f958f8bda8333fd481e579e7a55e012fc7c90011dd34e79d8fdc5f1e8022020b1d05d7bcda5223973a38f4d6e61fc71cd5ebc4af4264f7e677af5a1934476012102a0f57da74971be3e4299ccb903b994f0bf63a394ec3482f7d009f873512eff4effffffff023336b101000000001976a914ca7811b425f1b4398a638dcb7245a0d7271f942688ac10270000000000002551210357e6900d88e1fecddaaef1dbc392ef9647f0e49c7905b3a273f54a5dae0003a651ae00000000"
    val BTC_PK = "MOCK_BTC_PK"
    val TRUSTCHAIN_PK = "MOCK_TRUSTCHAIN_PK"

    @Test
    fun testTrustGenesisBlock() {
        // Setup mocks
        val walletManager = mockk<WalletManager>()

        mockkObject(WalletManagerAndroid)
        every { WalletManagerAndroid.getInstance() } returns walletManager

        val transactionPackage = mockk<WalletManager.TransactionPackage>()
        every { transactionPackage.transactionId } returns TX_ID
        every { walletManager.safeCreationAndSendGenesisWallet(Coin.valueOf(ENTRANCE_FEE)) } returns transactionPackage
        every { walletManager.attemptToGetTransactionAndSerialize(TX_ID)} returns TX_SERIALIZED
        every { walletManager.networkPublicECKeyHex() } returns BTC_PK


        // Actual test
        val coinCommunity = spyk(CoinCommunity(), recordPrivateCalls = true)
        val txId = coinCommunity.createGenesisSharedWallet(ENTRANCE_FEE)

        every { coinCommunity.myPeer.publicKey.keyToBin() } returns TRUSTCHAIN_PK.toByteArray()

        val serializedTx = coinCommunity.fetchBitcoinTransaction(txId)

        val trustchain = mockk<TrustChainHelper>()
        every { coinCommunity getProperty "trustchain" } returns trustchain

        coinCommunity.broadcastCreatedSharedWallet(serializedTx!!, ENTRANCE_FEE, 1)

    }

}
