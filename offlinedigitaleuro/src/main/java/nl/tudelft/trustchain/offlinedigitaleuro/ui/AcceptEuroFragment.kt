package nl.tudelft.trustchain.offlinedigitaleuro.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinedigitaleuro.R
import nl.tudelft.trustchain.offlinedigitaleuro.databinding.AcceptEuroFragmentBinding
import nl.tudelft.trustchain.offlinedigitaleuro.payloads.TransferQR
import nl.tudelft.trustchain.offlinedigitaleuro.utils.TransactionUtility
import nl.tudelft.trustchain.offlinedigitaleuro.utils.WebOfTrustUtility
import org.json.JSONObject

class AcceptEuroFragment : OfflineDigitalEuroBaseFragment(R.layout.accept_euro_fragment) {
    private val binding by viewBinding(AcceptEuroFragmentBinding::bind)

    private var maybeTransaction: TransferQR? = null

    private var maybePrevOwner: PublicKey? = null

    private var maybeTrustScore: Int? = null

    private var doubleSpendExists: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()

        // get the transaction from the passed data
        setTransaction(JSONObject(requireArguments().getString(ARG_QR)!!))

        // get the previous owner
        setPrevOwner()

        // check tokens for double spending, if double spent then store the user and destroy the transaction
        checkForDuplicateTokens()

        setTrustScoreAndMessage()
    }

    private fun setupListeners() {
        binding.btnAccept.setOnClickListener {
            if (maybeTransaction == null || maybePrevOwner == null) {
                val prevMsg: String = binding.txtError.text.toString()
                val newErrMsg = "Error: transaction is not good. Reject instead"
                // to display the error message only once
                if (!prevMsg.contains(newErrMsg)) {
                    val newMsg = "$prevMsg\n$newErrMsg"
                    binding.txtError.text = newMsg
                }
                return@setOnClickListener
            }

            // store the tokens and insert the user in the web of trust db
            val transaction: TransferQR = maybeTransaction!!
            val prevOwner: PublicKey = maybePrevOwner!!

            // add the tokens to us
            val (result, errMsgReceiveTrans) = TransactionUtility.receiveTransaction(transaction, db, getTrustChainCommunity().myPeer.publicKey)
            if (!result) {
                maybeTransaction = null
                maybePrevOwner = null

                binding.txtError.text = errMsgReceiveTrans

                return@setOnClickListener
            }

            // add the user or update its trust
            if (maybeTrustScore == null) {
                val (resultWOT, errMsgWOT) = WebOfTrustUtility.addNewPeer(prevOwner, WebOfTrustUtility.TRUST_INCREASE, db)
                if (!resultWOT) {
                    Toast.makeText(requireContext(), errMsgWOT, Toast.LENGTH_LONG).show()
                }
            } else {
                val (resultWOT, errMsgWOT) = WebOfTrustUtility.updateUserTrust(prevOwner, +WebOfTrustUtility.TRUST_INCREASE, db)
                if (!resultWOT) {
                    Toast.makeText(requireContext(), errMsgWOT, Toast.LENGTH_LONG).show()
                }
            }

            findNavController().navigate(R.id.action_acceptMoneyFragment_to_transferFragment)
        }

        binding.btnRefuse.setOnClickListener {
            findNavController().navigate(R.id.action_acceptMoneyFragment_to_transferFragment)
        }
    }

    private fun setTransaction(transactionArg: JSONObject) {
        val (maybeTransaction, errMsgTransfer) = TransferQR.fromJson(transactionArg)

        if (maybeTransaction == null) {
            binding.txtError.text = errMsgTransfer
            return
        }

        this.maybeTransaction = maybeTransaction

        // get the amount to be transferred
        val amountMsg = "€ ${maybeTransaction.getValue()}"
        binding.txtAmount.text = amountMsg
    }

    private fun setPrevOwner() {
        if (maybeTransaction == null) {
            return
        }

        val tqr: TransferQR = maybeTransaction!!
        var titleMsg = "Transfer from: "

        val (maybePrevOwner, errMsgGetPrevOwner) = tqr.getPreviousOwner()
        if (maybePrevOwner == null) {
            val prevMsg = binding.txtError.text.toString()
            val newMsg = "$prevMsg\n$errMsgGetPrevOwner"
            binding.txtError.text = newMsg

            titleMsg += "Unknown"
        } else {
            this.maybePrevOwner = maybePrevOwner
            titleMsg += maybePrevOwner.keyToBin().toHex()
        }

        binding.txtTitle.text = titleMsg
    }

    //    Checks if transaction tokens are already in the DB. If true, then search for the double
    // spender and store his public key with a very small score
    private fun checkForDuplicateTokens() {
        if (maybeTransaction == null || maybePrevOwner == null) {
            return
        }

        val transaction: TransferQR = maybeTransaction!!
        val prevOwner: PublicKey = maybePrevOwner!!

        val (maybeDuplicates, errMsgFindDup) = TransactionUtility.getDuplicateTokens(transaction, db)
        if (maybeDuplicates == null) {
            maybeTransaction = null
            maybePrevOwner = null

            binding.txtError.text = errMsgFindDup
            return
        }

        val duplicates: MutableList<TransactionUtility.DuplicatedTokens> = maybeDuplicates

        if (duplicates.size == 0) {
            return
        }

        val doubleSpentErrMsg = "Warning: tokens are double spent and transaction will fail"
        binding.txtError.text = doubleSpentErrMsg
        // now try to find who double spent and store those public keys as double spenders
        val doubleSpenders: MutableMap<ByteArray, PublicKey> = mutableMapOf()
        for (duplicate in duplicates) {
            val (maybeDoubleSpender, errMsgDoubleSpender) = duplicate.getDoubleSpender()
            if (maybeDoubleSpender == null) {
                val prevMsg = binding.txtError.text
                val newMsg = "$prevMsg\n$errMsgDoubleSpender"
                binding.txtError.text = newMsg
                continue
            }
            val doubleSpender: PublicKey = maybeDoubleSpender
            doubleSpenders[doubleSpender.keyToBin()] = doubleSpender
        }

        for (ds in doubleSpenders) {
            if (ds == prevOwner) {
                val prevOwnerDoubleSpenderMsg = "WARNING: sender double spent"
                binding.trustScoreWarning.text = prevOwnerDoubleSpenderMsg
                binding.trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
            }

            val (result, errMsg) = WebOfTrustUtility.addOrUpdatePeer(ds.value, WebOfTrustUtility.TRUST_MIN, db, absolute = true)
            if (result == null) {
                // something strange happened
                val prevMsg = binding.txtError.text
                val newMsg = "$prevMsg\n$errMsg"
                binding.txtError.text = newMsg
                continue
            }
        }

        doubleSpendExists = true
        maybeTransaction = null
        maybePrevOwner = null
    }

    @SuppressLint("SetTextI18n")
    private fun setTrustScoreAndMessage() {
        if (doubleSpendExists) {
            val prevMsg = binding.trustScoreWarning.text
            val invalidTransactionMsg = "WARNING: the transaction contains double spending, so it is invalid"
            val newMsg = if (!prevMsg.equals("")) {
                "$prevMsg\n$invalidTransactionMsg"
            } else {
                invalidTransactionMsg
            }
            binding.trustScoreWarning.text = newMsg
            binding.trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
            return
        }

        if (maybeTransaction == null || maybePrevOwner == null) {
            val invalidTransactionMsg = "WARNING: the transaction is invalid"
            binding.trustScoreWarning.text = invalidTransactionMsg
            binding.trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
            return
        }

        val prevOwner: PublicKey = maybePrevOwner!!

        maybeTrustScore = WebOfTrustUtility.getTrustOfPeer(prevOwner, db)

        if (maybeTrustScore != null) {
            val ownerTrustScore: Int = maybeTrustScore!!
            if (ownerTrustScore >= 70) {
                binding.trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_high, ownerTrustScore)
                binding.trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.android_green))
            } else if (ownerTrustScore >= 30) {
                binding.trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_average, ownerTrustScore)
                binding.trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue))
            } else if (ownerTrustScore >= 0) {
                binding.trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_neutral, ownerTrustScore)
                binding.trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.metallic_gold))
            } else if (ownerTrustScore >= -20) {
                binding.trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_low, ownerTrustScore)
                binding.trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
            } else {
                binding.trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_double_spender, ownerTrustScore)
                binding.trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
            }
        } else {
            binding.trustScoreWarning.text = getString(R.string.send_money_trustscore_warning_no_score)
            binding.trustScoreWarning.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.metallic_gold))
            binding.trustScoreWarning.visibility = View.VISIBLE
        }
    }

    companion object {
        const val ARG_QR = "qr"
    }
}
