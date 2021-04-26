package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.currencyii.R
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
        val view = context.layoutInflater.inflate(R.layout.join_sw_row_data, null, false)

        val blockData = SWJoinBlockTransactionData(items[p0].transaction).getData()

        val walletId = view.findViewById<TextView>(R.id.sw_id_item_t)
        val votingThreshold = view.findViewById<TextView>(R.id.sw_threshold_vt)
        val entranceFee = view.findViewById<TextView>(R.id.sw_entrance_fee_vt)
        val nrOfUsers = view.findViewById<TextView>(R.id.nr_of_users_tv)
        val inWallet = view.findViewById<TextView>(R.id.you_joined_tv)
        val yourVotes = view.findViewById<TextView>(R.id.your_votes_tv)
        val clickToJoin = view.findViewById<TextView>(R.id.click_to_join)
        val balance = view.findViewById<TextView>(R.id.your_balance_tv)

        val trustchainPks = blockData.SW_TRUSTCHAIN_PKS
        val isUserInWallet = trustchainPks.contains(myPublicKey)

        val walletIdText = "${blockData.SW_UNIQUE_ID}"
        val votingThresholdText = "${blockData.SW_VOTING_THRESHOLD} %"
        val entranceFeeText = Coin.valueOf(blockData.SW_ENTRANCE_FEE).toFriendlyString()
        val users = "${trustchainPks.size} user(s) in this shared wallet"
        val inWalletText = "$isUserInWallet"
        val votes = "${trustchainPks.filter { it == myPublicKey }.size}"
        val previousTransaction = CTransaction().deserialize(blockData.SW_TRANSACTION_SERIALIZED.hexToBytes())

        walletId.text = walletIdText
        votingThreshold.text = votingThresholdText
        entranceFee.text = entranceFeeText
        nrOfUsers.text = users
        inWallet.text = inWalletText
        yourVotes.text = votes
        clickToJoin.text = listButtonText
        balance.text = Coin.valueOf(previousTransaction.vout.filter { it.scriptPubKey.size == 35 }[0].nValue).toFriendlyString()

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
