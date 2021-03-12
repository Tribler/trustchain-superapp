package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.keyvault.LibNaClPK
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
import org.bitcoinj.core.DumpedPrivateKey.fromBase58
import org.bitcoinj.core.LegacyAddress.fromBase58
import org.bitcoinj.crypto.BIP38PrivateKey.fromBase58
import org.bouncycastle.util.encoders.Base64Encoder
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class VotesFragment : BaseFragment(R.layout.fragment_votes) {
    private lateinit var tabsAdapter: TabsAdapter
    private lateinit var viewPager: ViewPager2

    private val TAB_NAMES = arrayOf("Upvotes", "Downvotes", "Not voted")

    private var voters: HashMap<Int, ArrayList<String>> = hashMapOf()
    private lateinit var title: TextView
    private lateinit var price: TextView
    private lateinit var demoVoteFab: ExtendedFloatingActionButton
    private lateinit var voteFab: ExtendedFloatingActionButton
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_votes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title = view.findViewById(R.id.title)
        price = view.findViewById(R.id.price)
        demoVoteFab = view.findViewById(R.id.fab_demo)
        voteFab = view.findViewById(R.id.fab_user)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.viewpager)

        demoVoteFab.visibility = View.GONE

        val localArgs = arguments
        if (localArgs is Bundle) {
            val type = localArgs.getString("type")
            val blockId = localArgs.getString("blockId")!!

            if (type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
                signatureAskBlockVotes(blockId)
            } else if (type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
                transferFundsAskBlockVotes(blockId)
            }
        }
        if (voters.size == 0) return

        tabsAdapter = TabsAdapter(this, voters)
        viewPager.adapter = tabsAdapter

        updateTabNames()
    }

    private fun updateTabNames() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = TAB_NAMES[position] + " (" + voters[position]!!.size + ")"
        }.attach()
    }

    private fun getSelectedBlock(blockId: String): TrustChainBlock? {
        val allBlocks = getCoinCommunity().fetchProposalBlocks()
        for (block in allBlocks) {
            if (block.blockId == blockId) return block
        }
        findNavController().navigateUp()
        Toast.makeText(this.context, "Something went wrong while fetching this block\nYou have ${allBlocks.size} blocks available", Toast.LENGTH_SHORT).show()
        return null
    }

    private fun signatureAskBlockVotes(blockId: String) {
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin()
        val block = getSelectedBlock(blockId) ?: return

        val rawData = SWSignatureAskTransactionData(block.transaction)
        val data = rawData.getData()

        val walletId = data.SW_UNIQUE_ID

        // TODO: Crashes when user has no wallet, but that isn't possible otherwise he shouldn't see the proposal at the first place.
        val sw = getCoinCommunity().discoverSharedWallets()
            .filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == walletId }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()

        val requestToJoinId = sw.publicKey.toHex()

        // TODO get the actual votes, instead of only the participants
        voters = hashMapOf(0 to arrayListOf(), 1 to arrayListOf(), 2 to swData.SW_TRUSTCHAIN_PKS)

        // Get my signature
        val walletManager = WalletManagerAndroid.getInstance()

        val latestHash = data.SW_PREVIOUS_BLOCK_HASH
        val mostRecentSWBlock =
            getCoinCommunity().fetchLatestSharedWalletBlock(latestHash.hexToBytes())
                ?: throw IllegalStateException("Most recent DAO block not found")
        val oldTransaction = SWJoinBlockTransactionData(mostRecentSWBlock.transaction).getData()
            .SW_TRANSACTION_SERIALIZED

        val newTransactionSerialized = data.SW_TRANSACTION_SERIALIZED
        val mySignature = walletManager.safeSigningJoinWalletTransaction(
            Transaction(walletManager.params, newTransactionSerialized.hexToBytes()),
            Transaction(walletManager.params, oldTransaction.hexToBytes()),
            walletManager.protocolECKey()
        )
        val mySignatureSerialized = mySignature.encodeToDER().toHex()

        // TODO get the id of the users that already voted, the signatures aren't the same, but they represent the number of upvotes
        val signatures =
            ArrayList(
                getCoinCommunity().fetchProposalSignatures(
                    data.SW_UNIQUE_ID,
                    data.SW_UNIQUE_PROPOSAL_ID
                )
            )

        val negativeSignatures = ArrayList( getCoinCommunity().fetchNegativeProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

        val favorPKs = signatures//ArrayList(signatures.map { getPK(it, swData.SW_BITCOIN_PKS, oldTransaction) })
        val againstPKs = negativeSignatures//ArrayList(negativeSignatures.map { getPK(it, swData.SW_BITCOIN_PKS, oldTransaction) })

        voters[0] = favorPKs
        voters[1] = againstPKs
        voters[2]!!.removeAll(againstPKs)
        voters[2]!!.removeAll(favorPKs)

        val userHasVoted = voters[0]!!.contains(mySignatureSerialized) || voters[1]!!.contains(
            mySignatureSerialized
        )

        title.text = data.SW_UNIQUE_PROPOSAL_ID
        price.text = getString(
            R.string.vote_join_request_message,
            requestToJoinId,
            walletId,
            data.SW_SIGNATURES_REQUIRED
        )
        voteFab.setOnClickListener { v ->
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle(R.string.vote_join_request_title)
            builder.setMessage(
                getString(
                    R.string.vote_join_request_message,
                    requestToJoinId,
                    walletId,
                    data.SW_SIGNATURES_REQUIRED
                )
            )
            builder.setPositiveButton("YES") { _, _ ->
                // Update the voter's list, because I voted yes
                voters = SWSignatureAskTransactionData(block.transaction).userVotes(
                    myPublicKey.toHex(),
                    0
                )

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 0
                Toast.makeText(
                    v.context,
                    getString(R.string.vote_join_request_upvoted, requestToJoinId, walletId),
                    Toast.LENGTH_SHORT
                ).show()
                updateTabNames()

                // Send yes vote
                getCoinCommunity().joinAskBlockReceived(block, myPublicKey, true)

                if (voters[2]!!.size == 0) {
                    findNavController().navigateUp()
                }
            }

            builder.setNeutralButton("NO") { _, _ ->
                // Update the voter's list, because I voted no
                voters = SWSignatureAskTransactionData(block.transaction).userVotes(
                    myPublicKey.toHex(),
                    1
                )

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 1
                Toast.makeText(
                    v.context,
                    getString(R.string.vote_join_request_downvoted, requestToJoinId, walletId),
                    Toast.LENGTH_SHORT
                ).show()
                updateTabNames()

                // Send no vote
                getCoinCommunity().joinAskBlockReceived(block, myPublicKey, false)
            }
            builder.show()
        }

        if (userHasVoted) {
            userHasAlreadyVoted()
        }
    }

    private fun transferFundsAskBlockVotes(blockId: String) {
        val myPublicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin()
        val block = getSelectedBlock(blockId) ?: return

        val rawData = SWTransferFundsAskTransactionData(block.transaction)
        val data = rawData.getData()

        val walletId = data.SW_UNIQUE_ID
        val priceString = data.SW_TRANSFER_FUNDS_AMOUNT.toString() + " Satoshi"

        val sw = getCoinCommunity().discoverSharedWallets()
            .filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == walletId }[0]
        val swData = SWJoinBlockTransactionData(sw.transaction).getData()

        // TODO get the actual votes, instead of only the participants
        voters = hashMapOf(0 to arrayListOf(), 1 to arrayListOf(), 2 to swData.SW_TRUSTCHAIN_PKS)

        // Get my signature
        val walletManager = WalletManagerAndroid.getInstance()

        val latestHash = data.SW_PREVIOUS_BLOCK_HASH
        val mostRecentSWBlock =
            getCoinCommunity().fetchLatestSharedWalletBlock(latestHash.hexToBytes())
                ?: throw IllegalStateException("Most recent DAO block not found")
        val oldTransaction = SWJoinBlockTransactionData(mostRecentSWBlock.transaction).getData()
            .SW_TRANSACTION_SERIALIZED

        val satoshiAmount = Coin.valueOf(data.SW_TRANSFER_FUNDS_AMOUNT)
        val previousTransaction = Transaction(
            walletManager.params,
            oldTransaction.hexToBytes()
        )
        val receiverAddress = Address.fromString(
            walletManager.params,
            data.SW_TRANSFER_FUNDS_TARGET_SERIALIZED
        )
        val mySignature = walletManager.safeSigningTransactionFromMultiSig(
            previousTransaction,
            walletManager.protocolECKey(),
            receiverAddress,
            satoshiAmount
        )

        val mySignatureSerialized = mySignature.encodeToDER().toHex()


        // TODO get the id of the users that already voted, the signatures aren't the same, but they represent the number of upvotes
        val signatures =
            ArrayList(
                getCoinCommunity().fetchProposalSignatures(
                    data.SW_UNIQUE_ID,
                    data.SW_UNIQUE_PROPOSAL_ID
                )
            )

        voters[0] = signatures
        voters[2]!!.removeAll(signatures)

        val userHasVoted = voters[0]!!.contains(mySignatureSerialized) || voters[1]!!.contains(
            mySignatureSerialized
        )

        title.text = data.SW_UNIQUE_PROPOSAL_ID
        price.text = getString(R.string.bounty_payout, priceString, walletId)
        voteFab.setOnClickListener { v ->
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle(getString(R.string.bounty_payout, priceString, walletId))
            builder.setMessage(
                getString(
                    R.string.bounty_payout_message,
                    priceString,
                    walletId,
                    voters[0]!!.size,
                    voters[1]!!.size,
                    voters[2]!!.size
                )
            )
            builder.setPositiveButton("YES") { _, _ ->
                // Update the voter's list, because I voted yes
                voters = SWTransferFundsAskTransactionData(block.transaction).userVotes(
                    myPublicKey.toHex(),
                    0
                )

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 0
                Toast.makeText(
                    v.context,
                    getString(R.string.bounty_payout_upvoted, priceString, walletId),
                    Toast.LENGTH_SHORT
                ).show()
                updateTabNames()

                // Send yes vote
                getCoinCommunity().transferFundsBlockReceived(block, myPublicKey)

                if (voters[2]!!.size == 0) {
                    findNavController().navigateUp()
                }
            }

            builder.setNeutralButton("NO") { _, _ ->
                // Update the voter's list, because I voted no
                voters = SWTransferFundsAskTransactionData(block.transaction).userVotes(
                    myPublicKey.toHex(),
                    1
                )

                // Update the GUI
                userHasAlreadyVoted()
                tabsAdapter = TabsAdapter(this, voters)
                viewPager.adapter = tabsAdapter
                viewPager.currentItem = 1
                Toast.makeText(
                    v.context,
                    getString(R.string.bounty_payout_downvoted, priceString, walletId),
                    Toast.LENGTH_SHORT
                ).show()
                updateTabNames()

                // TODO: send no vote for user
            }
            builder.show()
        }

        if (userHasVoted) {
            userHasAlreadyVoted()
        }
    }

    private fun userHasAlreadyVoted() {
        voteFab.visibility = View.GONE
    }

    private fun getPK(
        signature: String,
        bitcoin_pks: ArrayList<String>,
        swTransactionSerialized: String
    ): String {
        val signatureKey = ECKey.ECDSASignature.decodeFromDER(signature.hexToBytes())

        for (pk in bitcoin_pks) {
            val result = ECKey.verify(swTransactionSerialized.hexToBytes(), signatureKey, pk.hexToBytes())


            val walletManager = WalletManagerAndroid.getInstance()
            val key = walletManager.protocolECKey().pubKey.toHex()
//            val myPk = walletManager.networkPublicECKeyHex()
            println(key)


//            val message = "hello World"
//            val signatureBytes = signature.hexToBytes()
//            val signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes)
//            val key = ECKey.recoverFromSignature(1, signatureKey, Sha256Hash.of(swTransactionSerialized.hexToBytes()), true)
//            println(key)
//            key.toAddress(Address.fromBase58(null, pk).getParameters()).toString().equals(pk);
            println(result)
            println(bitcoin_pks)
            println(swTransactionSerialized)
        }

        return "Unknown signature found"
    }
}
