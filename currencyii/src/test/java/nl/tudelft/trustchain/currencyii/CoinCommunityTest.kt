@file:Suppress("DEPRECATION")

package nl.tudelft.trustchain.currencyii

// import io.mockk.*
// import nl.tudelft.ipv8.IPv8
// import nl.tudelft.ipv8.android.IPv8Android
// import nl.tudelft.trustchain.currencyii.coin.WalletManager
// import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
// import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
// import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
// import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
// import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
// import nl.tudelft.ipv8.util.hexToBytes
// import nl.tudelft.trustchain.currencyii.util.DAOCreateHelper
// import nl.tudelft.trustchain.currencyii.util.DAOJoinHelper
// import org.bitcoinj.core.Coin
// import org.bitcoinj.params.TestNet3Params
// import org.junit.jupiter.api.Test

class CoinCommunityTest {

//    val ENTRANCE_FEE = 10000L
//
//    val FUNDS_TRANSFER_AMOUNT = 1000L
//    val FUNDS_RECEIVER_ADDRESS = "mo4nNbGVfJGozjtD9ViXvcgW1fjq9stdzd"
//
//    val TRUSTCHAIN_PK = "4c69624e61434c504b3a8d56b1bd19d38e9524c04d1a13f6020e56818829ecb3ba9a97bd395380d8336e2a796f574f4391b5ad795ef9740fb5287c7100909c547c85213ef71c9a932857"
//
// //     BTC keys
//    val BTC_PK = "mi38Bwzh7GKeTy7w1DNTUF8zNUzoE8LiCs"
//    val BTC_PK_2 = "zHeLWRUv1jJn6ciWYMjJrPxv6Gg96US6Fz"
//
// //     TX 1: Creation wallet
//    val TX_CREATE_WALLET_ID = "14781fbecf604e5c6cf67d6383dfc9f3f7d8a163e3a82b1154b90675f0a7752c"
//    val TX_CREATE_WALLET_SERIALIZED = "0100000001e0fd1e412759b302ef37fc00276b2ba273e17edc7fc441402637fc116aac8883000000006b483045022100ae71317f958f8bda8333fd481e579e7a55e012fc7c90011dd34e79d8fdc5f1e8022020b1d05d7bcda5223973a38f4d6e61fc71cd5ebc4af4264f7e677af5a1934476012102a0f57da74971be3e4299ccb903b994f0bf63a394ec3482f7d009f873512eff4effffffff023336b101000000001976a914ca7811b425f1b4398a638dcb7245a0d7271f942688ac10270000000000002551210357e6900d88e1fecddaaef1dbc392ef9647f0e49c7905b3a273f54a5dae0003a651ae00000000"
//
// //     TX 2: Add user to wallet
//    val TX_ADD_USER_ID = "14781fbecf604e5c6cf67d6383dfc9f3f7d8a163e3a82b1154b90675f0a7752c"
//    val TX_ADD_USER_SERIALIZED = "0100000001c85d15fd6a09dd0abb81c02073e8122cfee753272bdb45319ece699217f9466e000000009100473044022033c5b2b3a6a170aa5f236ddff5b43d50de95ca04b7d6a0656e2866bbfc31a882022018c6edf69a39ae7ce4dbb9c7f837214e34cf78f0c886832137a5e383d13fe5bf0147304402201ecbd8f36e85fd27d301e8142ae7dbf070d4b7ce6c8cb2067f0db62d3e5e793802206c70650fb76f0f30885cbd9e7a18ad92f31570b57db1a2800a717530841e0b1f01ffffffff02e8030000000000001976a91452cde8b3a35836ecb011d82882dd3d02e8e895e188acad3e0000000000006752210357e6900d88e1fecddaaef1dbc392ef9647f0e49c7905b3a273f54a5dae0003a64104b8439dec935d9abda33194d27a1d0a86f4e52b702475ed13d4ae83501e50d5d71402189b6229163e1afbae663eb0df305e2d9b5b61bdcd0125a2360d2cd3200e52ae00000000"
//
// //     TX 3: Transfer funds
//    val TX_TRANSFER_FUNDS_ID = "768876624493431b079ec883326e4c23bddba20a0c870bfe23989ca0991c88fc"
//    val TX_TRANSFER_FUNDS_SERIALIZED = "0100000001c85d15fd6a09dd0abb81c02073e8122cfee753272bdb45319ece699217f9466e000000009100473044022033c5b2b3a6a170aa5f236ddff5b43d50de95ca04b7d6a0656e2866bbfc31a882022018c6edf69a39ae7ce4dbb9c7f837214e34cf78f0c886832137a5e383d13fe5bf0147304402201ecbd8f36e85fd27d301e8142ae7dbf070d4b7ce6c8cb2067f0db62d3e5e793802206c70650fb76f0f30885cbd9e7a18ad92f31570b57db1a2800a717530841e0b1f01ffffffff02e8030000000000001976a91452cde8b3a35836ecb011d82882dd3d02e8e895e188acad3e0000000000006752210357e6900d88e1fecddaaef1dbc392ef9647f0e49c7905b3a273f54a5dae0003a64104b8439dec935d9abda33194d27a1d0a86f4e52b702475ed13d4ae83501e50d5d71402189b6229163e1afbae663eb0df305e2d9b5b61bdcd0125a2360d2cd3200e52ae00000000"
//
//    val SW_BLOCK_HASH = ByteArray(10)
//
//    // 1.1 + 1.2
//    @Test
//    fun testTrustGenesisBlock() {
//        // Setup mocks
//        val walletManager = mockk<WalletManager>()
//        mockkObject(WalletManagerAndroid)
//        every { WalletManagerAndroid.getInstance() } returns walletManager
//
//        val transactionPackage = mockk<WalletManager.TransactionPackage>()
//        every { transactionPackage.transactionId } returns TX_CREATE_WALLET_ID
//        every { walletManager.safeCreationAndSendGenesisWallet(Coin.valueOf(ENTRANCE_FEE)) } returns transactionPackage
//        every { walletManager.attemptToGetTransactionAndSerialize(TX_CREATE_WALLET_ID) } returns TX_CREATE_WALLET_SERIALIZED
//        every { walletManager.networkPublicECKeyHex() } returns BTC_PK
//
//        val coinCommunity = spyk(CoinCommunity(), recordPrivateCalls = true)
//        val trustchain = mockk<TrustChainHelper>()
//        every { coinCommunity.myPeer.publicKey.keyToBin() } returns TRUSTCHAIN_PK.hexToBytes()
//        every { coinCommunity getProperty "trustchain" } returns trustchain
//        every { trustchain.createProposalBlock(any<String>(), any<ByteArray>(), any<String>()) } returns Unit
//
//        // Actual test
//        val txId = coinCommunity.createBitcoinGenesisWallet(ENTRANCE_FEE)
//        val serializedTx = coinCommunity.fetchBitcoinTransaction(txId)
//        private val daoCreateHelper = DAOCreateHelper()
//        daoCreateHelper.broadcastCreatedSharedWallet(serializedTx!!, ENTRANCE_FEE.toString(), 1)
//
//        // Verify that the trustchain method is called
//        verify { trustchain.createProposalBlock(any<String>(), TRUSTCHAIN_PK.hexToBytes(),
//            CoinCommunity.JOIN_BLOCK
//        ) }
//    }

    // 2.1: Join wallet of BTC_PK as BTC_PK_2
//    @Test
//    fun testTrustchainCreateBitcoinSharedWallet() {
//        // Setup mocks
//        val walletManager = mockk<WalletManager>()
//        mockkObject(WalletManagerAndroid)
//        every { WalletManagerAndroid.getInstance() } returns walletManager
//        every { walletManager.networkPublicECKeyHex() } returns BTC_PK_2
//        every { walletManager.params } returns TestNet3Params.get()
//
//        val coinCommunity = spyk(CoinCommunity(), recordPrivateCalls = true)
//        val trustchain = mockk<TrustChainHelper>()
//        every { coinCommunity.myPeer.publicKey.keyToBin() } returns TRUSTCHAIN_PK.hexToBytes()
//        every { coinCommunity getProperty "trustchain" } returns trustchain
//        every { trustchain.createProposalBlock(any<String>(), any(), any()) } returns Unit
//
//        val trustChainCommunity = mockk<TrustChainCommunity>()
//        val trustChainStore = mockk<TrustChainStore>()
//        val iPv8 = mockk<IPv8>()
//        val swJoinBlock = mockk<TrustChainBlock>()
//        mockkObject(IPv8Android)
//        every { IPv8Android.getInstance() } returns iPv8
//        every { iPv8.getOverlay<TrustChainCommunity>() } returns trustChainCommunity
//        every { trustChainCommunity.database } returns trustChainStore
//        every { trustChainStore.getBlockWithHash(SW_BLOCK_HASH) } returns swJoinBlock
//
//        // Setup mock trustchain block
//        val blockData = SWJoinBlockTransactionData(
//            ENTRANCE_FEE,
//            TX_CREATE_WALLET_SERIALIZED,
//            100,
//            arrayListOf(TRUSTCHAIN_PK),
//            arrayListOf(BTC_PK)
//        )
//        every { swJoinBlock.transaction } returns hashMapOf("message" to blockData.getJsonString())
//
//        val newTransactionProposalMock = WalletManager.TransactionPackage(TX_ADD_USER_ID, TX_ADD_USER_SERIALIZED)
//        every { walletManager.safeCreationJoinWalletTransaction(any(), any(), any(), any()) } returns newTransactionProposalMock
//        // Actual test
//        val newTransactionProposal =
//            coinCommunity.createBitcoinSharedWalletForJoining(SW_BLOCK_HASH)
//
//        assertEquals("Old wallet TX in block is not correct", TX_ADD_USER_ID, newTransactionProposal.transactionId)
//        assertEquals("New wallet TX in block is not correct", TX_ADD_USER_SERIALIZED, newTransactionProposal.serializedTransaction)
//
//        val verifyTransaction = Transaction(walletManager.params, TX_CREATE_WALLET_SERIALIZED.hexToBytes())
//        verify(exactly = 1) {
//            walletManager.safeCreationJoinWalletTransaction(
//                arrayListOf(BTC_PK, BTC_PK_2),
//                Coin.valueOf(ENTRANCE_FEE),
//                verifyTransaction,
//                2)
//        }
//    }

    // 2.3
//    @Test
//    fun testTrustchainAddSharedWalletJoinBlock() {
//        // Setup mocks
//        val walletManager = mockk<WalletManager>()
//        mockkObject(WalletManagerAndroid)
//        every { WalletManagerAndroid.getInstance() } returns walletManager
//        every { walletManager.networkPublicECKeyHex() } returns BTC_PK
//        every { walletManager.params } returns TestNet3Params.get()
//
//        val coinCommunity = spyk(CoinCommunity(), recordPrivateCalls = true)
//        val trustchain = mockk<TrustChainHelper>()
//        every { coinCommunity.myPeer.publicKey.keyToBin() } returns TRUSTCHAIN_PK.hexToBytes()
//        every { coinCommunity getProperty "trustchain" } returns trustchain
//        every { trustchain.createProposalBlock(any<String>(), any(), any()) } returns Unit
//
//        val trustChainCommunity = mockk<TrustChainCommunity>()
//        val trustChainStore = mockk<TrustChainStore>()
//        val iPv8 = mockk<IPv8>()
//        val swJoinBlock = mockk<TrustChainBlock>()
//        mockkObject(IPv8Android)
//        every { IPv8Android.getInstance() } returns iPv8
//        every { iPv8.getOverlay<TrustChainCommunity>() } returns trustChainCommunity
//        every { trustChainCommunity.database } returns trustChainStore
//        every { trustChainStore.getBlockWithHash(SW_BLOCK_HASH) } returns swJoinBlock
//
//        // Setup mock trustchain block
//        val blockData = SWJoinBlockTransactionData(
//            ENTRANCE_FEE,
//            TX_CREATE_WALLET_SERIALIZED,
//            100,
//            arrayListOf(TRUSTCHAIN_PK),
//            arrayListOf(BTC_PK)
//        )
//        every { swJoinBlock.transaction } returns hashMapOf("message" to blockData.getJsonString())

    // Actual test
//        coinCommunity.broadcastJoinedSharedWallet(SW_BLOCK_HASH)
//
//        verify {
//            trustchain.createProposalBlock(
//                any<String>(),
//                any(),
//                CoinCommunity.JOIN_BLOCK
//            )
//        }
//    }

    // 2.2 + 2.4
//    @Test
//    fun testTrustchainSafeSendingJoinWalletTransaction() {
//        // Setup mocks
//        val walletManager = mockk<WalletManager>()
//        mockkObject(WalletManagerAndroid)
//        every { WalletManagerAndroid.getInstance() } returns walletManager
//        every { walletManager.networkPublicECKeyHex() } returns BTC_PK
//        every { walletManager.params } returns TestNet3Params.get()
//
//        val coinCommunity = spyk(CoinCommunity(), recordPrivateCalls = true)
//        val trustchain = mockk<TrustChainHelper>()
//        every { coinCommunity.myPeer.publicKey.keyToBin() } returns TRUSTCHAIN_PK.hexToBytes()
//        every { coinCommunity getProperty "trustchain" } returns trustchain
//        every { trustchain.createProposalBlock(any<String>(), any(), any()) } returns Unit
//
//        val trustChainCommunity = mockk<TrustChainCommunity>()
//        val trustChainStore = mockk<TrustChainStore>()
//        val iPv8 = mockk<IPv8>()
//        val swJoinBlock = mockk<TrustChainBlock>()
//        mockkObject(IPv8Android)
//        every { IPv8Android.getInstance() } returns iPv8
//        every { iPv8.getOverlay<TrustChainCommunity>() } returns trustChainCommunity
//        every { trustChainCommunity.database } returns trustChainStore
//        every { trustChainStore.getBlockWithHash(SW_BLOCK_HASH) } returns swJoinBlock
//
//        val blockData = SWJoinBlockTransactionData(
//            ENTRANCE_FEE,
//            TX_CREATE_WALLET_SERIALIZED,
//            1,
//            arrayListOf(TRUSTCHAIN_PK),
//            arrayListOf(BTC_PK)
//        )
//
//        val trustChainTransaction = hashMapOf("message" to blockData.getJsonString())
//        every { swJoinBlock.transaction } returns trustChainTransaction
//
//        val newTransactionPackage = WalletManager.TransactionPackage("id", "serialized")
//        every { walletManager.safeSendingJoinWalletTransaction(any(), any(), any()) } returns newTransactionPackage
//
//        // Actual test
//        val swSignatureAskTransactionData =
//            coinCommunity.proposeJoinWalletOnTrustChain(SW_BLOCK_HASH, TX_ADD_USER_SERIALIZED)
//
//        // just using an empty list of signatures since walletManager is mocked anyway
//        val sigList = listOf<String>()
//        val sigListECDSA = sigList.map {
//            ECKey.ECDSASignature.decodeFromDER(it.hexToBytes())
//        }
//        coinCommunity.safeSendingJoinWalletTransaction(swSignatureAskTransactionData, sigList)
//
//        val txOld = Transaction(walletManager.params, TX_CREATE_WALLET_SERIALIZED.hexToBytes())
//        val txNew = Transaction(walletManager.params, TX_ADD_USER_SERIALIZED.hexToBytes())
//
//        verify {
//            walletManager.safeSendingJoinWalletTransaction(
//                sigListECDSA,
//                txNew,
//                txOld
//            )
//        }
//    }

    // 3.1
//    @Test
//    fun testTrustchainAskForTransferFundsSignatures() {
//        // Setup mocks
//        val walletManager = mockk<WalletManager>()
//        mockkObject(WalletManagerAndroid)
//        every { WalletManagerAndroid.getInstance() } returns walletManager
//        every { walletManager.networkPublicECKeyHex() } returns BTC_PK
//        every { walletManager.params } returns TestNet3Params.get()
//
//        val coinCommunity = spyk(CoinCommunity(), recordPrivateCalls = true)
//        val trustchain = mockk<TrustChainHelper>()
//        every { coinCommunity.myPeer.publicKey.keyToBin() } returns TRUSTCHAIN_PK.hexToBytes()
//        every { coinCommunity getProperty "trustchain" } returns trustchain
//        every { trustchain.createProposalBlock(any<String>(), any(), any()) } returns Unit
//
//        val trustChainCommunity = mockk<TrustChainCommunity>()
//        val trustChainStore = mockk<TrustChainStore>()
//        val iPv8 = mockk<IPv8>()
//        val swJoinBlock = mockk<TrustChainBlock>()
//        mockkObject(IPv8Android)
//        every { IPv8Android.getInstance() } returns iPv8
//        every { iPv8.getOverlay<TrustChainCommunity>() } returns trustChainCommunity
//        every { trustChainCommunity.database } returns trustChainStore
//        every { trustChainStore.getBlockWithHash(SW_BLOCK_HASH) } returns swJoinBlock
//
//        val blockData = SWJoinBlockTransactionData(
//            ENTRANCE_FEE,
//            TX_CREATE_WALLET_SERIALIZED,
//            100,
//            arrayListOf("trustchain_pk1"),
//            arrayListOf("btc_pk1")
//        )
//
//        val trustChainTransaction = hashMapOf("message" to blockData.getJsonString())
//        every { swJoinBlock.transaction } returns trustChainTransaction
//
//        val newTransactionProposal = WalletManager.TransactionPackage("id", "serialized")
//        every { walletManager.safeCreationJoinWalletTransaction(any(), any(), any(), any()) } returns newTransactionProposal
//
//        // Actual test
//        val askSignatureBlockData =
//            coinCommunity.askForTransferFundsSignatures(SW_BLOCK_HASH, FUNDS_RECEIVER_ADDRESS, FUNDS_TRANSFER_AMOUNT)
//
//        assertEquals("Signatures required invalid", 1, askSignatureBlockData.getData().SW_SIGNATURES_REQUIRED)
//
//        verify {
//            trustchain.createProposalBlock(
//                any<String>(),
//                any(),
//                CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK
//            )
//        }
//    }

    // 3.2
//    @Test
//    fun testTrustchainTransferFunds() {
//        // Setup mocks
//        val walletManager = mockk<WalletManager>()
//        mockkObject(WalletManagerAndroid)
//        every { WalletManagerAndroid.getInstance() } returns walletManager
//        every { walletManager.networkPublicECKeyHex() } returns BTC_PK
//        every { walletManager.params } returns TestNet3Params.get()
//
//        val coinCommunity = spyk(CoinCommunity(), recordPrivateCalls = true)
//        val trustchain = mockk<TrustChainHelper>()
//        every { coinCommunity.myPeer.publicKey.keyToBin() } returns TRUSTCHAIN_PK.hexToBytes()
//        every { coinCommunity getProperty "trustchain" } returns trustchain
//        every { trustchain.createProposalBlock(any<String>(), any(), any()) } returns Unit
//
//        val trustChainCommunity = mockk<TrustChainCommunity>()
//        val trustChainStore = mockk<TrustChainStore>()
//        val iPv8 = mockk<IPv8>()
//        val swJoinBlock = mockk<TrustChainBlock>()
//        mockkObject(IPv8Android)
//        every { IPv8Android.getInstance() } returns iPv8
//        every { iPv8.getOverlay<TrustChainCommunity>() } returns trustChainCommunity
//        every { trustChainCommunity.database } returns trustChainStore
//        every { coinCommunity["fetchLatestSharedWalletTransactionBlock"](SW_BLOCK_HASH) } returns swJoinBlock
//        every { coinCommunity["tryToFetchSerializedTransaction"](swJoinBlock) } returns TX_ADD_USER_SERIALIZED
//
//        val blockData = SWJoinBlockTransactionData(
//            ENTRANCE_FEE,
//            TX_ADD_USER_SERIALIZED,
//            100,
//            arrayListOf(TRUSTCHAIN_PK, TRUSTCHAIN_PK),
//            arrayListOf(BTC_PK, BTC_PK_2)
//        )
//
//        val trustChainTransaction = hashMapOf("message" to blockData.getJsonString())
//        every { swJoinBlock.transaction } returns trustChainTransaction
//
//        val newTransactionProposal = WalletManager.TransactionPackage(TX_TRANSFER_FUNDS_ID, TX_TRANSFER_FUNDS_SERIALIZED)
//        every {
//            walletManager.safeSendingTransactionFromMultiSig(
//                any(),
//                any(),
//                any(),
//                any()
//            )
//        } returns newTransactionProposal
//
//        // Actual test
//        // just using an empty list of signatures since walletManager is mocked anyway
//        val serializedSignatures = listOf<String>()
//        val sigListECDSA = serializedSignatures.map {
//            ECKey.ECDSASignature.decodeFromDER(it.hexToBytes())
//        }
//        val txPackage =
//            coinCommunity.transferFunds(serializedSignatures, SW_BLOCK_HASH, FUNDS_RECEIVER_ADDRESS, FUNDS_TRANSFER_AMOUNT)
//
//        val verifyTransaction = Transaction(walletManager.params, TX_TRANSFER_FUNDS_SERIALIZED.hexToBytes())
//        val verifyAddress = Address.fromString(walletManager.params, FUNDS_RECEIVER_ADDRESS)
//        val verifyCoinAmount = Coin.valueOf(FUNDS_TRANSFER_AMOUNT)
//        verify {
//            walletManager.safeSendingTransactionFromMultiSig(
//            verifyTransaction,
//            sigListECDSA,
//            verifyAddress,
//            verifyCoinAmount)
//        }
//
//        assertEquals("Tx ID is not correct in tx package", TX_TRANSFER_FUNDS_ID, txPackage.transactionId)
//        assertEquals("Serialized Tx is not correct in tx package", TX_TRANSFER_FUNDS_SERIALIZED, txPackage.serializedTransaction)
//    }
}
