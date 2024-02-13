package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import nl.tudelft.trustchain.currencyii.util.taproot.CTransaction
import org.bitcoinj.core.Coin
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
        val daoId = view.findViewById<TextView>(R.id.dao_id_tv)
        val proposalId = view.findViewById<TextView>(R.id.proposal_id_tv)
        val signaturesRequired = view.findViewById<TextView>(R.id.signatures_required_tv)
        val transferReceiver = view.findViewById<TextView>(R.id.transfer_target_tv)
        val transferAmount = view.findViewById<TextView>(R.id.transfer_amount_tv)
        val votedButton = view.findViewById<TextView>(R.id.voted_button)
        val balance = view.findViewById<TextView>(R.id.dao_balance_tv)
        val balanceText = view.findViewById<TextView>(R.id.balance_tv)

        val walletManager = WalletManagerAndroid.getInstance()

        if (block.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
            val data = SWTransferFundsAskTransactionData(block.transaction).getData()
            // Get favor votes
            val favorVotes = ArrayList(context.getCoinCommunity().fetchProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID)).map { it.SW_BITCOIN_PK }
            // Get against votes
            val negativeVotes = ArrayList(context.getCoinCommunity().fetchNegativeProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID)).map { it.SW_BITCOIN_PK }

            // Check if I voted
            val myPublicBitcoinKey = walletManager.protocolECKey().publicKeyAsHex
            if (favorVotes.contains(myPublicBitcoinKey) || negativeVotes.contains(myPublicBitcoinKey)) {
                votedButton.visibility = View.VISIBLE
            }

            // If the proposal can't be met anymore, draw a red border
            if (!context.getCoinCommunity().canWinTransferRequest(data)) {
                view.setBackgroundResource(R.drawable.border)
            }

            val previousTransaction = CTransaction().deserialize(
                data.SW_TRANSACTION_SERIALIZED.hexToBytes()
            )

            val previousMultiSigOutput = previousTransaction.vout.filter { it.scriptPubKey.size == 35 }[0]

            balanceText.visibility = View.VISIBLE
            balance.visibility = View.VISIBLE

            about.text = "Transfer funds request"
            createdAt.text = formatter.format(block.timestamp)
            daoId.text = data.SW_UNIQUE_ID
            proposalId.text = data.SW_UNIQUE_PROPOSAL_ID
            signaturesRequired.text = "${favorVotes.size}/${data.SW_SIGNATURES_REQUIRED}"
            transferReceiver.text = data.SW_TRANSFER_FUNDS_TARGET_SERIALIZED
            transferAmount.text = Coin.valueOf(data.SW_TRANSFER_FUNDS_AMOUNT).toFriendlyString()
            balance.text = Coin.valueOf(previousMultiSigOutput.nValue).toFriendlyString()
        } else if (block.type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
            val data = SWSignatureAskTransactionData(block.transaction).getData()
            // Get favor votes
            val favorVotes = ArrayList(context.getCoinCommunity().fetchProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID)).map { it.SW_BITCOIN_PK }
            // Get against votes
            val negativeVotes = ArrayList(context.getCoinCommunity().fetchNegativeProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID)).map { it.SW_BITCOIN_PK }

            // Check if I voted
            val myPublicBitcoinKey = walletManager.protocolECKey().publicKeyAsHex
            if (favorVotes.contains(myPublicBitcoinKey) || negativeVotes.contains(myPublicBitcoinKey)) {
                votedButton.visibility = View.VISIBLE
            }

            // If the proposal can't be met anymore, draw a red border
            if (!context.getCoinCommunity().canWinJoinRequest(data)) {
                view.setBackgroundResource(R.drawable.border)
            }

            about.text = "Join request"
            createdAt.text = formatter.format(block.timestamp)
            daoId.text = data.SW_UNIQUE_ID
            proposalId.text = data.SW_UNIQUE_PROPOSAL_ID
            signaturesRequired.text = "${favorVotes.size}/${data.SW_SIGNATURES_REQUIRED}"
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
