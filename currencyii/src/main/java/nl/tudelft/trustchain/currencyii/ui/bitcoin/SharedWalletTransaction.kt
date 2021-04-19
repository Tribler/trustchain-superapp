package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_shared_wallet_transaction.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWUtil
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import java.lang.NumberFormatException

/**
 * A simple [Fragment] subclass.
 * Use the [SharedWalletTransaction.newInstance] factory method to
 * create an instance of this fragment.
 */
class SharedWalletTransaction : BaseFragment(R.layout.fragment_shared_wallet_transaction) {
    private var blockHash: ByteArray? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        button.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    transferFundsClicked()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragment =
            inflater.inflate(R.layout.fragment_shared_wallet_transaction, container, false)
        val args = SharedWalletTransactionArgs.fromBundle(requireArguments())

        blockHash = args.trustChainBlockHash.hexToBytes()

        fragment.findViewById<TextView>(R.id.users_proposal).text =
            "${args.users} user(s) in this shared wallet"
        fragment.findViewById<TextView>(R.id.public_key_proposal).text =
            "Wallet ID: ${args.publicKey}"
        fragment.findViewById<TextView>(R.id.entrance_fee_proposal).text =
            "Entrance fee: ${Coin.valueOf(args.entranceFee).toFriendlyString()}"
        fragment.findViewById<TextView>(R.id.voting_threshold_proposal).text =
            "Voting threshold: ${args.votingThreshold}%"
        fragment.findViewById<TextView>(R.id.dao_balance).text =
            "Balance: ${getBalance().toFriendlyString()}"

        return fragment
    }

    /**
     * Get the balance of the current wallet
     * @return current balance
     */
    private fun getBalance(): Coin {
        val swJoinBlock: TrustChainBlock =
            getCoinCommunity().fetchLatestSharedWalletBlock(blockHash!!)
                ?: throw IllegalStateException("Shared Wallet not found given the hash: ${blockHash!!}")
        val walletData = SWJoinBlockTransactionData(swJoinBlock.transaction).getData()

        val walletManager = WalletManagerAndroid.getInstance()
        val previousTransaction = Transaction(
            walletManager.params,
            walletData.SW_TRANSACTION_SERIALIZED.hexToBytes()
        )
        return walletManager.getMuSigOutput(previousTransaction).value
    }

    private fun transferFundsClicked() {
        if (!validateTransferInput()) {
            activity?.runOnUiThread {
                alert_view.text =
                    "Failed: Bitcoin PK should be a string, minimal satoshi amount: ${SWUtil.MINIMAL_TRANSACTION_AMOUNT}"
            }
            return
        }
        val bitcoinPublicKey = input_bitcoin_public_key.text.toString()
        val satoshiTransferAmount = input_satoshi_amount.text.toString().toLong()

        val swJoinBlock: TrustChainBlock =
            getCoinCommunity().fetchLatestSharedWalletBlock(blockHash!!)
                ?: throw IllegalStateException("Shared Wallet not found given the hash: ${blockHash!!}")
//        val walletData = SWJoinBlockTransactionData(swJoinBlock.transaction).getData()

        if (getBalance().minus(Coin.valueOf(satoshiTransferAmount)).isNegative) {
            activity?.runOnUiThread {
                alert_view.text =
                    "Failed: Transfer amount should be smaller than the current balance"
            }
            return
        }

        val transferFundsData = try {
            getCoinCommunity().proposeTransferFunds(
                swJoinBlock,
                bitcoinPublicKey,
                satoshiTransferAmount
            )
        } catch (t: Throwable) {
            Log.i(
                "Coin",
                "Proposing transfer funds failed. ${t.message ?: "No further information"}."
            )
            activity?.runOnUiThread {
                alert_view.text = t.message ?: "Unexpected error occurred. Try again"
            }
            return
        }

        val signatures = collectSignatures(transferFundsData)
        try {
            getCoinCommunity().transferFunds(
                swJoinBlock.transaction,
                transferFundsData.getData(),
                signatures,
                bitcoinPublicKey,
                satoshiTransferAmount//,
                //requireContext()
            )
            activity?.runOnUiThread {
                alert_view.text = "Funds transfered!"
            }
        } catch (t: Throwable) {
            Log.i("Coin", "Transferring funds failed. ${t.message ?: "No further information"}.")
            resetWalletInitializationValues()
            activity?.runOnUiThread {
                alert_view.text = t.message ?: "Unexpected error occurred. Try again"
            }
        }
    }

    private fun updateAlertLabel(progress: Double) {
        Log.i("Coin", "Coin: broadcast of transfer funds transaction progress: $progress.")

        activity?.runOnUiThread {
            if (progress >= 1) {
                alert_view?.text = "Transfer funds progress: completed!"
            } else {
                val progressString = "%.0f".format(progress * 100)
                alert_view.text = "Transfer funds progress: $progressString%..."
            }
        }
    }

    private fun collectSignatures(data: SWTransferFundsAskTransactionData): List<String> {
        var signatures: List<String>? = null
        activity?.runOnUiThread {
            alert_view.text = "Loading... This might take some time."
        }

        while (signatures == null) {
            signatures =
                checkSufficientWalletSignatures(data, data.getData().SW_SIGNATURES_REQUIRED)
            if (signatures == null) {
                Thread.sleep(1_000)
            }
        }

        return signatures
    }

    private fun checkSufficientWalletSignatures(
        data: SWTransferFundsAskTransactionData,
        requiredSignatures: Int
    ): List<String>? {
        val blockData = data.getData()
        val signatures =
            getCoinCommunity().fetchProposalSignatures(
                blockData.SW_UNIQUE_ID,
                blockData.SW_UNIQUE_PROPOSAL_ID
            )
        Log.i(
            "Coin",
            "Signatures for ${blockData.SW_UNIQUE_ID}.${blockData.SW_UNIQUE_PROPOSAL_ID}: ${signatures.size}/$requiredSignatures"
        )

        activity?.runOnUiThread {
            alert_view?.text =
                "Collecting signatures: ${signatures.size}/${blockData.SW_SIGNATURES_REQUIRED} received!"
        }

        if (signatures.size >= requiredSignatures) {
            return signatures
        }
        return null
    }

    private fun resetWalletInitializationValues() {
        activity?.runOnUiThread {
            alert_view.text = ""
        }
    }

    private fun validateTransferInput(): Boolean {
        val bitcoinPublicKey = input_bitcoin_public_key.text.toString()
        val satoshiTransferAmount: Long = try {
            input_satoshi_amount.text.toString().toLong()
        } catch (e: NumberFormatException) {
            0
        }
        return bitcoinPublicKey != "" && satoshiTransferAmount >= SWUtil.MINIMAL_TRANSACTION_AMOUNT && blockHash != null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment SharedWalletTransaction.
         */
        @JvmStatic
        fun newInstance() = SharedWalletTransaction()
    }
}
