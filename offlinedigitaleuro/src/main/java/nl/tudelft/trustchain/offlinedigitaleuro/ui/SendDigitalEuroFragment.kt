package nl.tudelft.trustchain.offlinedigitaleuro.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinedigitaleuro.src.Wallet
import org.json.JSONObject

import nl.tudelft.trustchain.offlinedigitaleuro.payloads.TransferQR
import nl.tudelft.trustchain.offlinedigitaleuro.src.Token
import nl.tudelft.trustchain.offlinedigitaleuro.R
import nl.tudelft.trustchain.offlinedigitaleuro.databinding.SendMoneyFragmentBinding
import nl.tudelft.trustchain.offlinedigitaleuro.utils.TransactionUtility

class SendDigitalEuroFragment : OfflineDigitalEuroBaseFragment(R.layout.send_money_fragment)  {
    private val binding by viewBinding(SendMoneyFragmentBinding::bind)

    // load just enough tokens to transfer
    private fun loadTokensToSend(oneCount: Int, twoCount: Int, fiveCount: Int, tenCount: Int): MutableSet<Token> {
        val ret: MutableSet<Token> = mutableSetOf()

        ret.addAll(
            dbTokens2Tokens(db.tokensDao().getAllTokensOfValue(1.0), oneCount)
        )
        ret.addAll(
            dbTokens2Tokens(db.tokensDao().getAllTokensOfValue(2.0), twoCount)
        )
        ret.addAll(
            dbTokens2Tokens(db.tokensDao().getAllTokensOfValue(5.0), fiveCount)
        )
        ret.addAll(
            dbTokens2Tokens(db.tokensDao().getAllTokensOfValue(10.0), tenCount)
        )

        return ret
    }

    private fun dbTokens2Tokens(dbTokens: Array<nl.tudelft.trustchain.offlinedigitaleuro.db.Token>, count: Int): MutableList<Token> {
        val ret: MutableList<Token> = mutableListOf()

        for (i in 0 until count) {
            val dbToken = dbTokens[i];
            val setOfToken = Token.deserialize(dbToken.token_data)
            for (t in setOfToken) {
                Log.i("TOKEN", "token_id ${t.id}")
                ret.add(t);
                break;
            }
        }

        return ret
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var sendTransaction: JSONObject? = null
        val coinCounts = JSONObject(requireArguments().getString(ARG_DATA)!!)

        runBlocking {
            sendTransaction = tryToShowQR(coinCounts)
        }

        binding.btnContinue.setOnClickListener {
            //remove tokens that were transferred from database
            if (!tryRemoveTokensSent(sendTransaction)) {
                return@setOnClickListener
            }

            findNavController().navigate(R.id.action_sendMoneyFragment_to_transferFragment)
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_sendMoneyFragment_to_transferFragment);
        }
    }

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    @SuppressLint("SetTextI18n")
    private suspend fun tryToShowQR(coinsRequested: JSONObject) : JSONObject? {
        val result = getTransactionJson(coinsRequested)
        if (result.first == null) {
            binding.txtSendData.text = result.second
            return null
        }

        val transactionJson: JSONObject = result.first!!

        val bitmap = withContext(Dispatchers.Default) {
            qrCodeUtils.createQR(transactionJson.toString())
        }

        if (bitmap == null) {
            val errMsg = "Error: failed to generate QR, reason: possibly too much data"
            binding.txtSendData.text = errMsg
            return null
        }

        binding.txtSendData.text = transactionJson.toString()
        binding.qrImageView.setImageBitmap(bitmap)

        return result.first
    }

    private fun getTransactionJson(coinsRequested: JSONObject): Pair<JSONObject?, String> {
        val request: TransactionUtility.SendRequest = TransactionUtility.SendRequest(
            coinsRequested.getInt(SendAmountFragment.ARG_1EURO_COUNT),
            coinsRequested.getInt(SendAmountFragment.ARG_2EURO_COUNT),
            coinsRequested.getInt(SendAmountFragment.ARG_5EURO_COUNT),
            coinsRequested.getInt(SendAmountFragment.ARG_10EURO_COUNT)
        )

        val myPvk: PrivateKey = getTrustChainCommunity().myPeer.key as PrivateKey

        return TransactionUtility.getSendTransaction(request, db, myPvk)
    }

    private fun tryRemoveTokensSent(maybeSendTransaction: JSONObject?) : Boolean {
        if (maybeSendTransaction == null) {
            val prevMsg: String = binding.txtSendData.text.toString()
            val newErrMsg = "Error: transaction preparation failed, can not continue. Cancel instead"
//            to display the error message only once
            if (!prevMsg.contains(newErrMsg)) {
                val newMsg = "$prevMsg\n$newErrMsg"
                binding.txtSendData.text = newMsg
            }

            return false
        }

        val sendTransaction: JSONObject = maybeSendTransaction
        val result = TransactionUtility.completeSendTransaction(sendTransaction, db)

        if (!result.first) {
            binding.txtSendData.text = result.second
            return false
        }

        return true
    }

    companion object {
        const val ARG_DATA = "data"
    }
}
