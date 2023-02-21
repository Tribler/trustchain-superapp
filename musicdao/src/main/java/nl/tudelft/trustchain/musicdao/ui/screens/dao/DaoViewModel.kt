package nl.tudelft.trustchain.musicdao.ui.screens.dao

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import nl.tudelft.trustchain.musicdao.core.repositories.ArtistRepository
import nl.tudelft.trustchain.musicdao.core.repositories.model.Artist
import nl.tudelft.trustchain.musicdao.ui.SnackbarHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.TrustChainHelper
import nl.tudelft.trustchain.currencyii.coin.WalletManager
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.*
import nl.tudelft.trustchain.currencyii.util.DAOTransferFundsHelper
import nl.tudelft.trustchain.currencyii.util.taproot.CTransaction
import nl.tudelft.trustchain.musicdao.core.dao.*
import org.bitcoinj.core.Coin
import prettyPrint
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DaoViewModel @Inject constructor(val artistRepository: ArtistRepository) : ViewModel() {

    var walletManager: WalletManager? = null
    var daos: MutableStateFlow<Map<TrustChainBlock, DAO>> = MutableStateFlow(mapOf())

    var isRefreshing = MutableStateFlow(false)
    var daoPeers = MutableStateFlow(0)

    fun initManager() {
        refreshOneShot()
    }

    fun createGenesisDAO(currentEntranceFee: Long, currentThreshold: Int, context: Context) {
        try {
            val newDAO = getDaoCommunity().createBitcoinGenesisWallet(
                currentEntranceFee,
                currentThreshold,
                context
            )
            walletManager?.addNewNonceKey(newDAO.getData().SW_UNIQUE_ID, context)
            SnackbarHandler.displaySnackbar("DAO ${newDAO.getData().SW_UNIQUE_ID} successfully created and broadcast.")
        } catch (e: Exception) {
            SnackbarHandler.displaySnackbar("Unexpected error occurred. Try again")
            e.printStackTrace()
        }
    }

    fun refreshOneShot() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                isRefreshing.value = true
                refresh()
                isRefreshing.value = false
            }
        }
    }

    /**
     * Refresh the state of the DAOs/proposals
     */
    @SuppressLint("NewApi")
    private suspend fun refresh() {
        Log.d("MVDAO", "Updating DAOs from network. ")

        daoPeers.value = getDaoCommunity().getPeers().size

        // Crawl for new wallets on the network.
        crawlAvailableSharedWallets()
        crawlProposalsAndUpdateIfNewFound()

        val proposals = getDaoCommunity().fetchProposalBlocks()
        val wallets = getDaoCommunity().discoverSharedWallets()

        Log.d("MVDAO", "Found ${wallets.size} DAOs on the network.")
        Log.d("MVDAO", "Found ${proposals.size} proposals on the network.")

        daos.value = wallets.associateWith { daoBlock ->
            val blockData = SWJoinBlockTransactionData(daoBlock.transaction).getData()
            aggregateDaosAndProposals(
                daoBlock,
                proposals.filter { proposalBlock ->
                    daoIdFromProposal(proposalBlock) == blockData.SW_UNIQUE_ID
                }
            )
        }

        // Only show DAOs after 14th of october.
        daos.value = daos.value.filter {
            it.key.timestamp >= GregorianCalendar(
                2022,
                Calendar.OCTOBER,
                14
            ).time
        }

        Log.d("MVDAO", "Currently ${daos.value.size} DAOs in the network.")
        daos.value.forEach {
            Log.d("MVDAO", it.prettyPrint())
        }
    }

    private fun aggregateDaosAndProposals(
        trustChainWalletBlock: TrustChainBlock,
        proposalBlocks: List<TrustChainBlock>
    ): DAO {
        val blockData = SWJoinBlockTransactionData(trustChainWalletBlock.transaction).getData()

        val proposals = proposalBlocks.mapNotNull { block ->
            when (block.type) {
                DaoCommunity.SIGNATURE_ASK_BLOCK -> {
                    val data = SWSignatureAskTransactionData(block.transaction).getData()
                    val signatures = getDaoCommunity().fetchProposalResponsesWithBlocks(
                        data.SW_UNIQUE_ID,
                        data.SW_UNIQUE_PROPOSAL_ID
                    )
                    return@mapNotNull JoinProposal(
                        proposalId = data.SW_UNIQUE_PROPOSAL_ID,
                        daoId = data.SW_UNIQUE_ID,
                        signaturesRequired = data.SW_SIGNATURES_REQUIRED,
                        proposalCreator = block.publicKey.toHex(),
                        proposalTime = block.timestamp.toString(),
                        proposalText = "",
                        proposalTitle = "",
                        signatures = signatures.map {
                            ProposalSignature(
                                bitcoinPublicKey = it.key.SW_BITCOIN_PK,
                                trustchainPublicKey = it.value.publicKey.toHex(),
                                proposalId = it.key.SW_UNIQUE_PROPOSAL_ID
                            )
                        },
                        transferAmountBitcoinSatoshi = blockData.SW_ENTRANCE_FEE.toInt()
                    ) to block
                }
                DaoCommunity.TRANSFER_FUNDS_ASK_BLOCK -> {
                    val data = SWTransferFundsAskTransactionData(block.transaction).getData()
                    val signatures = getDaoCommunity().fetchProposalResponsesWithBlocks(
                        data.SW_UNIQUE_ID,
                        data.SW_UNIQUE_PROPOSAL_ID
                    )
                    return@mapNotNull TransferProposal(
                        proposalId = data.SW_UNIQUE_PROPOSAL_ID,
                        daoId = data.SW_UNIQUE_ID,
                        signaturesRequired = data.SW_SIGNATURES_REQUIRED,
                        proposalCreator = block.publicKey.toHex(),
                        proposalTime = block.timestamp.toString(),
                        proposalText = data.SW_UNIQUE_ID,
                        proposalTitle = "",
                        signatures = signatures.map {
                            ProposalSignature(
                                bitcoinPublicKey = it.key.SW_BITCOIN_PK,
                                trustchainPublicKey = it.value.publicKey.toHex(),
                                proposalId = it.key.SW_UNIQUE_PROPOSAL_ID
                            )
                        },
                        transferAmountBitcoinSatoshi = data.SW_TRANSFER_FUNDS_AMOUNT.toInt(),
                        transferAddress = data.SW_TRANSFER_FUNDS_TARGET_SERIALIZED
                    ) to block
                }
                else -> {
                    return@mapNotNull null
                }
            }
        }.toMap()

        return DAO(
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
        Log.d("MVDAO", "Found ${allUsers.size} peers, crawling")

        for (peer in allUsers) {
            try {
                // TODO: Commented this line out, it causes the app to crash
//                withTimeout(SW_CRAWLING_TIMEOUT_MILLI) {
                trustchain.crawlChain(peer)
                trustchain
                    .getChainByUser(peer.publicKey.keyToBin())
            } catch (t: Throwable) {
                val message = t.message ?: "No further information"
                Log.d("MVDAO", "Crawling failed for: ${peer.publicKey}. $message.")
            }
        }
    }

    fun joinSharedWalletClicked(daoId: String, context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val block = getDao(daoId)?.first
                val dao = getDao(daoId)?.second

                if (block == null || dao == null) {
                    SnackbarHandler.displaySnackbar("DAO not found")
                    return@withContext
                }

                // Add a proposal to trust chain to join a shared wallet
                val proposeBlockData = try {
                    getDaoCommunity().proposeJoinWallet(block).getData()
                } catch (t: Throwable) {
                    Log.d(
                        "MVDAO",
                        "Join wallet proposal failed. ${t.message ?: "No further information"}."
                    )
                    SnackbarHandler.displaySnackbar("Join wallet proposal failed. ${t.message ?: "No further information"}.")
                    return@withContext
                }

                // Wait and collect signatures
                SnackbarHandler.displaySnackbar("Please wait for all signatures to be collected.")

                var signatures: List<SWResponseSignatureBlockTD>? = null
                while (signatures == null) {
                    Thread.sleep(1000)
                    val oldSignatureCount = signatures?.size ?: 0
                    signatures = collectJoinWalletResponses(proposeBlockData)
                    if (signatures != null && signatures.size != oldSignatureCount) {
                        SnackbarHandler.displaySnackbar(
                            "Received a new signature: ${signatures.size}/${
                            requiredSignatures(
                                dao
                            )
                            }"
                        )
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
                refreshOneShot()
            }
        }
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

        if (responses.size >= blockData.SW_SIGNATURES_REQUIRED) {
            return responses
        }
        return null
    }

    private fun requiredSignatures(dao: DAO): Int {
        return SWUtil.percentageToIntThreshold(dao.members.size, dao.threshHold)
    }

    fun hasMadeProposalVote(proposal: Proposal): Boolean {
        val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        val publicKey2 = getDaoCommunity().myPeer.publicKey.keyToBin().toHex()
        val find = proposal.signatures.find { it.trustchainPublicKey == publicKey }
        Log.d("MUSICAO3", "${proposal.signatures}")
        Log.d("MUSICAO3", publicKey)
        Log.d("MUSICAO3", publicKey2)
        return find != null
    }

    fun transferFundsClickedByMe(
        publicKey: String,
        satoshiTransferAmount: Long,
        blockHash: ByteArray,
        context: Context,
        activityRequired: Activity
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                transferFundsClickedLong(
                    publicKey,
                    satoshiTransferAmount,
                    blockHash,
                    context,
                    activityRequired
                )
            }
        }
    }

    @SuppressLint("NewApi")
    private suspend fun transferFundsClickedLong(
        bitcoinPublicKey: String,
        satoshiTransferAmount: Long,
        blockHash: ByteArray,
        context: Context,
        activityRequired: Activity
    ) {
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

        Log.d(
            "MVDAO",
            "Calling transferFundsData."
        )

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
            SnackbarHandler.displaySnackbar("Proposing transfer funds failed. ${t.message ?: "No further information"}.")
            return
        }

        SnackbarHandler.displaySnackbar("Proposal has been made. Waiting for signatures...")

        val responses = collectResponses(transferFundsData)
        Log.d(
            "MVDAO",
            "Collected all signatures, continuing..."
        )
        SnackbarHandler.displaySnackbar("Collected all signatures, continuing...")
        try {
            responses.forEach {
                Log.d("ATM", it.SW_BITCOIN_PK)
                Log.d("ATM", it.SW_NONCE)
                Log.d("ATM", it.SW_UNIQUE_PROPOSAL_ID)
            }
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
                "Funds transferred!"
            )
            SnackbarHandler.displaySnackbar("Funds have been transferred!")
            refreshOneShot()
        } catch (t: Throwable) {
            Log.d("MVDAO", "Transferring funds failed. ${t.message ?: "No further information"}.")
            Log.d(
                "MVDAO",
                t.message ?: "Unexpected error occurred. Try again"
            )
            SnackbarHandler.displaySnackbar("Transferring funds failed. ${t.message ?: "No further information"}.")
        }
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

    private suspend fun collectResponses(data: SWTransferFundsAskTransactionData): List<SWResponseSignatureBlockTD> {
        var responses: List<SWResponseSignatureBlockTD>? = null
        Log.d(
            "MVDAO",
            "Loading... This might take some time."
        )

        // Wait and collect signatures
        SnackbarHandler.displaySnackbar("Please wait for all signatures to be collected.")

        while (responses == null) {
            responses =
                checkSufficientResponses(data, data.getData().SW_SIGNATURES_REQUIRED)
            if (responses == null) {
                delay(1000L)
            }
        }

        return responses
    }

    fun upvoteTransfer(context: Context, proposal: Proposal) {
        Log.d(
            "MVDAO",
            "Attempting to upvote transfer on DAO ${proposal.daoId} for proposal ${proposal.proposalId}."
        )

        val proposalId = proposal.proposalId
        val proposalReceived = getProposal(proposalId)!!
        val proposalBlock = proposalReceived.second!!

        val daoReceived = getDao(proposal.daoId)!!
        val daoBlock = daoReceived.first

        val transferBlock = SWTransferDoneTransactionData(daoBlock.transaction).getData()
        val oldTransaction = transferBlock.SW_TRANSACTION_SERIALIZED

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin()

                DAOTransferFundsHelper.transferFundsBlockReceived(
                    oldTransaction,
                    proposalBlock,
                    transferBlock,
                    myPublicKey,
                    true,
                    context
                )

                Log.d(
                    "MVDAO",
                    "Upvoted transfer on DAO ${proposal.daoId} for proposal ${proposal.proposalId}."
                )
            }
            refreshOneShot()
        }
    }

    fun upvoteJoin(context: Context, proposal: Proposal) {
        Log.d(
            "MVDAO",
            "Attempting to upvote transfer on DAO ${proposal.daoId} for proposal ${proposal.proposalId}."
        )

        val id = proposal.proposalId
        val proposalReceived = getProposal(id)!!

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin()
                val votedInFavor = true

                getDaoCommunity().joinAskBlockReceived(
                    proposalReceived.second!!,
                    myPublicKey,
                    votedInFavor,
                    context
                )

                SnackbarHandler.displaySnackbar("Voting on proposal ${proposalReceived.first.proposalId}")
                Log.d(
                    "MVDAO",
                    "Upvoted transfer on DAO ${proposal.daoId} for proposal ${proposal.proposalId}."
                )

                refreshOneShot()
            }
        }
    }

    fun getDao(daoId: String): Pair<TrustChainBlock, DAO>? {
        return daos.value.entries.find { it.value.daoId == daoId }?.toPair()
    }

    fun getProposal(proposalId: String): Pair<Proposal, TrustChainBlock?>? {
        for (dao in daos.value.values) {
            for (proposal in dao.proposals) {
                if (proposal.key.proposalId == proposalId) {
                    return proposal.toPair()
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun getListsOfArtists(): List<Artist> {
        return artistRepository.getArtists()
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
}
