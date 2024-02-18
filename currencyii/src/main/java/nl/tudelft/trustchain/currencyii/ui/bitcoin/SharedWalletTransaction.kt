package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.databinding.FragmentSharedWalletTransactionBinding
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWResponseSignatureBlockTD
import nl.tudelft.trustchain.currencyii.sharedWallet.SWTransferFundsAskTransactionData
import nl.tudelft.trustchain.currencyii.sharedWallet.SWUtil
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import nl.tudelft.trustchain.currencyii.util.taproot.CTransaction
import org.bitcoinj.core.Coin
import java.lang.NumberFormatException

/**
 * A simple [Fragment] subclass.
 * Use the [SharedWalletTransaction.newInstance] factory method to
 * create an instance of this fragment.
 */
class SharedWalletTransaction : BaseFragment(R.layout.fragment_shared_wallet_transaction) {

    private var _binding: FragmentSharedWalletTransactionBinding? = null
    private val binding get() = _binding!!

    private var blockHash: ByteArray? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSharedWalletTransactionBinding.inflate(inflater, container, false)
        val view = binding.root
        // Inflate the layout for this fragment
        val args = SharedWalletTransactionArgs.fromBundle(requireArguments())

        blockHash = args.trustChainBlockHash.hexToBytes()

        binding.usersProposal.text = "${args.users} user(s) in this shared wallet"
        binding.publicKeyProposal.text = "Wallet ID: ${args.publicKey}"
        binding.entranceFeeProposal.text =
            "Entrance fee: ${Coin.valueOf(args.entranceFee).toFriendlyString()}"
        binding.votingThresholdProposal.text = "Voting threshold: ${args.votingThreshold}%"
        binding.daoBalance.text = "Balance: ${getBalance().toFriendlyString()}"

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.button.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    transferFundsClicked()
                }
            }
        }
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

        val previousTransaction =
            CTransaction().deserialize(walletData.SW_TRANSACTION_SERIALIZED.hexToBytes())
        return Coin.valueOf(previousTransaction.vout.filter { it.scriptPubKey.size == 35 }[0].nValue)
    }

    private fun transferFundsClicked() {
        if (!validateTransferInput()) {
            activity?.runOnUiThread {
                binding.alertView.text =
                    "Failed: Bitcoin PK should be a string, minimal satoshi amount: ${SWUtil.MINIMAL_TRANSACTION_AMOUNT}"
            }
            return
        }
        val bitcoinPublicKey = binding.inputBitcoinPublicKey.text.toString()
        val satoshiTransferAmount = binding.inputSatoshiAmount.text.toString().toLong()

        val swJoinBlock: TrustChainBlock =
            getCoinCommunity().fetchLatestSharedWalletBlock(blockHash!!)
                ?: throw IllegalStateException("Shared Wallet not found given the hash: ${blockHash!!}")
        val walletData = SWJoinBlockTransactionData(swJoinBlock.transaction).getData()

        if (getBalance().minus(Coin.valueOf(satoshiTransferAmount)).isNegative) {
            activity?.runOnUiThread {
                binding.alertView.text =
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
                binding.alertView.text = t.message ?: "Unexpected error occurred. Try again"
            }
            return
        }
        val context = requireContext()
        val activityRequired = requireActivity()

        val responses = collectResponses(transferFundsData)
        try {
            getCoinCommunity().transferFunds(
                walletData,
                swJoinBlock.transaction,
                transferFundsData.getData(),
                responses,
                bitcoinPublicKey,
                satoshiTransferAmount,
                context,
                activityRequired
            )
            activity?.runOnUiThread {
                binding.alertView.text = "Funds transfered!"
            }
        } catch (t: Throwable) {
            Log.i("Coin", "Transferring funds failed. ${t.message ?: "No further information"}.")
            resetWalletInitializationValues()
            activity?.runOnUiThread {
                binding.alertView.text = t.message ?: "Unexpected error occurred. Try again"
            }
        }
    }

    private fun collectResponses(data: SWTransferFundsAskTransactionData): List<SWResponseSignatureBlockTD> {
        var responses: List<SWResponseSignatureBlockTD>? = null
        activity?.runOnUiThread {
            binding.alertView.text = "Loading... This might take some time."
        }

        while (responses == null) {
            responses =
                checkSufficientResponses(data, data.getData().SW_SIGNATURES_REQUIRED)
            if (responses == null) {
                Thread.sleep(1_000)
            }
        }

        return responses
    }

    private fun checkSufficientResponses(
        data: SWTransferFundsAskTransactionData,
        requiredSignatures: Int
    ): List<SWResponseSignatureBlockTD>? {
        val blockData = data.getData()
        val responses =
            getCoinCommunity().fetchProposalResponses(
                blockData.SW_UNIQUE_ID,
                blockData.SW_UNIQUE_PROPOSAL_ID
            )
        Log.i(
            "Coin",
            "Responses for ${blockData.SW_UNIQUE_ID}.${blockData.SW_UNIQUE_PROPOSAL_ID}: ${responses.size}/$requiredSignatures"
        )

        activity?.runOnUiThread {
            binding.alertView.text =
                "Collecting signatures: ${responses.size}/${blockData.SW_SIGNATURES_REQUIRED} received!"
        }

        if (responses.size >= requiredSignatures) {
            return responses
        }
        return null
    }

    private fun resetWalletInitializationValues() {
        activity?.runOnUiThread {
            binding.alertView.text = ""
        }
    }

    private fun validateTransferInput(): Boolean {
        val bitcoinPublicKey = binding.inputBitcoinPublicKey.text.toString()
        val satoshiTransferAmount: Long = try {
            binding.inputSatoshiAmount.text.toString().toLong()
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
