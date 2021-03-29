package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.CoinUtil
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.SWSignatureAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutput
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
        val fee = view.findViewById<TextView>(R.id.transfer_fee_tv)
        val balance = view.findViewById<TextView>(R.id.dao_balance_tv)
        val balanceText = view.findViewById<TextView>(R.id.balance_tv)
        val feeText = view.findViewById<TextView>(R.id.fee_tv)

        val walletManager = WalletManagerAndroid.getInstance()

        if (block.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
            val data = SWTransferFundsAskTransactionData(block.transaction).getData()
            // Get favor votes
            val signatures = ArrayList(context.getCoinCommunity().fetchProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
            // Get against votes
            val negativeSignatures = ArrayList(context.getCoinCommunity().fetchNegativeProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

            // Check if I voted
            val mySignatureSerialized = context.getCoinCommunity().getMySignatureTransaction(data).encodeToDER().toHex()
            if (signatures.contains(mySignatureSerialized) || negativeSignatures.contains(mySignatureSerialized)) {
                votedButton.visibility = View.VISIBLE
            }

            // If the proposal can't be met anymore, draw a red border
            if (!context.getCoinCommunity().canWinTransferRequest(data)) {
                view.setBackgroundResource(R.drawable.border)
            }

            val previousTransaction = Transaction(
                walletManager.params,
                data.SW_TRANSACTION_SERIALIZED.hexToBytes()
            )

            // Calculate fee and set the change output corresponding to calculated fee
            val calculatedFeeValue = CoinUtil.calculateEstimatedTransactionFee(
                previousTransaction,
                walletManager.params,
                CoinUtil.TxPriority.LOW_PRIORITY
            )
            val previousMultiSigOutput: TransactionOutput =
                walletManager.getMultiSigOutput(previousTransaction).unsignedOutput
            // Make sure that the fee does not exceed the amount of funds available
            val calculatedFee =
                Coin.valueOf(calculatedFeeValue.coerceAtMost((previousMultiSigOutput.value - Coin.valueOf(data.SW_TRANSFER_FUNDS_AMOUNT)).value))
            balanceText.visibility = View.VISIBLE
            balance.visibility = View.VISIBLE
            feeText.visibility = View.VISIBLE
            fee.visibility = View.VISIBLE

            about.text = "Transfer funds request"
            createdAt.text = formatter.format(block.timestamp)
            daoId.text = data.SW_UNIQUE_ID
            proposalId.text = data.SW_UNIQUE_PROPOSAL_ID
            signaturesRequired.text = "${signatures.size}/${data.SW_SIGNATURES_REQUIRED}"
            transferReceiver.text = data.SW_TRANSFER_FUNDS_TARGET_SERIALIZED
            transferAmount.text = Coin.valueOf(data.SW_TRANSFER_FUNDS_AMOUNT).toFriendlyString()
            balance.text = walletManager.getMultiSigOutput(previousTransaction).value.toFriendlyString()
            fee.text = calculatedFee.toFriendlyString()
        } else if (block.type == CoinCommunity.SIGNATURE_ASK_BLOCK) {
            val data = SWSignatureAskTransactionData(block.transaction).getData()
            // Get favor votes
            val signatures = ArrayList(context.getCoinCommunity().fetchProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))
            // Get against votes
            val negativeSignatures = ArrayList(context.getCoinCommunity().fetchNegativeProposalSignatures(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID))

            // Check if I voted
            val mySignatureSerialized = context.getCoinCommunity().getMySignatureJoinRequest(data).encodeToDER().toHex()
            if (signatures.contains(mySignatureSerialized) || negativeSignatures.contains(mySignatureSerialized)) {
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
            signaturesRequired.text = "${signatures.size}/${data.SW_SIGNATURES_REQUIRED}"
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
