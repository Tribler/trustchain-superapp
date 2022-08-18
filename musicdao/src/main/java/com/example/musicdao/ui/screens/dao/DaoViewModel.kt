package com.example.musicdao.ui.screens.dao

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicdao.ui.SnackbarHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.currencyii.coin.*
import nl.tudelft.trustchain.currencyii.sharedWallet.*
import nl.tudelft.trustchain.currencyii.util.taproot.CTransaction
import org.bitcoinj.core.Coin
import prettyPrint
import javax.inject.Inject

@HiltViewModel
class DaoViewModel @Inject constructor() : ViewModel() {

    private val _daos: MutableStateFlow<List<DAO>> = MutableStateFlow(listOf())

    var walletManager: WalletManager? = null
    var daos: MutableStateFlow<Map<TrustChainBlock, DAO>> = MutableStateFlow(mapOf())

    fun initManager(context: Context) {
        Log.d("MVDAO", "INITIATING DAO MODULE.")
        if (WalletManagerAndroid.isInitialized()) {
            Log.d("MVDAO", "DAO MODULE ALREADY INITIALIZED, SKIPPING")
            return
        }
        val params = BitcoinNetworkOptions.REG_TEST

//        val seed = WalletManager.generateRandomDeterministicSeed(params)
        val seed = SerializedDeterministicKey(
            "spell seat genius horn argue family steel buyer spawn chef guard vast",
            1583488954L
        )
        val seed_word = seed.seed
        val creationNumber = seed.creationTime.toLong()

        val config = WalletManagerConfiguration(
            params,
            SerializedDeterministicKey(seed_word, creationNumber),
            null
        )

        WalletManagerAndroid.Factory(context)
            .setConfiguration(config)
            .init()

        this.walletManager = WalletManagerAndroid.getInstance()
        Log.d("MVDAO", "Wallet manager: $walletManager")
        Log.d("MVDAO", walletManager?.kit?.wallet().toString())
        viewModelScope.launch {
            refresh()
        }
    }

    fun createGenesisDAO(currentEntranceFee: Long, currentThreshold: Int, context: Context) {
        try {
            val newDAO = getDaoCommunity().createBitcoinGenesisWallet(
                currentEntranceFee,
                currentThreshold,
                context
            )
            walletManager?.addNewNonceKey(newDAO.getData().SW_UNIQUE_ID, context)
            SnackbarHandler.displaySnackbar("DAO successfully created and broadcast.")
        } catch (e: Exception) {
            SnackbarHandler.displaySnackbar("Unexpected error occurred. Try again")
            e.printStackTrace()
        }
    }

    fun refreshOneShot() {
        viewModelScope.launch {
            refresh()
        }
    }

    suspend fun refresh() {
        Log.d("MVDAO", "Updating DAOs from network. ")

        // Crawl for new wallets on the network.
        crawlAvailableSharedWallets()
        crawlProposalsAndUpdateIfNewFound()

        val proposals = getDaoCommunity().fetchProposalBlocks()
        val wallets = getDaoCommunity().discoverSharedWallets()

        Log.d("MVDAO", "Found ${wallets.size} DAOs on the network.")
        Log.d("MVDAO", "Found ${proposals.size} proposals on the network.")

        proposals.forEach { proposalBlock ->
            val daoId = daoIdFromProposal(proposalBlock)
            Log.d("MVDAO", daoId)
        }

        val aggregated = wallets.associateWith { daoBlock ->
            val blockData = SWJoinBlockTransactionData(daoBlock.transaction).getData()
            aggregate(
                daoBlock,
                proposals.filter { proposalBlock ->
                    Log.d("MVDAO", "Check ${daoIdFromProposal(proposalBlock)} == ${blockData.SW_UNIQUE_ID}")

                    daoIdFromProposal(proposalBlock) == blockData.SW_UNIQUE_ID
                }
            )
        }

        daos.value = aggregated

        Log.d("MVDAO", "Currently ${daos.value.size} DAOs in the network.")
        daos.value.forEach() {
            Log.d("MVDAO", it.prettyPrint())
        }
    }

    fun aggregate(
        trustChainWalletBlock: TrustChainBlock,
        proposalBlocks: List<TrustChainBlock>
    ): DAO {
        val blockData = SWJoinBlockTransactionData(trustChainWalletBlock.transaction).getData()

        Log.d("MVDAO", "1 - ${proposalBlocks.size}")
        val proposals = proposalBlocks.mapNotNull { block ->
            Log.d("MVDAO", "1.5 - ${block.type}")

            if (block.type == DaoCommunity.SIGNATURE_ASK_BLOCK) {
                Log.d("MVDAO", "1.51")

                val data = SWSignatureAskTransactionData(block.transaction).getData()
                return@mapNotNull Proposal(
                    proposalId = data.SW_UNIQUE_PROPOSAL_ID,
                    daoId = data.SW_UNIQUE_ID,
                    signaturesRequired = data.SW_SIGNATURES_REQUIRED,
                    proposalCreator = block.publicKey.toHex(),
                    proposalTime = block.timestamp.toString(),
                    proposalText = "",
                    proposalTitle = "",
                    signatures = listOf(),
                    transferAmountBitcoinSatoshi = 20,
                    transferAddress = "TRANSFER_ADDRESS"
                )
            } else if (block.type == DaoCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
                Log.d("MVDAO", "1.52")

                val data = SWTransferFundsAskTransactionData(block.transaction).getData()
                return@mapNotNull Proposal(
                    proposalId = data.SW_UNIQUE_PROPOSAL_ID,
                    daoId = data.SW_UNIQUE_ID,
                    signaturesRequired = data.SW_SIGNATURES_REQUIRED,
                    proposalCreator = block.publicKey.toHex(),
                    proposalTime = block.timestamp.toString(),
                    proposalText = data.SW_UNIQUE_ID,
                    proposalTitle = "",
                    signatures = listOf(),
                    transferAmountBitcoinSatoshi = data.SW_TRANSFER_FUNDS_AMOUNT.toInt(),
                    transferAddress = data.SW_TRANSFER_FUNDS_TARGET_SERIALIZED
                )
            } else {
                return@mapNotNull null
            }

        }
        Log.d("MVDAO", "2 - ${proposals.size}")
        val dao = DAO(
            daoId = blockData.SW_UNIQUE_ID,
            name = blockData.SW_UNIQUE_ID,
            about = "",
            proposals = proposals,
            members = blockData.SW_BITCOIN_PKS.zip(blockData.SW_TRUSTCHAIN_PKS).map {
                Member(
                    it.first,
                    it.second
                )
            },
            threshHold = blockData.SW_VOTING_THRESHOLD,
            entranceFee = blockData.SW_ENTRANCE_FEE,
            previousTransaction = blockData.SW_TRANSACTION_SERIALIZED,
            balance = getBalance(trustChainWalletBlock.calculateHash()).value
        )

        return dao
    }

    /**
     * Crawl all shared wallet blocks of users in the trust chain.
     */
    private suspend fun crawlProposalsAndUpdateIfNewFound() {
        val allUsers = getDaoCommunity().getPeers()
        Log.d("MVDAO", "Found ${allUsers.size} peers, crawling")

        for (peer in allUsers) {
            try {
                // TODO: Commented this line out, it causes the app to crash
//                withTimeout(JoinDAOFragment.SW_CRAWLING_TIMEOUT_MILLI) {
                trustchain.crawlChain(peer)
                val crawlResult = trustchain
                    .getChainByUser(peer.publicKey.keyToBin())
                    .filter {
                        (
                            it.type == CoinCommunity.SIGNATURE_ASK_BLOCK ||
                                it.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK
                            ) && !getDaoCommunity().checkEnoughFavorSignatures(it)
                    }
                Log.d(
                    "MVDAO",
                    "Crawl result: ${crawlResult.size} proposals found (from ${peer.address})"
                )
                if (crawlResult.isNotEmpty()) {
//                    updateProposals(crawlResult)
//                    updateProposalListUI()
                }
//                }
            } catch (t: Throwable) {
                val message = t.message ?: "no message"
                Log.d("MVDAO", "Crawling failed for: ${peer.address} message: $message")
            }
        }
    }


    private fun daoIdFromProposal(block: TrustChainBlock): String {
        if (block.type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
            return SWSignatureAskTransactionData(block.transaction).getData().SW_UNIQUE_ID
        }

        if (block.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
            return SWTransferFundsAskTransactionData(block.transaction).getData().SW_UNIQUE_ID
        }

        return "invalid-pk"
    }

    private suspend fun crawlAvailableSharedWallets() {
        val allUsers = getDaoCommunity().getPeers()
        Log.i("Coin", "Found ${allUsers.size} peers, crawling")

        for (peer in allUsers) {
            try {
                // TODO: Commented this line out, it causes the app to crash
//                withTimeout(SW_CRAWLING_TIMEOUT_MILLI) {
                trustchain.crawlChain(peer)
                val crawlResult = trustchain
                    .getChainByUser(peer.publicKey.keyToBin())
            } catch (t: Throwable) {
                val message = t.message ?: "No further information"
                Log.e("MVDAO", "Crawling failed for: ${peer.publicKey}. $message.")
            }
        }
    }

    fun joinSharedWalletClicked(daoId: String, context: Context) {
        Log.d("MVDAO", "joinSharedWalletClicked 1")

        val block = getDao(daoId)?.first
        val dao = getDao(daoId)?.second

        Log.d("MVDAO", "joinSharedWalletClicked 3")

        if (block == null) {
            SnackbarHandler.displaySnackbar("DAO not found")
            return
        }

        // Add a proposal to trust chain to join a shared wallet
        val proposeBlockData = try {
            getDaoCommunity().proposeJoinWallet(block).getData()
        } catch (t: Throwable) {
            Log.d("MVDAO", "Join wallet proposal failed. ${t.message ?: "No further information"}.")
            SnackbarHandler.displaySnackbar("Unexpected error occurred. Try again")
            return
        }
        Log.d("MVDAO", "joinSharedWalletClicked 4")

        // Wait and collect signatures
        SnackbarHandler.displaySnackbar("Please wait for all signatures to be collected.")

        var signatures: List<SWResponseSignatureBlockTD>? = null
        while (signatures == null) {
            Thread.sleep(1000)
            val old_signature_count = signatures?.size ?: 0
            signatures = collectJoinWalletResponses(proposeBlockData)
            if (signatures != null && signatures.size != old_signature_count) {
                SnackbarHandler.displaySnackbar("Received a new signature: ${signatures.size}/${dao?.threshHold}")
            }
        }

        // Create a new shared wallet using the signatures of the others.
        // Broadcast the new shared bitcoin wallet on trust chain.
        try {
            getDaoCommunity().joinBitcoinWallet(
                block.transaction,
                proposeBlockData,
                signatures,
                context
            )
            // Add new nonceKey after joining a DAO
            WalletManagerAndroid.getInstance()
                .addNewNonceKey(proposeBlockData.SW_UNIQUE_ID, context)
        } catch (t: Throwable) {
            Log.d("MVDAO", "Joining failed. ${t.message ?: "No further information"}.")
            SnackbarHandler.displaySnackbar("Unexpected error occurred. Try again")
        }
        Log.d("MVDAO", "joinSharedWalletClicked 5")

        SnackbarHandler.displaySnackbar("You joined ${proposeBlockData.SW_UNIQUE_ID}!")
    }

    private fun collectJoinWalletResponses(
        blockData: SWSignatureAskBlockTD
    ): List<SWResponseSignatureBlockTD>? {
        val responses =
            getDaoCommunity().fetchProposalResponses(
                blockData.SW_UNIQUE_ID,
                blockData.SW_UNIQUE_PROPOSAL_ID
            )
        Log.d(
            "MVDAO",
            "Waiting for signatures. ${responses.size}/${blockData.SW_SIGNATURES_REQUIRED} received!"
        )

//        setAlertText(
//            "Collecting signatures: ${responses.size}/${blockData.SW_SIGNATURES_REQUIRED} received!"
//        )

        if (responses.size >= blockData.SW_SIGNATURES_REQUIRED) {
            return responses
        }
        return null
    }

    fun getDao(daoId: String): Pair<TrustChainBlock, DAO>? {
        return daos.value.entries.find { it.value.daoId == daoId }?.toPair()
    }

    fun getProposal(proposalId: String): Proposal? {
        for (dao in daos.value.values) {
            for (proposal in dao.proposals) {
                if (proposal.proposalId == proposalId) {
                    return proposal
                }
            }
        }
        return null
    }

    private fun getBalance(blockHash: ByteArray): Coin {
        val swJoinBlock: TrustChainBlock =
            getDaoCommunity().fetchLatestSharedWalletBlock(blockHash!!)
                ?: throw IllegalStateException("Shared Wallet not found given the hash: ${blockHash!!}")
        val walletData = SWJoinBlockTransactionData(swJoinBlock.transaction).getData()

        val previousTransaction =
            CTransaction().deserialize(walletData.SW_TRANSACTION_SERIALIZED.hexToBytes())
        return Coin.valueOf(previousTransaction.vout.filter { it.scriptPubKey.size == 35 }[0].nValue)
    }

    fun transferFundsClicked(
        bitcoinPublicKey: String,
        satoshiTransferAmount: Long,
        blockHash: ByteArray,
        context: Context,
        activityRequired: Activity
    ) {
        viewModelScope.launch {
            transferFundsClickedLong(
                bitcoinPublicKey,
                satoshiTransferAmount,
                blockHash,
                context,
                activityRequired
            )
        }
    }

    suspend fun transferFundsClickedLong(
        bitcoinPublicKey: String,
        satoshiTransferAmount: Long,
        blockHash: ByteArray,
        context: Context,
        activityRequired: Activity
    ) {
//        if (!validateTransferInput()) {
//            activity?.runOnUiThread {
//                alert_view.text =
//                    "Failed: Bitcoin PK should be a string, minimal satoshi amount: ${SWUtil.MINIMAL_TRANSACTION_AMOUNT}"
//            }
//            return
//        }
//        val bitcoinPublicKey = input_bitcoin_public_key.text.toString()
//        val satoshiTransferAmount = input_satoshi_amount.text.toString().toLong()

        val swJoinBlock: TrustChainBlock =
            getDaoCommunity().fetchLatestSharedWalletBlock(blockHash!!)
                ?: throw IllegalStateException("Shared Wallet not found given the hash: ${blockHash!!}")
        val walletData = SWJoinBlockTransactionData(swJoinBlock.transaction).getData()

        if (getBalance(blockHash).minus(Coin.valueOf(satoshiTransferAmount)).isNegative) {
            Log.d(
                "MVDAO",
                "Failed: Transfer amount should be smaller than the current balance"
            )
            return
        }

        val transferFundsData = try {
            getDaoCommunity().proposeTransferFunds(
                swJoinBlock,
                bitcoinPublicKey,
                satoshiTransferAmount
            )
        } catch (t: Throwable) {
            Log.d(
                "MVDAO",
                "Proposing transfer funds failed. ${t.message ?: "No further information"}."
            )
            Log.d(
                "MVDAO",
                t.message ?: "Unexpected error occurred. Try again"
            )
            return
        }
//        val context = requireContext()
//        val activityRequired = requireActivity()

        val responses = collectResponses(transferFundsData)
        try {
            getDaoCommunity().transferFunds(
                walletData,
                swJoinBlock.transaction,
                transferFundsData.getData(),
                responses,
                bitcoinPublicKey,
                satoshiTransferAmount,
                context,
                activityRequired
            )

            Log.d(
                "MVDAO",
                "Funds transfered!"
            )
        } catch (t: Throwable) {
            Log.d("MVDAO", "Transferring funds failed. ${t.message ?: "No further information"}.")
//            resetWalletInitializationValues()
            Log.d(
                "MVDAO",
                t.message ?: "Unexpected error occurred. Try again"
            )
        }
    }

    private suspend fun collectResponses(data: SWTransferFundsAskTransactionData): List<SWResponseSignatureBlockTD> {
        var responses: List<SWResponseSignatureBlockTD>? = null
        Log.d(
            "MVDAO",
            "Loading... This might take some time."
        )

        while (responses == null) {
            responses =
                checkSufficientResponses(data, data.getData().SW_SIGNATURES_REQUIRED)
            if (responses == null) {
                delay(1000L)
            }
        }

        return responses
    }

    private fun checkSufficientResponses(
        data: SWTransferFundsAskTransactionData,
        requiredSignatures: Int
    ): List<SWResponseSignatureBlockTD>? {
        val blockData = data.getData()
        val responses =
            getDaoCommunity().fetchProposalResponses(
                blockData.SW_UNIQUE_ID,
                blockData.SW_UNIQUE_PROPOSAL_ID
            )
        Log.d(
            "MVDAO",
            "Responses for ${blockData.SW_UNIQUE_ID}.${blockData.SW_UNIQUE_PROPOSAL_ID}: ${responses.size}/$requiredSignatures"
        )

        Log.d(
            "MVDAO",
            "Collecting signatures: ${responses.size}/${blockData.SW_SIGNATURES_REQUIRED} received!"
        )

        if (responses.size >= requiredSignatures) {
            return responses
        }
        return null
    }

    private fun getDaoCommunity(): DaoCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("DaoCommunity is not configured")
    }

    protected fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    protected val trustchain: TrustChainHelper by lazy {
        TrustChainHelper(getTrustChainCommunity())
    }

    fun userInDao(dao: DAO): Boolean {
        val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        return dao.members.find { it.trustchainPublicKey == publicKey } != null
    }

    private fun isAddressValid(address: String): Boolean {
        return address.length in 26..35
    }

    private fun isPrivateKeyValid(privateKey: String): Boolean {
        return privateKey.length in 51..52 || privateKey.length == 64
    }
}

val dao_2 = DAO(
    daoId = "2032102",
    name = "ENS",
    about = "This is an about section.",
    proposals = listOf(
        Proposal(
            proposalCreator = "0x128937219838921378921",
            daoId = "dsda",
            proposalId = "1",
            proposalTime = "asd",
            proposalTitle = "Proposal Title22",
            proposalText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus cursus enim nec hendrerit iaculis. Pellentesque scelerisque dapibus nibh, ut porttitor erat efficitur ut. Nulla lacinia mollis dui, quis tincidunt massa condimentum quis. Donec congue laoreet bibendum. Fusce nec neque nibh.",
            signatures = listOf(
                ProposalSignature(
                    proposalId = "1",
                    bitcoinPublicKey = "asdasd",
                    trustchainPublicKey = "asdasd"
                ),
                ProposalSignature(
                    proposalId = "1",
                    bitcoinPublicKey = "asdasd",
                    trustchainPublicKey = "asdasd"
                ),
                ProposalSignature(
                    proposalId = "1",
                    bitcoinPublicKey = "asdasd",
                    trustchainPublicKey = "asdasd"
                )
            ),
            signaturesRequired = 10,
            transferAddress = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
            transferAmountBitcoinSatoshi = 213
        ),
        Proposal(
            proposalCreator = "0x128937219838921378921",
            daoId = "dsda",
            proposalId = "321",
            proposalTime = "asd",
            proposalTitle = "Proposal Title333",
            proposalText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus cursus enim nec hendrerit iaculis. Pellentesque scelerisque dapibus nibh, ut porttitor erat efficitur ut. Nulla lacinia mollis dui, quis tincidunt massa condimentum quis. Donec congue laoreet bibendum. Fusce nec neque nibh.",
            signatures = listOf(
                ProposalSignature(
                    proposalId = "1",
                    bitcoinPublicKey = "asdasd",
                    trustchainPublicKey = "asdasd"
                ),
                ProposalSignature(
                    proposalId = "1",
                    bitcoinPublicKey = "asdasd",
                    trustchainPublicKey = "asdasd"
                ),
                ProposalSignature(
                    proposalId = "1",
                    bitcoinPublicKey = "asdasd",
                    trustchainPublicKey = "asdasd"
                )
            ),
            signaturesRequired = 10,
            transferAddress = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
            transferAmountBitcoinSatoshi = 213
        )

    ),
    members = listOf(),
    threshHold = 10,
    entranceFee = 10,
    previousTransaction = "",
    balance = 0

)
