package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.databinding.ProposalRowDataBinding
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
    override fun getView(
        p0: Int,
        p1: View?,
        p2: ViewGroup?
    ): View {
        val binding =
            if (p1 != null) {
                ProposalRowDataBinding.bind(p1)
            } else {
                ProposalRowDataBinding.inflate(context.layoutInflater)
            }
        val view = binding.root

        val block = items[p0]
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm")

        val about = binding.aboutTv
        val createdAt = binding.timestampTv
        val daoId = binding.daoIdTv
        val proposalId = binding.proposalIdTv
        val signaturesRequired = binding.signaturesRequiredTv
        val transferReceiver = binding.transferTargetTv
        val transferAmount = binding.transferAmountTv
        val votedButton = binding.votedButton
        val balance = binding.daoBalanceTv
        val balanceText = binding.balanceTv

        val walletManager = WalletManagerAndroid.getInstance()

        if (block.type == CoinCommunity.TRANSFER_FUNDS_ASK_BLOCK) {
            val data = SWTransferFundsAskTransactionData(block.transaction).getData()
            // Get favor votes
            val favorVotes =
                ArrayList(
                    context.getCoinCommunity()
                        .fetchProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID)
                ).map { it.SW_BITCOIN_PK }
            // Get against votes
            val negativeVotes =
                ArrayList(
                    context.getCoinCommunity().fetchNegativeProposalResponses(
                        data.SW_UNIQUE_ID,
                        data.SW_UNIQUE_PROPOSAL_ID
                    )
                ).map { it.SW_BITCOIN_PK }

            // Check if I voted
            val myPublicBitcoinKey = walletManager.protocolECKey().publicKeyAsHex
            if (favorVotes.contains(myPublicBitcoinKey) ||
                negativeVotes.contains(
                    myPublicBitcoinKey
                )
            ) {
                votedButton.visibility = View.VISIBLE
            }

            // If the proposal can't be met anymore, draw a red border
            if (!context.getCoinCommunity().canWinTransferRequest(data)) {
                view.setBackgroundResource(R.drawable.border)
            }

            val previousTransaction =
                CTransaction().deserialize(
                    data.SW_TRANSACTION_SERIALIZED.hexToBytes()
                )

            val previousMultiSigOutput =
                previousTransaction.vout.filter { it.scriptPubKey.size == 35 }[0]

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
            val favorVotes =
                ArrayList(
                    context.getCoinCommunity()
                        .fetchProposalResponses(data.SW_UNIQUE_ID, data.SW_UNIQUE_PROPOSAL_ID)
                ).map { it.SW_BITCOIN_PK }
            // Get against votes
            val negativeVotes =
                ArrayList(
                    context.getCoinCommunity().fetchNegativeProposalResponses(
                        data.SW_UNIQUE_ID,
                        data.SW_UNIQUE_PROPOSAL_ID
                    )
                ).map { it.SW_BITCOIN_PK }

            // Check if I voted
            val myPublicBitcoinKey = walletManager.protocolECKey().publicKeyAsHex
            if (favorVotes.contains(myPublicBitcoinKey) ||
                negativeVotes.contains(
                    myPublicBitcoinKey
                )
            ) {
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
        val binding = ProposalRowDataBinding.bind(view)

        val transferReceiverLabel = binding.transferTarget
        val transferAmountLabel = binding.transferAmount
        val transferReceiver = binding.transferTargetTv
        val transferAmount = binding.transferAmountTv

        try {
            transferReceiverLabel.visibility = View.GONE
            transferAmountLabel.visibility = View.GONE
            transferReceiver.visibility = View.GONE
            transferAmount.visibility = View.GONE
        } catch (_: Exception) {
            // View no longer visible.
        }
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
