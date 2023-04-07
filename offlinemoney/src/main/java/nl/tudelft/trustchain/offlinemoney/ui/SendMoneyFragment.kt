package nl.tudelft.trustchain.offlinemoney.ui

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
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.SendMoneyFragmentBinding
import nl.tudelft.trustchain.offlinemoney.src.Wallet
import org.json.JSONObject

import nl.tudelft.trustchain.offlinemoney.payloads.TransferQR
import nl.tudelft.trustchain.offlinemoney.src.Token

class SendMoneyFragment : OfflineMoneyBaseFragment(R.layout.send_money_fragment)  {
    private val binding by viewBinding(SendMoneyFragmentBinding::bind)

    // load just enough tokens to transfer
    private fun loadTokensToSend(oneCount: Int, twoCount: Int, fiveCount: Int): MutableSet<Token> {
        val ret: MutableSet<Token> = mutableSetOf();

        ret.addAll(
            dbTokens2Tokens(db.tokensDao().getAllTokensOfValue(1.0), oneCount)
        );
        ret.addAll(
            dbTokens2Tokens(db.tokensDao().getAllTokensOfValue(2.0), twoCount)
        );
        ret.addAll(
            dbTokens2Tokens(db.tokensDao().getAllTokensOfValue(5.0), fiveCount)
        );

        return ret;
    }

    private fun dbTokens2Tokens(dbTokens: Array<nl.tudelft.trustchain.offlinemoney.db.Token>, count: Int): MutableList<Token> {
        var ret: MutableList<Token> = mutableListOf()

        for (i in 0 until count) {
            var dbToken = dbTokens[i];
            var setOfToken = Token.deserialize(dbToken.token_data);
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
            val coinCounts: JSONObject = JSONObject(requireArguments().getString(ARG_DATA)!!)

            tokensToSend.addAll(loadTokensToSend(
                coinCounts.getInt(SendAmountFragment.ARG_1EURO_COUNT),
                coinCounts.getInt(SendAmountFragment.ARG_2EURO_COUNT),
                coinCounts.getInt(SendAmountFragment.ARG_5EURO_COUNT)
            ));

        }

        lifecycleScope.launch {
            val transferJson = TransferQR.createJson(Wallet().privateKey, tokensToSend);

            binding.txtSendData.text = transferJson.toString();

            val bitmap = withContext(Dispatchers.Default) {
                qrCodeUtils.createQR(transferJson.toString())
            }

            binding.qrImageView.setImageBitmap(bitmap)
        }

        binding.btnContinue.setOnClickListener {
//            remove tokens that were transferred from database
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
