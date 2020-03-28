package nl.tudelft.ipv8.android.demo.coin

import io.mockk.*
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.android.demo.TrustChainHelper
import nl.tudelft.ipv8.util.hexToBytes
import org.bitcoinj.core.Coin
import org.junit.Test

class WalletManagerTrustchainTest {

    val ENTRANCE_FEE = 10000L
    val TX_ID = "14781fbecf604e5c6cf67d6383dfc9f3f7d8a163e3a82b1154b90675f0a7752c"
    val TX_SERIALIZED = "0100000001e0fd1e412759b302ef37fc00276b2ba273e17edc7fc441402637fc116aac8883000000006b483045022100ae71317f958f8bda8333fd481e579e7a55e012fc7c90011dd34e79d8fdc5f1e8022020b1d05d7bcda5223973a38f4d6e61fc71cd5ebc4af4264f7e677af5a1934476012102a0f57da74971be3e4299ccb903b994f0bf63a394ec3482f7d009f873512eff4effffffff023336b101000000001976a914ca7811b425f1b4398a638dcb7245a0d7271f942688ac10270000000000002551210357e6900d88e1fecddaaef1dbc392ef9647f0e49c7905b3a273f54a5dae0003a651ae00000000"
    val BTC_PK = "mi38Bwzh7GKeTy7w1DNTUF8zNUzoE8LiCs"
    val TRUSTCHAIN_PK = "4c69624e61434c504b3a8d56b1bd19d38e9524c04d1a13f6020e56818829ecb3ba9a97bd395380d8336e2a796f574f4391b5ad795ef9740fb5287c7100909c547c85213ef71c9a932857"

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

        val coinCommunity = spyk(CoinCommunity(), recordPrivateCalls = true)
        every { coinCommunity.myPeer.publicKey.keyToBin() } returns TRUSTCHAIN_PK.hexToBytes()
        val trustchain = mockk<TrustChainHelper>()
        every { coinCommunity getProperty "trustchain" } returns trustchain

        // Actual test
        val txId = coinCommunity.createGenesisSharedWallet(ENTRANCE_FEE)
        val serializedTx = coinCommunity.fetchBitcoinTransaction(txId)

        coinCommunity.broadcastCreatedSharedWallet(serializedTx!!, ENTRANCE_FEE, 1)

        // Verify that the trustchain method is called
        verify { trustchain.createProposalBlock(any<String>(), TRUSTCHAIN_PK.hexToBytes(), CoinCommunity.SHARED_WALLET_BLOCK) }
    }

}
