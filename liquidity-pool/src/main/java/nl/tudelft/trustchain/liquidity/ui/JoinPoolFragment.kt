package nl.tudelft.trustchain.liquidity.ui

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
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

enum class TransactionStatus {
    UNKNOWN, SENT, NOTSENT, VERIFIED, PENDING
}

class JoinPoolFragment : BaseFragment(R.layout.fragment_pool_join) {

    lateinit var app: WalletAppKit
    lateinit var btcWallet: Wallet
    lateinit var sharedPreference: SharedPreferences
    lateinit var euroWallet: EuroTokenWallet
    lateinit var poolEuroAddress: String
    lateinit var poolBitcoinAddress: String
    var transactionStatus: MutableMap<String, TransactionStatus> = mutableMapOf("btc" to TransactionStatus.NOTSENT, "euro" to TransactionStatus.NOTSENT)
    var btcTransaction: Transaction? = null
    var euroTokenTransaction: TrustChainBlock? = null
    var joinTransaction: TrustChainBlock? = null

    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, GatewayStore.getInstance(requireContext()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app = WalletService.getGlobalWallet()
        btcWallet = app.wallet()

        sharedPreference = this.requireActivity().getSharedPreferences("transactions", Context.MODE_PRIVATE)

        euroWallet = EuroTokenWallet(transactionRepository, getIpv8().myPeer.publicKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenStarted {
            poolBitcoinAddress = sharedPreference.getString("poolBitcoinAddress", "mv7x4cqQMM8ptFgiHwJZTMTQTcTFV5rteU").toString()
            poolEuroAddress = sharedPreference.getString("poolEuroAddress", "4c69624e61434c504b3a85f8c38cec5caa72c2e23da91d0288d7f49615ee400ca82e97e925e1a2f7ae33460843aea94611d04eb535f463a9857f592a7cb072eb7e0b52f080ef078c7c5f").toString()
            val btcTXID = sharedPreference.getString("btcTXID", null)
            if (btcTXID != null) {
                btcTransaction = btcWallet.getTransaction(Sha256Hash.wrap(btcTXID))
                checkBtcStatus()
            }
            val euroTXID = sharedPreference.getString("euroTXID", null)
            if (euroTXID != null) {
                euroTokenTransaction = transactionRepository.getTransactionWithHash(euroTXID.hexToBytes())
                checkEuroStatus()
            }
            val joinTXID = sharedPreference.getString("joinTXID", null)
            if (joinTXID != null) {
                joinTransaction = transactionRepository.getTransactionWithHash(joinTXID.hexToBytes())
            }

            val params = RegTestParams.get()
            sendBtc.setOnClickListener {
                println(params)
                sendBtc.isEnabled = false
                val sendRequest = SendRequest.to(Address.fromString(params, poolBitcoinAddress), Coin.valueOf(10000000))
                val sendRes = btcWallet.sendCoins(sendRequest)
                transactionStatus["btc"] = TransactionStatus.SENT
                // TODO: Make it such we can still get the result even if the user closes the fragment before the callback is made.
                Futures.addCallback(sendRes.broadcastComplete, object : FutureCallback<Transaction> {
                    override fun onSuccess(result: Transaction?) {
                        Log.d("BitcoinTransaction", "Transaction success")
                        transactionStatus["btc"] = TransactionStatus.PENDING
                        btcTransaction = result
                        val txid = btcTransaction!!.txId.toString()
                        Log.d("LiquidityPool", "TXID: $txid")
                        val editor = sharedPreference.edit()
                        editor.putString("btcTXID", btcTransaction!!.txId.toString())
                        editor.apply()
                        checkBtcStatus()
                    }

                    override fun onFailure(t: Throwable) {
                        Log.d("LiquidityPool", "Broadcasting BTC transaction failed")
                    }
                }, Threading.USER_THREAD)
            }

            sendEuro.setOnClickListener {
                sendEuro.isEnabled = false
                transactionStatus["euro"] = TransactionStatus.SENT
                val proposalBlock: TrustChainBlock? = euroWallet.sendTokens(poolEuroAddress.hexToBytes(), 0L)
                if (proposalBlock != null) {
                    transactionStatus["euro"] = TransactionStatus.PENDING
                    euroTokenTransaction = proposalBlock
                    val txid = euroTokenTransaction!!.calculateHash()
                    val editor = sharedPreference.edit()
                    editor.putString("euroTXID", txid.toHex())
                    editor.apply()
                    checkEuroStatus()
                }
            }

            join_pool.setOnClickListener {
                if (btcTransaction != null && euroTokenTransaction != null) {
                    join_pool.isEnabled = false
                    val joinProposalBlock = euroWallet.joinPool(poolEuroAddress.hexToBytes(), btcTransaction!!.txId.toString(), euroTokenTransaction!!.calculateHash().toHex())
                    if (joinProposalBlock != null) {
                        joinTransaction = joinProposalBlock
                        val txid = joinTransaction!!.calculateHash()
                        val editor = sharedPreference.edit()
                        editor.putString("joinTXID", txid.toHex())
                        editor.apply()
                    }
                }
            }

            joinPoolSettingsButton.setOnClickListener {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Liquidity Pool Addresses")
                val inputEuro = EditText(requireContext())
                inputEuro.inputType = InputType.TYPE_CLASS_TEXT
                inputEuro.hint = "Liquidity Pool Eurotoken Address\n$poolEuroAddress"
                val inputBtc = EditText(requireContext())
                inputBtc.inputType = InputType.TYPE_CLASS_TEXT
                inputBtc.hint = "Liquidity Pool Bitcoin Address\n$poolBitcoinAddress"
                val layout = LinearLayout(requireContext())
                layout.orientation = LinearLayout.VERTICAL
                layout.addView(inputEuro)
                layout.addView(inputBtc)
                builder.setView(layout)
                builder.setPositiveButton("OK") { _, _ ->
                    poolEuroAddress = inputEuro.text.toString()
                    poolBitcoinAddress = inputBtc.text.toString() }
                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

                val editor = sharedPreference.edit()
                editor.putString("poolEuroAddress", poolEuroAddress)
                editor.putString("poolBitcoinAddress", poolBitcoinAddress)
                editor.apply()

                builder.show()
            }

            while (isActive) {
                checkBtcStatus()
                checkEuroStatus()
                // TODO: Uncomment the line below when not testing
//                join_pool.isEnabled = status["btc"] == Status.VERIFIED && status["euro"] == Status.VERIFIED  && joinTransaction == null
                join_pool.isEnabled = true
                btc_status.text = getString(R.string.transaction_status, getStatusString("btc"))
                eurotoken_status.text = getString(R.string.transaction_status, getStatusString("euro"))
                join_status.text = getString(R.string.transaction_status, getPoolStatus())
                delay(2000)
            }
        }
    }

    private fun getStatusString(cointype: String): String = when (transactionStatus.get(cointype)) {
        TransactionStatus.UNKNOWN -> "Unknown"
        TransactionStatus.NOTSENT -> "Not sent"
        TransactionStatus.SENT -> "Send"
        TransactionStatus.PENDING -> "Pending"
        TransactionStatus.VERIFIED -> "Verified"
        else -> ""
    }

    // TODO Beautify this code
    private fun getPoolStatus(): String {
        if (joinTransaction != null) {
            if (transactionRepository.trustChainCommunity.database.getLinked(joinTransaction!!) != null) {
                return "Joined"
            }
            if (joinTransaction!!.isProposal) {
                return "Pending"
            } else {
                return "Unknown"
            }
        } else {
            return "Not joined"
        }
    }

    private fun checkBtcStatus() {
        if (btcTransaction != null) {
//            sendBtc.isEnabled = false

            when (btcTransaction!!.confidence.confidenceType) {
                TransactionConfidence.ConfidenceType.BUILDING -> {
                    transactionStatus["btc"] = if (btcTransaction!!.confidence.depthInBlocks >= 1) TransactionStatus.VERIFIED else TransactionStatus.PENDING
                }
                TransactionConfidence.ConfidenceType.PENDING -> {
                    transactionStatus["btc"] = TransactionStatus.PENDING
                }
                TransactionConfidence.ConfidenceType.DEAD -> {
                    transactionStatus["btc"] = TransactionStatus.NOTSENT; sendBtc.isEnabled = true
                }
                else -> {
                    transactionStatus["btc"] = TransactionStatus.UNKNOWN
                }
            }
        } else {
            transactionStatus["btc"] = TransactionStatus.NOTSENT
        }
    }

    private fun checkEuroStatus() {
        if (euroTokenTransaction != null) {
//            sendEuro.isEnabled = false
            if (transactionRepository.trustChainCommunity.database.getLinked(euroTokenTransaction!!) != null) {
                transactionStatus["euro"] = TransactionStatus.VERIFIED
            } else if (euroTokenTransaction!!.isProposal) {
                transactionStatus["euro"] = TransactionStatus.PENDING
            }
        } else {
            transactionStatus["euro"] = TransactionStatus.NOTSENT
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pool_join, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()

        app.stopAsync()
    }
}
