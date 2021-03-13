package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import java.text.SimpleDateFormat

class ProposalListAdapter(
    private val context: BaseFragment,
    private val items: List<TrustChainBlock>
) : BaseAdapter() {

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val view = context.layoutInflater.inflate(R.layout.proposal_row_data, null, false)

        val block = items[p0]
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm")

        val about = view.findViewById<TextView>(R.id.about_tv)
        val createdAt = view.findViewById<TextView>(R.id.timestamp_tv)
        val doaId = view.findViewById<TextView>(R.id.dao_id_tv)
        val proposalId = view.findViewById<TextView>(R.id.proposal_id_tv)
        val signaturesRequired = view.findViewById<TextView>(R.id.signatures_required_tv)
        val transferReceiver = view.findViewById<TextView>(R.id.transfer_target_tv)
        val transferAmount = view.findViewById<TextView>(R.id.transfer_amount_tv)

        if (block.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
            val data = SWTransferFundsAskTransactionData(block.transaction).getData()
            val signatures = ArrayList(context.getCoinCommunity().fetchProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
            val againstSignatures = ArrayList(context.getCoinCommunity().fetchNegativeProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
            val totalVoters = data.SW_BITCOIN_PKS
            val requiredVotes = data.SW_SIGNATURES_REQUIRED

            if (requiredVotes > totalVoters.size - againstSignatures.size)  {
                view.setBackgroundResource(R.drawable.border)
            }

            about.text = "Transfer funds request"
            createdAt.text = formatter.format(block.timestamp)
            doaId.text = data.SW_UNIQUE_ID
            proposalId.text = data.SW_UNIQUE_PROPOSAL_ID
            signaturesRequired.text = signatures.size.toString() + "/" + data.SW_SIGNATURES_REQUIRED.toString()
            transferReceiver.text = data.SW_TRANSFER_FUNDS_TARGET_SERIALIZED
            transferAmount.text = "${data.SW_TRANSFER_FUNDS_AMOUNT} Satoshi"
        } else if (block.type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
            val data = SWSignatureAskTransactionData(block.transaction).getData()

            val signatures = ArrayList(context.getCoinCommunity().fetchProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
            val sw = context.getCoinCommunity().discoverSharedWallets()
                .filter { b -> SWJoinBlockTransactionData(b.transaction).getData().SW_UNIQUE_ID == data.SW_UNIQUE_ID }[0]
            val swData = SWJoinBlockTransactionData(sw.transaction).getData()
            val againstSignatures = ArrayList(context.getCoinCommunity().fetchNegativeProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
            val totalVoters = swData.SW_BITCOIN_PKS
            val requiredVotes = data.SW_SIGNATURES_REQUIRED

            if (requiredVotes > totalVoters.size - againstSignatures.size)  {
                view.setBackgroundResource(R.drawable.border)
            }

            about.text = "Join request"
            createdAt.text = formatter.format(block.timestamp)
            doaId.text = data.SW_UNIQUE_ID
            proposalId.text = data.SW_UNIQUE_PROPOSAL_ID
            signaturesRequired.text = signatures.size.toString() + "/" + data.SW_SIGNATURES_REQUIRED.toString()

            // Hide the components only used for transfer funds
            hideTransferProposalComponents(view)
        }

        return view
    }

    private fun hideTransferProposalComponents(view: View) {
        val transferReceiverLabel = view.findViewById<TextView>(R.id.transfer_target)
        val transferAmountLabel = view.findViewById<TextView>(R.id.transfer_amount)
        val transferReceiver = view.findViewById<TextView>(R.id.transfer_target_tv)
        val transferAmount = view.findViewById<TextView>(R.id.transfer_amount_tv)
        transferReceiverLabel?.visibility = View.GONE
        transferAmountLabel?.visibility = View.GONE
        transferReceiver?.visibility = View.GONE
        transferAmount?.visibility = View.GONE
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
