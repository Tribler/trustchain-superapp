package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.databinding.FragmentMySharedWalletsBinding
import nl.tudelft.trustchain.currencyii.databinding.JoinSwRowDataBinding
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import nl.tudelft.trustchain.currencyii.util.taproot.CTransaction
import org.bitcoinj.core.Coin

class SharedWalletListAdapter(
    private val context: BaseFragment,
    private val items: List<TrustChainBlock>,
    private val myPublicKey: String,
    private val listButtonText: String,
    private val disableOnUserJoined: Boolean? = false
) : BaseAdapter() {

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val binding = if (p1 != null) {
            JoinSwRowDataBinding.bind(p1)
        } else {
            JoinSwRowDataBinding.inflate(context.layoutInflater)
        }
        val view = binding.root
        val blockData = SWJoinBlockTransactionData(items[p0].transaction).getData()

        val walletId = binding.swIdItemT
        val votingThreshold = binding.swThresholdVt
        val entranceFee = binding.swEntranceFeeVt
        val nrOfUsers = binding.nrOfUsersTv
        val inWallet = binding.youJoinedTv
        val yourVotes = binding.yourVotesTv
        val clickToJoin = binding.clickToJoin
        val balance = binding.yourBalanceTv

        val trustchainPks = blockData.SW_TRUSTCHAIN_PKS
        val isUserInWallet = trustchainPks.contains(myPublicKey)

        val walletIdText = "${blockData.SW_UNIQUE_ID}"
        val votingThresholdText = "${blockData.SW_VOTING_THRESHOLD} %"
        val entranceFeeText = Coin.valueOf(blockData.SW_ENTRANCE_FEE).toFriendlyString()
        val users = "${trustchainPks.size} user(s) in this shared wallet"
        val inWalletText = "$isUserInWallet"
        val votes = "${trustchainPks.filter { it == myPublicKey }.size}"
        val previousTransaction =
            CTransaction().deserialize(blockData.SW_TRANSACTION_SERIALIZED.hexToBytes())

        walletId.text = walletIdText
        votingThreshold.text = votingThresholdText
        entranceFee.text = entranceFeeText
        nrOfUsers.text = users
        inWallet.text = inWalletText
        yourVotes.text = votes
        clickToJoin.text = listButtonText
        balance.text =
            Coin.valueOf(previousTransaction.vout.filter { it.scriptPubKey.size == 35 }[0].nValue)
                .toFriendlyString()

        if (this.disableOnUserJoined!! && isUserInWallet) {
            clickToJoin.isEnabled = false
            clickToJoin.setTextColor(Color.GRAY)
        }

        return view

    }

    override fun getItem(p0: Int): Any {
        return items[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getCount(): Int {
        return items.size
    }
}
