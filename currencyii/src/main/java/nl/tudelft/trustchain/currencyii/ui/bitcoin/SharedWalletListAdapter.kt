package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.currencyii.R

class SharedWalletListAdapter(
    private val context: BaseFragment,
    private val items: List<TrustChainBlock>,
    private val myPublicKey: String,
    private val listButtonText: String
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

        val trustchainPks = blockData.SW_TRUSTCHAIN_PKS

        val walletIdText = "${blockData.SW_UNIQUE_ID}"
        val votingThresholdText = "${blockData.SW_VOTING_THRESHOLD} %"
        val entranceFeeText = "${blockData.SW_ENTRANCE_FEE} Satoshi"
        val users = "${trustchainPks.size} user(s) in this shared wallet"
        val inWalletText = "${trustchainPks.contains(myPublicKey)}"
        val votes = "${trustchainPks.filter { it == myPublicKey }.size}"

        walletId.text = walletIdText
        votingThreshold.text = votingThresholdText
        entranceFee.text = entranceFeeText
        nrOfUsers.text = users
        inWallet.text = inWalletText
        yourVotes.text = votes
        clickToJoin.text = listButtonText

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
