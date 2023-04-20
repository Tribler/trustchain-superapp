package nl.tudelft.trustchain.offlinedigitaleuro.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinedigitaleuro.src.Wallet
import org.json.JSONObject

import nl.tudelft.trustchain.offlinedigitaleuro.payloads.TransferQR
import nl.tudelft.trustchain.offlinedigitaleuro.src.Token
import nl.tudelft.trustchain.offlinedigitaleuro.R
import nl.tudelft.trustchain.offlinedigitaleuro.databinding.SendMoneyFragmentBinding

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

        val tokensToSend: MutableSet<Token> = mutableSetOf();

        runBlocking(Dispatchers.IO) {
            val coinCounts = JSONObject(requireArguments().getString(ARG_DATA)!!)

            tokensToSend.addAll(loadTokensToSend(
                coinCounts.getInt(SendAmountFragment.ARG_1EURO_COUNT),
                coinCounts.getInt(SendAmountFragment.ARG_2EURO_COUNT),
                coinCounts.getInt(SendAmountFragment.ARG_5EURO_COUNT),
                coinCounts.getInt(SendAmountFragment.ARG_10EURO_COUNT)
            ));

        }

        lifecycleScope.launch {
            val transferJson = TransferQR.createJson(Wallet().privateKey, tokensToSend);

            binding.txtSendData.text = transferJson.toString()

            val bitmap = withContext(Dispatchers.Default) {
                qrCodeUtils.createQR(transferJson.toString())
            }

            binding.qrImageView.setImageBitmap(bitmap)
        }

        binding.btnContinue.setOnClickListener {
            //remove tokens that were transferred from database
            runBlocking(Dispatchers.IO) {
                for (token in tokensToSend) {
                    db.tokensDao().deleteToken(
                            token.id.toHex()
                    );
                    Log.d("TOKEN", "delete token ${token.id.toHex()}")
                }

                val allTokens = db.tokensDao().getAllTokens()
                for (token in allTokens) {
                    Log.i("db_token", "Token_ID: ${token.token_id} \t Token value: ${token.token_value}")
                }
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

    companion object {
        const val ARG_DATA = "data"
    }
}
