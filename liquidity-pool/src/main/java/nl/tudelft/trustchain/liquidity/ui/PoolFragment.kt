package nl.tudelft.trustchain.liquidity.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.android.synthetic.main.fragment_pool.*
import kotlinx.android.synthetic.main.fragment_pool_join.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.bitcoin.WalletService
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.data.EuroTokenWallet
import org.bitcoinj.core.*
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet

/**
 * A simple [Fragment] subclass.
 * Use the [PoolFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PoolFragment : BaseFragment(R.layout.fragment_pool) {

    lateinit var app: WalletAppKit
    lateinit var btcWallet: Wallet
    lateinit var sharedPreference: SharedPreferences
    lateinit var  euroWallet: EuroTokenWallet
    var btcTransaction: Transaction? = null
    var euroTokenTransaction: TrustChainBlock? = null
    var tradeTransaction: TrustChainBlock? = null
    var status: MutableMap<String, Status> = mutableMapOf("btc" to Status.NOTSEND, "euro" to Status.NOTSEND)
    val euroLiqWallet = "4c69624e61434c504b3ac47746bf9be225a333f244340792b28df40ace7dcf7407ce36305c7a83e34b56268aeceb137d81541ec8a75aaa17fd75e70130b4948de1a92955174536f0cc1a"
    lateinit var selectedSupplyToken: String
    val tokenPair: Map<String, String> = mutableMapOf("BTC" to "Euro Token", "Euro Token" to "BTC")
    val tradeRatio: Float = 1.0F
    var suppliedAmount: Float = 0.0F
    var demandAmount: Float = 0.0F

    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, GatewayStore.getInstance(requireContext()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")

        app = WalletService.getGlobalWallet()
        btcWallet = app.wallet()

        // create other preferences than join blocks ones? (we use transactions there too)
        sharedPreference = this.requireActivity().getSharedPreferences("transactions", Context.MODE_PRIVATE)

        euroWallet = EuroTokenWallet(transactionRepository, getIpv8().myPeer.publicKey);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        select_supply_token.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("LiquidityPool", "No token selected Error")
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSupplyToken = parent!!.selectedItem.toString()
                token_supply_text.text = selectedSupplyToken
                token_demand_text.text = tokenPair[selectedSupplyToken]
            }
        }

        traded_amount.doAfterTextChanged {
            suppliedAmount = if (it.toString() == "") 0.0F else it.toString().toFloat()
            demandAmount = tradeRatio * suppliedAmount
            token_demand_amount.text = demandAmount.toString()
        }


        lifecycleScope.launchWhenStarted {
            val btcTransferTXID = sharedPreference.getString("btcTransferTXID", null)
            if (btcTransferTXID != null) {
                btcTransaction = btcWallet.getTransaction(Sha256Hash.wrap(btcTransferTXID))
                checkBtcStatus()
            }
            val euroTransferTXID = sharedPreference.getString("euroTransferTXID", null)
            if (euroTransferTXID != null) {
                euroTokenTransaction = transactionRepository.getTransactionWithHash(euroTransferTXID.hexToBytes())
                checkEuroStatus()
            }
            val tradeTXID = sharedPreference.getString("tradeTXID", null)
            if (tradeTXID != null) {
                //tradeTransaction is the last trade that has occurred
                tradeTransaction = transactionRepository.getTransactionWithHash(tradeTXID.hexToBytes())
            }
            /**
             * if transaction is pending from before, disable button
             * TODO: Should we check other fields as well ? (UNKNOWN etc)
             */

            if (getTransactionStatus() == "Pending" || status["btc"] == Status.PENDING || status["eur"] == Status.PENDING) {
 //               convert_tokens.isEnabled = false
 //               convert_tokens.setBackgroundColor(Color.parseColor("#808080"))
            }

            convert_tokens.setOnClickListener {
                // TODO grey out button when disabled
   //             convert_tokens.isEnabled = false
   //             convert_tokens.setBackgroundColor(Color.parseColor("#808080"));
                if(selectedSupplyToken == "BTC") {
                    btcTransaction(suppliedAmount)
                }
                if(selectedSupplyToken == "Euro Token") {
                    euroTokenTransaction(suppliedAmount)
                }
            }

            while (isActive) {
                //Displaying the current confirmed token amounts
                first_token_amount.text = btcWallet.getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString()
                second_token_amount.text = TransactionRepository.prettyAmount(euroWallet.getBalance())

                checkBtcStatus()
                checkEuroStatus()
                if (status["btc"] == Status.VERIFIED || status["euro"] == Status.VERIFIED) {
                    completeTransaction()
                }

                // TODO: do we have to check other states? Unknown for example?
      /*          if (getTransactionStatus() == "Pending") {
                    convert_tokens.isEnabled = false
                    convert_tokens.setBackgroundColor(Color.parseColor("#808080"))
                } else {
                    convert_tokens.isEnabled = true
                    convert_tokens.setBackgroundColor(Color.parseColor("#2962FF"))
                }*/

                bitcoin_status.text = "BTC Status: " + getStatusString("btc")
                euro_status.text = "EUR Status: " + getStatusString("euro")
                transaction_status.text = "Transaction Status: " + getTransactionStatus()

                delay(1000)
            }
        }

    }

    private fun btcTransaction(suppliedAmount: Float) {
        val params = RegTestParams.get()

        val satoshiAmount = (suppliedAmount*100000000/2).toLong()
        val sendRequest = SendRequest.to(Address.fromString(params, "mh4GL8HU5Da5qWfJLnmijX66KkeBJftkk7"), Coin.valueOf(satoshiAmount))
        val sendRes = btcWallet.sendCoins(sendRequest)
        status["btc"] = Status.SEND
        // TODO: Make it such we can still get the result even if the user closes the fragment before the callback is made.
        Futures.addCallback(sendRes.broadcastComplete, object : FutureCallback<Transaction> {
            override fun onSuccess(result: Transaction?) {
                Log.d("BitcoinTransaction", "Transaction success")
                status["btc"] = Status.PENDING
                btcTransaction = result
                val txid = btcTransaction!!.txId.toString()
                Log.d("LiquidityPool", "TXID: $txid")
                val editor = sharedPreference.edit()
                editor.putString("btcTransferTXID", btcTransaction!!.txId.toString())
                editor.apply()
                checkBtcStatus()
            }

            override fun onFailure(t: Throwable) {
                Log.d("LiquidityPool", "Broadcasting BTC transaction failed")
            }
        }, Threading.USER_THREAD)
    }

    private fun euroTokenTransaction(suppliedAmount: Float) {
        status["euro"] = Status.SEND
        val proposalBlock: TrustChainBlock? = euroWallet.sendTokens(euroLiqWallet.hexToBytes(), (suppliedAmount/2).toLong())
        if(proposalBlock != null) {
            status["euro"] = Status.PENDING
            euroTokenTransaction = proposalBlock
            val txid = euroTokenTransaction!!.calculateHash()
            val editor = sharedPreference.edit()
            editor.putString("euroTransferTXID", txid.toHex())
            editor.apply()
            checkEuroStatus()
        }
    }

    /**
     * Coins sent, send transfer proposal and reset
     */
    private fun completeTransaction() {
        var tradeProposalBlock: TrustChainBlock? = null

        if (btcTransaction != null) {
            tradeProposalBlock = euroWallet.tradeTokens(euroLiqWallet.hexToBytes(),
                btcTransaction!!.txId.toString(), "eurotoken", euroWallet.getPublicKey().keyToBin().toHex())
        }
        if (euroTokenTransaction != null){
            tradeProposalBlock = euroWallet.tradeTokens(euroLiqWallet.hexToBytes(),
                euroTokenTransaction!!.calculateHash().toHex(), "bitcoin", btcWallet.currentReceiveAddress().toString())
        }
        if(tradeProposalBlock != null) {
            tradeTransaction = tradeProposalBlock
            val txid = tradeTransaction!!.calculateHash()
            val editor = sharedPreference.edit()
            editor.putString("tradeTXID", txid.toHex())
            editor.apply()
        }

        /**
         * Trade proposal is sent, reset transactions and prefs
         * (we only have to check tradeTransaction from now on)
         */
        btcTransaction = null
        euroTokenTransaction = null
        val editor = sharedPreference.edit()
        editor.putString("btcTransferTXID", null)
        editor.putString("euroTransferTXID", null)
        editor.apply()
    }

    private fun checkBtcStatus() {
        if (btcTransaction != null) {
            when (btcTransaction!!.confidence.confidenceType) {
                TransactionConfidence.ConfidenceType.BUILDING -> {
                    status["btc"] = if (btcTransaction!!.confidence.depthInBlocks >= 1) Status.VERIFIED else Status.PENDING
                }
                TransactionConfidence.ConfidenceType.PENDING -> {
                    status["btc"] = Status.PENDING;
                }
                TransactionConfidence.ConfidenceType.DEAD -> {
                    status["btc"] = Status.NOTSEND; convert_tokens.isEnabled = true; convert_tokens.setBackgroundColor(Color.parseColor("#2962FF"))
                }
                else -> {
                    status["btc"] = Status.UNKNOWN
                }
            }
        } else {
            status["btc"] = Status.NOTSEND
        }
    }

    private fun checkEuroStatus() {
        if (euroTokenTransaction != null) {
            if (transactionRepository.trustChainCommunity.database.getLinked(euroTokenTransaction!!) != null) {
                status["euro"] = Status.VERIFIED
            }
            else if (euroTokenTransaction!!.isProposal) {
                status["euro"] = Status.PENDING
            }
        } else {
            status["euro"] = Status.NOTSEND
        }
    }

    private fun getTransactionStatus(): String {
        if(tradeTransaction != null) {
            if (transactionRepository.trustChainCommunity.database.getLinked(tradeTransaction!!) != null) {
                return "Traded"
            }
            if (tradeTransaction!!.isProposal) {
                return "Pending"
            } else {
                return "Unknown"
            }
        } else {
            return "Not Sent"
        }
    }


    private fun getStatusString(cointype: String): String = when (status.get(cointype)) {
        Status.UNKNOWN -> "Unknown"
        Status.NOTSEND -> "Not sent"
        Status.SEND -> "Send"
        Status.PENDING -> "Pending"
        Status.VERIFIED -> "Verified"
        else -> ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pool, container, false)
    }
}
