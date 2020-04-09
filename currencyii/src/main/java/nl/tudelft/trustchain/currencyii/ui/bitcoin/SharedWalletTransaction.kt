package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_shared_wallet_transaction.*
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.WalletManager
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

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
            transferFundsClicked()
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
        fragment.findViewById<TextView>(R.id.public_key_proposal).text = args.publicKey
        fragment.findViewById<TextView>(R.id.voting_threshold_proposal).text =
            "${args.votingThreshold} %"

        fragment.findViewById<TextView>(R.id.entrance_fee_proposal).text = "${args.entranceFee} BTC"
        fragment.findViewById<TextView>(R.id.users_proposal).text =
            "${args.users} user(s) in this shared wallet"

        blockHash = args.trustChainBlockHash.hexToBytes()

        return fragment
    }

    private fun transferFundsClicked() {
        Log.i("Coin", "Transfer funds clicked !!!!")
        if (validateCreationInput()) {
            val bitcoinPublicKey = input_bitcoin_public_key.text.toString()
            val satoshiTransferAmount = input_satoshi_amount.text.toString().toLong()

            val transferFundsData = getCoinCommunity().askForTransferFundsSignatures(
                blockHash!!,
                bitcoinPublicKey,
                satoshiTransferAmount
            )

            lifecycleScope.launchWhenStarted {
                fetchCurrentTransactionStatusLoop(
                    transferFundsData,
                    bitcoinPublicKey,
                    satoshiTransferAmount
                )
            }
        } else {
            alert_view.text = "Failed: Bitcoin PK should be a string, minimal satoshi amount: 5000"
        }
    }

    private fun fetchCurrentTransactionStatusLoop(
        data: SWTransferFundsAskTransactionData,
        bitcoinPublicKey: String,
        satoshiAmount: Long
    ) {

        var finished = false
        alert_view.text = "Loading... This might take some time."

        while (!finished) {
            val signatures =
                checkSufficientWalletSignatures(data, data.getData().SW_SIGNATURES_REQUIRED)
            if (signatures == null) {
                Thread.sleep(1_000)
                continue
            }

            val transactionData = getCoinCommunity().transferFunds(
                signatures,
                blockHash!!,
                bitcoinPublicKey,
                satoshiAmount
            )

            fetchCurrentSharedWalletStatusLoop(transactionData, data)

            finished = true
        }

        resetWalletInitializationValues()
    }

    private fun fetchCurrentSharedWalletStatusLoop(
        transactionData: WalletManager.TransactionPackage,
        data: SWTransferFundsAskTransactionData
    ) {
        var finished = false

        while (!finished) {
            val serializedTransaction =
                getCoinCommunity().fetchBitcoinTransaction(transactionData.transactionId)
            if (serializedTransaction == null) {
                Thread.sleep(1_000)
                continue
            }

            getCoinCommunity().postTransactionSucceededOnTrustChain(data, serializedTransaction)
            finished = true
        }

        resetWalletInitializationValues()
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
        if (signatures.size >= requiredSignatures) {
            return signatures
        }
        return null
    }

    private fun resetWalletInitializationValues() {
        alert_view.text = ""
    }

    private fun validateCreationInput(): Boolean {
        val bitcoinPublicKey = input_bitcoin_public_key.text.toString()
        val satoshiTransferAmount = input_satoshi_amount.text.toString().toLong()
        return bitcoinPublicKey != null &&
            satoshiTransferAmount >= 5000 &&
            blockHash != null
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
