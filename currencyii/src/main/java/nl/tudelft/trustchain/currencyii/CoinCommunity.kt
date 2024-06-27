package nl.tudelft.trustchain.currencyii

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.payload.AlivePayload
import nl.tudelft.trustchain.currencyii.payload.ElectedPayload
import nl.tudelft.trustchain.currencyii.payload.ElectionPayload
import nl.tudelft.trustchain.currencyii.payload.SignPayload
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTD
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWResponseNegativeSignatureBlockTD
import nl.tudelft.trustchain.currencyii.sharedWallet.SWResponseNegativeSignatureTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWResponseSignatureBlockTD
import nl.tudelft.trustchain.currencyii.sharedWallet.SWResponseSignatureTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskBlockTD
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferDoneTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskBlockTD
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.util.DAOCreateHelper
import nl.tudelft.trustchain.currencyii.util.DAOJoinHelper
import nl.tudelft.trustchain.currencyii.util.DAOTransferFundsHelper
import kotlin.coroutines.CoroutineContext

@Suppress("UNCHECKED_CAST")
open class CoinCommunity constructor(
    private val context: Context,
    serviceId: String = "02313685c1912a141279f8248fc8db5899c5df5b",
) : Community() {
    class Factory(
        private val context: Context,
    ) : Overlay.Factory<CoinCommunity>(CoinCommunity::class.java) {
        override fun create(): CoinCommunity {
            return CoinCommunity(context)
        }
    }

    override val serviceId = serviceId
    private var currentLeader: HashMap<String, Peer?> = HashMap()
    private var candidates: HashMap<String, ArrayList<Peer>> = HashMap()

    init {
        messageHandlers[MessageId.ELECTION_REQUEST] = ::onElectionRequestPacket
        messageHandlers[MessageId.ELECTED_RESPONSE] = ::onElectedResponsePacket
        messageHandlers[MessageId.ALIVE_RESPONSE] = ::onAliveResponsePacket
        messageHandlers[MessageId.JOIN_DAO_DATA] = ::onDaoJoinDataPacket
    }

    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    private val daoCreateHelper = DAOCreateHelper()
    private val daoJoinHelper = DAOJoinHelper()
    private val daoTransferFundsHelper = DAOTransferFundsHelper()
//    private val leaderElectionHelper = LeaderElectionHelper()

    /**
     * Create a bitcoin genesis wallet and broadcast the result on trust chain.
     * The bitcoin transaction may take some time to finish.
     * @throws - exception if something goes wrong with creating or broadcasting bitcoin transaction.
     * @param entranceFee - Long, the entrance fee for joining the DAO.
     * @param threshold - Int, the percentage of members that need to vote before allowing someone in the DAO.
     */
    fun createBitcoinGenesisWallet(
        entranceFee: Long,
        threshold: Int,
        context: Context
    ): SWJoinBlockTransactionData {
        return daoCreateHelper.createBitcoinGenesisWallet(
            myPeer,
            entranceFee,
            threshold,
            context
        )
    }

    /**
     * 2.1 Send a proposal on the trust chain to join a shared wallet and to collect signatures.
     * The proposal is a serialized bitcoin join transaction.
     * **NOTE** the latest walletBlockData should be given, otherwise the serialized transaction is invalid.
     * @param walletBlock - the latest (that you know of) shared wallet block.
     */
    fun proposeJoinWallet(walletBlock: TrustChainBlock): SWSignatureAskTransactionData {
        return daoJoinHelper.proposeJoinWallet(myPeer, walletBlock)
    }

    /**
     * 2.2 Commit the join wallet transaction on the bitcoin blockchain and broadcast the result on trust chain.
     *
     * Note:
     * There should be enough sufficient signatures, based on the multisig wallet data.
     * @throws - exceptions if something goes wrong with creating or broadcasting bitcoin transaction.
     * @param walletBlockData - TrustChainTransaction, describes the wallet that is joined
     * @param blockData - SWSignatureAskBlockTD, the block where the other users are voting on
     * @param responses - the positive responses for your request to join the wallet
     */
    fun joinBitcoinWallet(
        walletBlockData: TrustChainTransaction,
        blockData: SWSignatureAskBlockTD,
        responses: List<SWResponseSignatureBlockTD>,
        context: Context
    ) {
        daoJoinHelper.joinBitcoinWallet(
            myPeer,
            walletBlockData,
            blockData,
            responses,
            context
        )
    }

    /**
     * 3.1 Send a proposal block on trustchain to ask for the signatures.
     * Assumed that people agreed to the transfer.
     * @param walletBlock - TrustChainBlock, describes the wallet where the transfer is from
     * @param receiverAddressSerialized - String, the address where the transaction needs to go
     * @param satoshiAmount - Long, the amount that needs to be transferred
     * @return the proposal block
     */
    fun proposeTransferFunds(
        walletBlock: TrustChainBlock,
        receiverAddressSerialized: String,
        satoshiAmount: Long
    ): SWTransferFundsAskTransactionData {
        return daoTransferFundsHelper.proposeTransferFunds(
            myPeer,
            walletBlock,
            receiverAddressSerialized,
            satoshiAmount
        )
    }

    /**
     * 3.2 Transfer funds from an existing shared wallet to a third-party. Broadcast bitcoin transaction.
     * @param walletData - SWJoinBlockTD, the data about the wallet when joining the wallet
     * @param walletBlockData - TrustChainTransaction, describes the wallet where the transfer is from
     * @param blockData - SWTransferFundsAskBlockTD, the block where the other users are voting on
     * @param responses - List<SWResponseSignatureBlockTD>, the list with positive responses on the voting
     * @param receiverAddress - String, the address where the transfer needs to go
     * @param satoshiAmount - Long, the amount that needs to be transferred
     */
    fun transferFunds(
        walletData: SWJoinBlockTD,
        walletBlockData: TrustChainTransaction,
        blockData: SWTransferFundsAskBlockTD,
        responses: List<SWResponseSignatureBlockTD>,
        receiverAddress: String,
        satoshiAmount: Long,
        context: Context,
        activity: Activity
    ) {
        daoTransferFundsHelper.transferFunds(
            myPeer,
            walletData,
            walletBlockData,
            blockData,
            responses,
            receiverAddress,
            satoshiAmount,
            context,
            activity
        )
    }

    /**
     * Discover shared wallets that you can join, return the latest blocks that the user knows of.
     */
    fun discoverSharedWallets(): List<TrustChainBlock> {
        val swBlocks = getTrustChainCommunity().database.getBlocksWithType(JOIN_BLOCK)
        return swBlocks
            .distinctBy { SWJoinBlockTransactionData(it.transaction).getData().SW_UNIQUE_ID }
            .map { fetchLatestSharedWalletBlock(it, swBlocks) ?: it }
    }

    /**
     * Discover shared wallets that you can join, return the latest (known) blocks
     * Fetch the latest block associated with a shared wallet.
     * swBlockHash - the hash of one of the blocks associated with a shared wallet.
     */
    fun fetchLatestSharedWalletBlock(swBlockHash: ByteArray): TrustChainBlock? {
        val swBlock =
            getTrustChainCommunity().database.getBlockWithHash(swBlockHash)
                ?: return null
        val swBlocks = getTrustChainCommunity().database.getBlocksWithType(JOIN_BLOCK)
        return fetchLatestSharedWalletBlock(swBlock, swBlocks)
    }

    /**
     * Fetch the latest shared wallet block, based on a given block 'block'.
     * The unique shared wallet id is used to find the most recent block in
     * the 'sharedWalletBlocks' list.
     */
    private fun fetchLatestSharedWalletBlock(
        block: TrustChainBlock,
        fromBlocks: List<TrustChainBlock>
    ): TrustChainBlock? {
        if (block.type != JOIN_BLOCK) {
            return null
        }
        val walletId = SWJoinBlockTransactionData(block.transaction).getData().SW_UNIQUE_ID

        return fromBlocks
            .filter { it.type == JOIN_BLOCK } // make sure the blocks have the correct type!
            .filter { SWJoinBlockTransactionData(it.transaction).getData().SW_UNIQUE_ID == walletId }
            .maxByOrNull { it.timestamp.time }
    }

    /**
     * Fetch the shared wallet blocks that you are part of, based on your trustchain PK.
     */
    fun fetchLatestJoinedSharedWalletBlocks(): List<TrustChainBlock> {
        return discoverSharedWallets().filter {
            val blockData = SWJoinBlockTransactionData(it.transaction).getData()
            val userTrustchainPks = blockData.SW_TRUSTCHAIN_PKS
            userTrustchainPks.contains(myPeer.publicKey.keyToBin().toHex())
        }
    }

    /**
     * Get the public key of the one that is receiving the request
     * @return string
     */
    private fun fetchSignatureRequestReceiver(block: TrustChainBlock): String {
        if (block.type == SIGNATURE_ASK_BLOCK) {
            return SWSignatureAskTransactionData(block.transaction).getData().SW_RECEIVER_PK
        }

        if (block.type == TRANSFER_FUNDS_ASK_BLOCK) {
            return SWTransferFundsAskTransactionData(block.transaction).getData().SW_RECEIVER_PK
        }

        return "invalid-pk"
    }

    fun sendPayload(
        peer: Peer,
        payload: ByteArray
    ) {
        Log.i("CoinCommunitySending", "Sending payload to ${peer.address}")
        makeToast("Sending payload to ${peer.address}")
        send(
            peer,
            payload
        )
    }

    internal fun createElectionRequest(dAOid: ByteArray): ByteArray {
        val payload = ElectionPayload(dAOid)
        return serializePacket(MessageId.ELECTION_REQUEST, payload)
    }

    internal fun createElectedResponse(dAOid: ByteArray): ByteArray {
        val payload = ElectedPayload(dAOid)
        return serializePacket(MessageId.ELECTED_RESPONSE, payload)
    }

    internal fun createAliveResponse(dAOid: ByteArray): ByteArray {
        val payload = AlivePayload(dAOid)
        return serializePacket(MessageId.ALIVE_RESPONSE, payload)
    }

    internal fun createSignPayloadResponse(
        dAOid: ByteArray,
        recentSWBlock: TrustChainBlock,
        proposeBlockData: SWSignatureAskBlockTD,
        signatures: List<SWResponseSignatureBlockTD>
    ): ByteArray {
        val payload = SignPayload(dAOid, recentSWBlock, proposeBlockData, signatures)
        return serializePacket(MessageId.JOIN_DAO_DATA, payload)
    }

    fun onAliveResponsePacket(packet: Packet) {
        val (peer, payload) =
            packet.getAuthPayload(
                AlivePayload.Deserializer
            )
        this.onAliveResponse(peer, payload)
    }

    private fun makeToast(text: String, duration: Int = Toast.LENGTH_SHORT, dispatcher: CoroutineContext = Dispatchers.Main) {
        CoroutineScope(dispatcher).launch {
            Toast.makeText(context, text, duration).show()
        }
    }

    private fun onDaoJoinDataPacket(packet: Packet) {
        Log.d("LEADER", "Received data from Peer wanting to join")
        makeToast("Received data from Peer wanting to join")


        val (peer, payload) =
            packet.getAuthPayload(
                SignPayload.Deserializer
            )
        try {
            Log.d("LEADER", "Peer wanting to join: ${peer.publicKey}")
            makeToast("Peer wanting to join: ${peer.publicKey}")

            joinBitcoinWallet(
                payload.mostRecentSWBlock.transaction,
                payload.proposeBlockData,
                payload.signatures,
                this.context
            )
            // Add new nonceKey after joining a DAO
            WalletManagerAndroid.getInstance()
                .addNewNonceKey(payload.proposeBlockData.SW_UNIQUE_ID, this.context)

            makeToast("Peer: ${peer.publicKey}, joined successfully")
        } catch (t: Throwable) {
            Log.e("LEADER", "Joining failed. ${t.message ?: "No further information"}.")
            makeToast("Peer: ${peer.publicKey}, failed to join")
        }
    }

    fun onSignPayloadResponse(
        peer: Peer,
        payload: SignPayload
    ) {
        // TODO: Implement adding to the wallet without a Context
        try {
            joinBitcoinWallet(
                payload.mostRecentSWBlock.transaction,
                payload.proposeBlockData,
                payload.signatures,
                this.context
            )
            // Add new nonceKey after joining a DAO
            WalletManagerAndroid.getInstance()
                .addNewNonceKey(payload.proposeBlockData.SW_UNIQUE_ID, this.context)
        } catch (t: Throwable) {
            Log.e("Coin", "Joining failed. ${t.message ?: "No further information"}.")
        }
    }

    fun onAliveResponse(
        peer: Peer,
        payload: AlivePayload
    ) {
        this.getCandidates()[payload.DAOid.decodeToString()]?.add(peer)
    }

    fun onElectedResponsePacket(packet: Packet) {
        val (peer, payload) =
            packet.getAuthPayload(
                ElectedPayload.Deserializer
            )
        this.onElectedResponse(peer, payload)
    }

    fun onElectedResponse(
        peer: Peer,
        payload: ElectedPayload
    ) {
        Log.d("LEADER", "Elected: " + peer.publicKey)
        makeToast("Elected: ${peer.publicKey} as leader")

        getCurrentLeader()[payload.DAOid.decodeToString()] = peer
    }

    fun onElectionRequestPacket(packet: Packet) {
        val (peer, payload) =
            packet.getAuthPayload(
                ElectionPayload.Deserializer
            )
        Log.d("Leader", "Election packet received.")
        makeToast("Election packet received.")

        getCandidates()[payload.DAOid.decodeToString()] = ArrayList()
        onElectionRequest(peer, payload)
    }

    fun getPeersPKInDao(DAOid: ByteArray): ArrayList<String> {
        val mostRecentWalletBlock =
            fetchLatestSharedWalletBlock(DAOid)
                ?: throw IllegalStateException("Most recent DAO block not found")
        val peerPK: ArrayList<String> = ArrayList<String>()
        val blockData = SWJoinBlockTransactionData(mostRecentWalletBlock.transaction).getData()
        for (swParticipantPk in blockData.SW_TRUSTCHAIN_PKS) {
            peerPK.add(swParticipantPk)
        }
        return peerPK
    }

    fun onElectionRequest(
        peer: Peer,
        payload: ElectionPayload
    ) {
        val peerPK = getPeersPKInDao(payload.DAOid)

        Log.d("Leader", "Election started.")
        val aliveResponse = this.createAliveResponse(payload.DAOid)
        this.sendPayload(peer, aliveResponse)

        makeToast("Leader election started")

        getCurrentLeader()[payload.DAOid.decodeToString()] = null

        val higherPeers = ArrayList<Peer>()

        for (p in this.getPeers()) {
            if (peerPK.contains(p.publicKey.keyToBin().toHex()) &&
                p.address.hashCode() > this.myPeer.address.hashCode()) {
                higherPeers.add(p)
            }
        }
        Log.d("Leader", "peers with higher ips:$higherPeers")

        if (higherPeers.isEmpty()) {
            Log.d("Leader", "Elected: " + this.myPeer.publicKey)
            makeToast("Elected ${this.myPeer.publicKey} as leader")

            val electedPayload = this.createElectedResponse(payload.DAOid)
            this.sendPayload(peer, electedPayload)
            getCurrentLeader()[payload.DAOid.decodeToString()] = this.myPeer
            return
        }
        var lastTime = System.currentTimeMillis()
        var i = 0
        for (p in higherPeers) {
            // Send election request to the peer with the highest hash
            val generatedPayload = this.createElectionRequest(payload.DAOid)
            i++
            this.sendPayload(p, generatedPayload)
            if (i == higherPeers.size) {
                lastTime = System.currentTimeMillis()
            }
        }
        while (System.currentTimeMillis() - lastTime < 1000) {
            // Wait for responses
        }
        if (this.candidates[payload.DAOid.decodeToString()]?.isEmpty() == true) {
            getCurrentLeader()[payload.DAOid.decodeToString()] = this.myPeer
            val electedPayload = this.createElectedResponse(payload.DAOid)
            this.sendPayload(peer, electedPayload)
        }
    }

    fun getCandidates(): HashMap<String, ArrayList<Peer>> {
        return this.candidates
    }

    fun getCurrentLeader(): HashMap<String, Peer?> {
        return this.currentLeader
    }

    fun getServiceIdNew(): String {
        return serviceId
    }

    fun leaderSignProposal(
        mostRecentSWBlock: TrustChainBlock,
        proposeBlockData: SWSignatureAskBlockTD,
        signatures: List<SWResponseSignatureBlockTD>,
        publicKeyBlock: ByteArray
    ) {
        Log.d("LEADER", "Leader doesn't exists.")
        Log.d("LEADER", "Requesting election...")
        makeToast("Requesting election...")

        val peers = this.getPeers()
        val peerPK = getPeersPKInDao(publicKeyBlock)
        for (peer in peers) {
            if (peer.publicKey == this.myPeer.publicKey) continue
            if (peerPK.contains(peer.publicKey.keyToBin().toHex())) {
                sendPayload(peer, this.createElectionRequest(publicKeyBlock))
                Log.d("LEADER", "Sending to peer at " + peer.address + " in " + serviceId + "...")
            }
        }
        Log.d("LEADER", "Waiting for leader...")
        while (!this.checkLeaderExists(publicKeyBlock)) {
            Thread.sleep(1000)
        }
        Log.d("LEADER", "Leader found.")

        val currentLeader = getCurrentLeader()[publicKeyBlock.decodeToString()]!!
        Log.d("LEADER", "sending dao join transaction data to leader ${currentLeader.publicKey}")
        makeToast("sending dao join transaction data to leader ${currentLeader.publicKey}",
            Toast.LENGTH_LONG)

        val payload =
            SignPayload(
                getServiceIdNew().toByteArray(),
                mostRecentSWBlock,
                proposeBlockData,
                signatures
            )
        val packet = serializePacket(MessageId.JOIN_DAO_DATA, payload)
        sendPayload(currentLeader, packet)

//        toastLeaderSignProposal(currentLeader.publicKey)
        makeToast("Sending DAO join data to ${currentLeader.publicKey}",
            Toast.LENGTH_LONG)
    }

    fun toastLeaderSignProposal(publicKey: PublicKey) {
        Toast.makeText(
            context,
            "Sending DAO join data to $publicKey",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun checkLeaderExists(dAOid: ByteArray): Boolean {
        return getCurrentLeader()[dAOid.decodeToString()] != null
    }

    fun fetchSignatureRequestProposalId(block: TrustChainBlock): String {
        if (block.type == SIGNATURE_ASK_BLOCK) {
            return SWSignatureAskTransactionData(block.transaction).getData().SW_UNIQUE_PROPOSAL_ID
        }
        if (block.type == TRANSFER_FUNDS_ASK_BLOCK) {
            return SWTransferFundsAskTransactionData(block.transaction).getData()
                .SW_UNIQUE_PROPOSAL_ID
        }

        return "invalid-proposal-id"
    }

    /**
     * Fetch all join and transfer proposals in descending timestamp order.
     * Speed assumption: each proposal has a unique proposal ID (distinct by unique proposal id,
     * without taking the unique wallet id into account).
     */
    fun fetchProposalBlocks(): List<TrustChainBlock> {
        val joinProposals = getTrustChainCommunity().database.getBlocksWithType(SIGNATURE_ASK_BLOCK)
        val transferProposals =
            getTrustChainCommunity().database.getBlocksWithType(
                TRANSFER_FUNDS_ASK_BLOCK
            )
        return joinProposals
            .union(transferProposals)
            .filter {
                fetchSignatureRequestReceiver(it) ==
                    myPeer.publicKey.keyToBin()
                        .toHex() && !checkEnoughFavorSignatures(it)
            }
            .distinctBy { fetchSignatureRequestProposalId(it) }
            .sortedByDescending { it.timestamp }
    }

    /**
     * Fetch all DAO blocks that contain a signature. These blocks are the response of a signature request.
     * Signatures are fetched from [SIGNATURE_AGREEMENT_BLOCK] type blocks.
     */
    fun fetchProposalResponses(
        walletId: String,
        proposalId: String
    ): List<SWResponseSignatureBlockTD> {
        return getTrustChainCommunity().database.getBlocksWithType(SIGNATURE_AGREEMENT_BLOCK)
            .filter {
                val blockData = SWResponseSignatureTransactionData(it.transaction)
                blockData.matchesProposal(walletId, proposalId)
            }.map {
                SWResponseSignatureTransactionData(it.transaction).getData()
            }
    }

    /**
     * Fetch all DAO blocks that contain a negative signature. These blocks are the response of a negative signature request.
     * Signatures are fetched from [SIGNATURE_AGREEMENT_NEGATIVE_BLOCK] type blocks.
     */
    fun fetchNegativeProposalResponses(
        walletId: String,
        proposalId: String
    ): List<SWResponseNegativeSignatureBlockTD> {
        return getTrustChainCommunity().database.getBlocksWithType(
            SIGNATURE_AGREEMENT_NEGATIVE_BLOCK
        )
            .filter {
                val blockData = SWResponseNegativeSignatureTransactionData(it.transaction)
                blockData.matchesProposal(walletId, proposalId)
            }.map {
                SWResponseNegativeSignatureTransactionData(it.transaction).getData()
            }
    }

    /**
     * Given a shared wallet proposal block, calculate the signature and respond with a trust chain block.
     */
    fun joinAskBlockReceived(
        block: TrustChainBlock,
        myPublicKey: ByteArray,
        votedInFavor: Boolean,
        context: Context
    ) {
        val latestHash =
            SWSignatureAskTransactionData(block.transaction).getData()
                .SW_PREVIOUS_BLOCK_HASH
        val mostRecentSWBlock =
            fetchLatestSharedWalletBlock(latestHash.hexToBytes())
                ?: throw IllegalStateException("Most recent DAO block not found")
        val joinBlock = SWJoinBlockTransactionData(mostRecentSWBlock.transaction).getData()
        val oldTransaction = joinBlock.SW_TRANSACTION_SERIALIZED

        DAOJoinHelper.joinAskBlockReceived(oldTransaction, block, joinBlock, myPublicKey, votedInFavor, context)
    }

    /**
     * Given a shared wallet transfer fund proposal block, calculate the signature and respond with a trust chain block.
     */
    fun transferFundsBlockReceived(
        block: TrustChainBlock,
        myPublicKey: ByteArray,
        votedInFavor: Boolean,
        context: Context
    ) {
        val latestHash =
            SWTransferFundsAskTransactionData(block.transaction).getData()
                .SW_PREVIOUS_BLOCK_HASH
        val mostRecentSWBlock =
            fetchLatestSharedWalletBlock(latestHash.hexToBytes())
                ?: throw IllegalStateException("Most recent DAO block not found")
        val transferBlock = SWTransferDoneTransactionData(mostRecentSWBlock.transaction).getData()
        val oldTransaction = transferBlock.SW_TRANSACTION_SERIALIZED

        DAOTransferFundsHelper.transferFundsBlockReceived(
            oldTransaction,
            block,
            transferBlock,
            myPublicKey,
            votedInFavor,
            context
        )
    }

    /**
     * Given a proposal, check if the number of signatures required is met
     */
    fun checkEnoughFavorSignatures(block: TrustChainBlock): Boolean {
        if (block.type == SIGNATURE_ASK_BLOCK) {
            val data = SWSignatureAskTransactionData(block.transaction).getData()
            val signatures =
                ArrayList(
                    fetchProposalResponses(
                        data.SW_UNIQUE_ID,
                        data.SW_UNIQUE_PROPOSAL_ID
                    )
                )
            return data.SW_SIGNATURES_REQUIRED <= signatures.size
        }
        if (block.type == TRANSFER_FUNDS_ASK_BLOCK) {
            val data = SWTransferFundsAskTransactionData(block.transaction).getData()
            val signatures =
                ArrayList(
                    fetchProposalResponses(
                        data.SW_UNIQUE_ID,
                        data.SW_UNIQUE_PROPOSAL_ID
                    )
                )
            return data.SW_SIGNATURES_REQUIRED <= signatures.size
        }

        return false
    }

    /**
     * Check if the number of required votes are more than the number of possible votes minus the negative votes.
     */
    fun canWinJoinRequest(data: SWSignatureAskBlockTD): Boolean {
        val sw =
            discoverSharedWallets().filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == data.SW_UNIQUE_ID }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()
        val againstSignatures =
            ArrayList(
                fetchNegativeProposalResponses(
                    data.SW_UNIQUE_ID,
                    data.SW_UNIQUE_PROPOSAL_ID
                )
            )
        val totalVoters = swData.SW_BITCOIN_PKS
        val requiredVotes = data.SW_SIGNATURES_REQUIRED

        return requiredVotes <= totalVoters.size - againstSignatures.size
    }

    /**
     * Check if the number of required votes are more than the number of possible votes minus the negative votes.
     */
    fun canWinTransferRequest(data: SWTransferFundsAskBlockTD): Boolean {
        val againstSignatures =
            ArrayList(
                fetchNegativeProposalResponses(
                    data.SW_UNIQUE_ID,
                    data.SW_UNIQUE_PROPOSAL_ID
                )
            )
        val totalVoters = data.SW_BITCOIN_PKS
        val requiredVotes = data.SW_SIGNATURES_REQUIRED

        return requiredVotes <= totalVoters.size - againstSignatures.size
    }

    object MessageId {
        const val ELECTION_REQUEST = 1
        const val ELECTED_RESPONSE = 2
        const val ALIVE_RESPONSE = 3
        const val JOIN_DAO_DATA = 4
    }

    companion object {
        // Default maximum wait timeout for bitcoin transaction broadcasts in seconds
        const val DEFAULT_BITCOIN_MAX_TIMEOUT: Long = 10

        // Block type for join DAO blocks
        const val JOIN_BLOCK = "v1DAO_JOIN"

        // Block type for transfer funds (from a DAO)
        const val TRANSFER_FINAL_BLOCK = "v1DAO_TRANSFER_FINAL"

        // Block type for basic signature requests
        const val SIGNATURE_ASK_BLOCK = "v1DAO_ASK_SIGNATURE"

        // Block type for transfer funds signature requests
        const val TRANSFER_FUNDS_ASK_BLOCK = "v1DAO_TRANSFER_ASK_SIGNATURE"

        // Block type for responding to a signature request with a (should be valid) signature
        const val SIGNATURE_AGREEMENT_BLOCK = "v1DAO_SIGNATURE_AGREEMENT"

        // Block type for responding with a negative vote to a signature request with a signature
        const val SIGNATURE_AGREEMENT_NEGATIVE_BLOCK = "v1DAO_SIGNATURE_AGREEMENT_NEGATIVE"
    }
}
