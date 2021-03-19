package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.R
import nl.tudelft.trustchain.common.ui.TabsAdapter
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.core.*

/**
 * The class for showing the votes fragment. This class is helped by VotesFragmentHelper.kt in Common.
 * It shows the layout in R.layout.fragment_votes in common.
 * A user can here see the (up/down/undecided)votes and send a vote himself
 */
class VotesFragment : BaseFragment(R.layout.fragment_votes) {
    // From common helper class
    private lateinit var tabsAdapter: TabsAdapter
    private lateinit var viewPager: ViewPager2

    private val TAB_NAMES = arrayOf("Upvotes", "Downvotes", "Not voted")

    // From the layout class
    private var voters: Array<ArrayList<String>> = arrayOf(ArrayList(), ArrayList(), ArrayList())
    private lateinit var title: TextView
    private lateinit var subTitle: TextView
    private lateinit var requiredVotes: TextView
    private lateinit var voteFab: ExtendedFloatingActionButton
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_votes, container, false)
    }

    /**
     * When the view is created it puts the correct data on it.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title = view.findViewById(R.id.title)
        subTitle = view.findViewById(R.id.sub_title)
        requiredVotes = view.findViewById(R.id.required_votes)
        voteFab = view.findViewById(R.id.fab_user)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.viewpager)

        val localArgs = arguments
        if (localArgs is Bundle) {
            val type = localArgs.getString("type")
            val blockId = localArgs.getString("blockId")!!

            // Check which type the block is and set the corresponding data
            if (type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
                signatureAskBlockVotes(blockId)
            } else if (type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
                transferFundsAskBlockVotes(blockId)
            }
        }
        // When there are no participants, there is an error and we should return.
        if (voters.isEmpty()) return

        // Set the tabs with the helper class in common
        tabsAdapter = TabsAdapter(this, voters)
        viewPager.adapter = tabsAdapter

        updateTabNames()
    }

    /**
     * Set the tab names with the number of votes in brackets
     */
    private fun updateTabNames() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = TAB_NAMES[position] + " (" + voters[position].size + ")"
        }.attach()
    }

    /**
     * Get the selected block via the blockId parsed.
     * @param blockId - the id of the block that is clicked
     * @return TrustChainBlock
     */
    private fun getSelectedBlock(blockId: String): TrustChainBlock? {
        // TODO: Check if this is the correct way to fetch all proposal blocks, because sometimes it crashes?
        var allBlocks: List<TrustChainBlock> = getCoinCommunity().fetchProposalBlocks()

        val allUsers = getDemoCommunity().getPeers()
        for (peer in allUsers) {
            lifecycleScope.launch {
                trustchain.crawlChain(peer)
            }
            val crawlResult = trustchain
                .getChainByUser(peer.publicKey.keyToBin())
                .filter {
                    it.type == CoinCommunity.SIGNATURE_ASK_BLOCK ||
                        it.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK
                }
            allBlocks = allBlocks + crawlResult
        }
        for (block in allBlocks) {
            if (block.blockId == blockId) return block
        }

        // If we couldn't find the block then move one page up and show a toast with error message
        findNavController().navigateUp()
        Toast.makeText(this.context, "Something went wrong while fetching this block\nYou have ${allBlocks.size} blocks available", Toast.LENGTH_SHORT).show()
        return null
    }

    /**
     * The method for setting the data for join requests
     */
    private fun signatureAskBlockVotes(blockId: String) {
        val walletManager = WalletManagerAndroid.getInstance()
        val myPublicBitcoinKey = walletManager.protocolECKey().publicKeyAsHex
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin()
        val block = getSelectedBlock(blockId) ?: return

        val rawData = SWSignatureAskTransactionData(block.transaction)
        val data = rawData.getData()

        val walletId = data.SW_UNIQUE_ID

        // TODO: Crashes when user has no wallet, but that isn't possible otherwise he shouldn't see the proposal at the first place.
        // Get information about the shared wallet
        val sw = getCoinCommunity().discoverSharedWallets()
            .filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == walletId }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()

        // Get the id of the person that wants to join
        val requestToJoinId = sw.publicKey.toHex()

        // Get the favor and against votes
        val signatures = ArrayList(getCoinCommunity().fetchProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
        val negativeSignatures = ArrayList(getCoinCommunity().fetchNegativeProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

        // Recalculate the signatures to the PKs
        val favorPKs = ArrayList(signatures.map { getPKJoin(it, swData.SW_BITCOIN_PKS, block) })
        val againstPKs = ArrayList(negativeSignatures.map { getPKJoin(it, swData.SW_BITCOIN_PKS, block) })

        // Set the voters so that they are visible in the different kind of tabs
        setVoters(swData.SW_BITCOIN_PKS, favorPKs, againstPKs)

        // Check if I have already voted
        userHasAlreadyVoted(myPublicBitcoinKey)

        title.text = data.SW_UNIQUE_PROPOSAL_ID
        subTitle.text = getString(R.string.vote_join_request_message, requestToJoinId, walletId)
        // Check if the proposal can still be met.
        if (getCoinCommunity().canWinJoinRequest(data)) {
            requiredVotes.text = getString(R.string.votes_required, data.SW_SIGNATURES_REQUIRED)
        } else {
            requiredVotes.text = getString(R.string.votes_required_fails, data.SW_SIGNATURES_REQUIRED)
            requiredVotes.setTextColor(Color.RED)
        }
        // Vote functionality
        voteFab.setOnClickListener { v ->
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle(R.string.vote_join_request_title)
            builder.setMessage(getString(R.string.vote_join_request_message, requestToJoinId, walletId))
            builder.setPositiveButton("YES") { _, _ ->
                // Update the voter's list, because I voted yes
                voters[2].remove(myPublicBitcoinKey)
                voters[0].add(myPublicBitcoinKey)

                // Update the GUI
                userHasAlreadyVoted(myPublicBitcoinKey)
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 0
                Toast.makeText(v.context, getString(R.string.vote_join_request_upvoted, requestToJoinId, walletId), Toast.LENGTH_SHORT).show()
                updateTabNames()

                // Send yes vote
                getCoinCommunity().joinAskBlockReceived(block, myPublicKey, true)
                Log.i("Coin", "Voted yes on joining of: ${block.transaction}")
            }

            builder.setNeutralButton("NO") { _, _ ->
                // Update the voter's list, because I voted no
                voters[2].remove(myPublicBitcoinKey)
                voters[1].add(myPublicBitcoinKey)

                // Update the GUI
                userHasAlreadyVoted(myPublicBitcoinKey)
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 1
                Toast.makeText(v.context, getString(R.string.vote_join_request_downvoted, requestToJoinId, walletId), Toast.LENGTH_SHORT).show()
                updateTabNames()

                // Send no vote
                getCoinCommunity().joinAskBlockReceived(block, myPublicKey, false)
                Log.i("Coin", "Voted no on joining of: ${block.transaction}")
            }
            builder.show()
        }
    }

    /**
     * The method for setting the data for transfer funds requests
     */
    private fun transferFundsAskBlockVotes(blockId: String) {
        val walletManager = WalletManagerAndroid.getInstance()
        val myPublicBitcoinKey = walletManager.protocolECKey().publicKeyAsHex
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin()
        val block = getSelectedBlock(blockId) ?: return

        val rawData = SWTransferFundsAskTransactionData(block.transaction)
        val data = rawData.getData()

        val walletId = data.SW_UNIQUE_ID
        val priceString = data.SW_TRANSFER_FUNDS_AMOUNT.toString() + " Satoshi"

        // TODO: Crashes when user has no wallet, but that isn't possible otherwise he shouldn't see the proposal at the first place.
        // Get information about the shared wallet
        val sw = getCoinCommunity().discoverSharedWallets()
            .filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == walletId }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()

        // Get the favor and against votes
        val signatures = ArrayList(getCoinCommunity().fetchProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
        val negativeSignatures = ArrayList(getCoinCommunity().fetchNegativeProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

        // Recalculate the signatures to the PKs
        val favorPKs = ArrayList(signatures.map { getPKTransfer(it, swData.SW_BITCOIN_PKS, block) })
        val againstPKs = ArrayList(negativeSignatures.map { getPKTransfer(it, swData.SW_BITCOIN_PKS, block) })

        // Set the voters so that they are visible in the different kind of tabs
        setVoters(swData.SW_BITCOIN_PKS, favorPKs, againstPKs)

        // Check if I have already voted
        userHasAlreadyVoted(myPublicBitcoinKey)

        title.text = data.SW_UNIQUE_PROPOSAL_ID
        subTitle.text = getString(R.string.bounty_payout, priceString, walletId)
        // Check if the proposal can still be met.
        if (getCoinCommunity().canWinTransferRequest(data)) {
            requiredVotes.text = getString(R.string.votes_required, data.SW_SIGNATURES_REQUIRED)
        } else {
            requiredVotes.text = getString(R.string.votes_required_fails, data.SW_SIGNATURES_REQUIRED)
            requiredVotes.setTextColor(Color.RED)
        }
        // Vote functionality
        voteFab.setOnClickListener { v ->
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle(getString(R.string.bounty_payout, priceString, walletId))
            builder.setMessage(
                getString(
                    R.string.bounty_payout_message,
                    priceString,
                    walletId,
                    voters[0].size,
                    voters[1].size,
                    voters[2].size
                )
            )
            builder.setPositiveButton("YES") { _, _ ->
                // Update the voter's list, because I voted yes
                voters[2].remove(myPublicBitcoinKey)
                voters[0].add(myPublicBitcoinKey)

                // Update the GUI
                userHasAlreadyVoted(myPublicBitcoinKey)
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 0
                Toast.makeText(v.context, getString(R.string.bounty_payout_upvoted, priceString, walletId), Toast.LENGTH_SHORT).show()
                updateTabNames()

                // Send yes vote
                getCoinCommunity().transferFundsBlockReceived(block, myPublicKey, true)
                Log.i("Coin", "Voted yes on transferring funds of: ${block.transaction}")
            }

            builder.setNeutralButton("NO") { _, _ ->
                // Update the voter's list, because I voted no
                voters[2].remove(myPublicBitcoinKey)
                voters[1].add(myPublicBitcoinKey)

                // Update the GUI
                userHasAlreadyVoted(myPublicBitcoinKey)
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 1
                Toast.makeText(v.context, getString(R.string.bounty_payout_downvoted, priceString, walletId), Toast.LENGTH_SHORT).show()
                updateTabNames()

                // Send no vote
                getCoinCommunity().transferFundsBlockReceived(block, myPublicKey, false)
                Log.i("Coin", "Voted yes on transferring funds of: ${block.transaction}")
            }
            builder.show()
        }
    }

    /**
     * When the user has already voted, or made a vote
     * It hides the vote button and maybe something more in the future.
     */
    private fun userHasAlreadyVoted(myPublicBitcoinKey: String) {
        if (!voters[2].contains(myPublicBitcoinKey)) {
            voteFab.visibility = View.GONE
        }
    }

    /**
     * Get the primary corresponding to the signature.
     * @param signature - The signature string that signed the message
     * @param bitcoin_pks - A list with all the bitcoin primary keys to compare the signature with
     * @param block - The Trustchainblock
     * @return String - Public Key
     */
    private fun getPKJoin(
        signature: String,
        bitcoin_pks: ArrayList<String>,
        block: TrustChainBlock
    ): String {
        val signatureKey: ECKey.ECDSASignature = ECKey.ECDSASignature.decodeFromDER(signature.hexToBytes())

        val latestHash = SWSignatureAskTransactionData(block.transaction).getData()
            .SW_PREVIOUS_BLOCK_HASH
        val mostRecentSWBlock = getCoinCommunity().fetchLatestSharedWalletBlock(latestHash.hexToBytes())
            ?: throw IllegalStateException("Most recent DAO block not found")
        val oldTransactionSerialized = SWJoinBlockTransactionData(mostRecentSWBlock.transaction).getData()
            .SW_TRANSACTION_SERIALIZED

        val walletManager = WalletManagerAndroid.getInstance()
        val blockData = SWSignatureAskTransactionData(block.transaction).getData()

        val newTransactionSerialized = blockData.SW_TRANSACTION_SERIALIZED
        val newTransaction = Transaction(walletManager.params, newTransactionSerialized.hexToBytes())
        val oldTransaction = Transaction(walletManager.params, oldTransactionSerialized.hexToBytes())

        val oldMultiSignatureOutput = walletManager.getMultiSigOutput(oldTransaction).unsignedOutput

        val sighash: Sha256Hash = newTransaction.hashForSignature(
            0,
            oldMultiSignatureOutput.scriptPubKey,
            Transaction.SigHash.ALL,
            false
        )

        for (pk in bitcoin_pks) {
            val key = ECKey.fromPublicOnly(pk.hexToBytes())
            val result = key.verify(sighash, signatureKey)
            if (result) {
                return pk
            }
        }
        return "Unrecognized signature received"
    }

    /**
     * Get the primary corresponding to the signature.
     * @param signature - The signature string that signed the message
     * @param bitcoin_pks - A list with all the bitcoin primary keys to compare the signature with
     * @param block - The Trustchainblock
     * @return String - Public Key
     */
    private fun getPKTransfer(
        signature: String,
        bitcoin_pks: ArrayList<String>,
        block: TrustChainBlock
    ): String {
        val signatureKey: ECKey.ECDSASignature = ECKey.ECDSASignature.decodeFromDER(signature.hexToBytes())

        val latestHash = SWTransferFundsAskTransactionData(block.transaction).getData()
            .SW_PREVIOUS_BLOCK_HASH
        val mostRecentSWBlock = getCoinCommunity().fetchLatestSharedWalletBlock(latestHash.hexToBytes())
            ?: throw IllegalStateException("Most recent DAO block not found")
        val oldTransactionSerialized = SWJoinBlockTransactionData(mostRecentSWBlock.transaction).getData()
            .SW_TRANSACTION_SERIALIZED

        val walletManager = WalletManagerAndroid.getInstance()
        val blockData = SWTransferFundsAskTransactionData(block.transaction).getData()

        val satoshiAmount = Coin.valueOf(blockData.SW_TRANSFER_FUNDS_AMOUNT)
        val previousTransaction = Transaction(walletManager.params, oldTransactionSerialized.hexToBytes())
        val receiverAddress = Address.fromString(walletManager.params, blockData.SW_TRANSFER_FUNDS_TARGET_SERIALIZED)

        val previousMultiSigOutput: TransactionOutput =
            walletManager.getMultiSigOutput(previousTransaction).unsignedOutput

        // Create the transaction which will have the multisig output as input,
        // The outputs will be the receiver address and another one for residual funds
        val spendTx =
            walletManager.createMultiSigPaymentTx(receiverAddress, satoshiAmount, previousMultiSigOutput)

        // Sign the transaction and return it.
        val multiSigScript = previousMultiSigOutput.scriptPubKey
        val sighash: Sha256Hash =
            spendTx.hashForSignature(0, multiSigScript, Transaction.SigHash.ALL, false)

        for (pk in bitcoin_pks) {
            val key = ECKey.fromPublicOnly(pk.hexToBytes())
            val result = key.verify(sighash, signatureKey)
            if (result) {
                return pk
            }
        }
        return "Unrecognized signature received"
    }

    /**
     * Set the voters, ordered by if they voted in favor, against or not.
     * @param participants - All the participants of the DAO.
     * @param favorPKs - All the primary keys of people that already voted in favor.
     * @param againstPKs - All the primary keys of people that already voted against.
     */
    private fun setVoters(
        participants: ArrayList<String>,
        favorPKs: ArrayList<String>,
        againstPKs: ArrayList<String>
    ) {
        voters[0] = favorPKs
        voters[1] = againstPKs
        voters[2] = participants

        // If a user has already voted remove their entry from the participants
        for (favorPK in favorPKs) {
            voters[2].remove(favorPK)
        }
        for (againstPk in againstPKs) {
            voters[2].remove(againstPk)
        }
    }
}
