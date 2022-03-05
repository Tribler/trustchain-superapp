package nl.tudelft.trustchain.liquidity.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_pool_bitcoin_ethereum.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.trustchain.common.bitcoin.BitcoinMultiSigWallet
import nl.tudelft.trustchain.common.bitcoin.BitcoinWallet
import nl.tudelft.trustchain.common.ethereum.EthereumWeb3jWallet
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.util.TrustChainInteractor
import org.bitcoinj.core.Coin
import org.bitcoinj.params.RegTestParams
import org.web3j.crypto.ECKeyPair
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import kotlin.math.roundToLong

class PoolBitcoinEthereumFragment : BaseFragment(R.layout.fragment_pool_bitcoin_ethereum) {

    enum class Interaction {
        PROVIDE,
        TRADE_BTC_ETH,
        TRADE_ETH_BTC
    }

    private lateinit var bitcoinWallet: BitcoinWallet
    private lateinit var ethereumWallet: EthereumWeb3jWallet

    private lateinit var bitcoinMultiSigWallet: BitcoinMultiSigWallet
    private lateinit var ethereumMultiSigWallet: EthereumWeb3jWallet

    private var debugLogLineNumber = 0
    private val BTC_TO_ETH_RATIO = 2.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted { // Initialization.
            initializeUI()
            initializeBitcoin()
            initializeEthereum()
            wrapUpUI()

            while (isActive) { // Update loop.
                bitcoin_balance.text = String.format("%.2f", bitcoinWallet.balance().value.toDouble().div(100000000f)) + " BTC"
                ethereum_balance.text = String.format("%.2f", ethereumWallet.balance().toDouble().div(1000000000000000000f)) + " ETH"

                bitcoin_pool_balance.text = bitcoinMultiSigWallet.balance().toFriendlyString()
                ethereum_pool_balance.text = String.format("%.2f", ethereumMultiSigWallet.balance().toDouble().div(1000000000000000000f)) + " ETH"

                delay(1000)
            }
        }
    }

    private fun initializeUI() {
        get_bitcoin_button.isEnabled = false
        get_ethereum_button.isEnabled = false
        provide_button.isEnabled = false

        get_bitcoin_button.setOnClickListener {
            onGetBitcoin()
        }
        get_ethereum_button.setOnClickListener {
            onGetEthereum()
        }
        provide_button.setOnClickListener {
            onProvide()
        }
    }

    private fun wrapUpUI() {
        get_bitcoin_button.isEnabled = true
        get_ethereum_button.isEnabled = true
        provide_button.isEnabled = true
    }

    private suspend fun initializeBitcoin() {
        val params = RegTestParams.get()
        val seed = "123456"
        val walletDirectory = context?.cacheDir ?: throw Error("CacheDir not found")

        debugLog("Creating personal bitcoin wallet.")
        bitcoinWallet = BitcoinWallet(params, seed, walletDirectory, seed)
        debugLog("Waiting for the wallet app kit to start running.")

        while (!bitcoinWallet.kit.isRunning) {
            // The wallet app kit is not running yet, keep waiting.
            delay(500)
        }
        debugLog("Created personal bitcoin wallet with address: ${bitcoinWallet.address()}.")

        debugLog("Creating multi-signature bitcoin wallet.")
        bitcoinMultiSigWallet =
            BitcoinMultiSigWallet(params, 1, listOf(bitcoinWallet.key()))
        debugLog("Created multi-signature bitcoin wallet with address: ${bitcoinMultiSigWallet.address()}.")
    }

    private fun initializeEthereum() {
        val web3j = Web3j.build(HttpService("https://goerli.infura.io/v3/496ed2a73f4845978f0062d91bc53999"))
        web3j.web3ClientVersion().sendAsync().get()
        debugLog("Connected to Ethereum goerli testnet node.")

        val password = "123456"
        val keyPair = ECKeyPair.create(BigInteger.valueOf(85678567585858758))
        val walletDirectory = context?.cacheDir ?: throw Error("CacheDir not found")
        ethereumWallet = nl.tudelft.trustchain.common.ethereum.EthereumWeb3jWallet(
            web3j,
            walletDirectory,
            keyPair,
            password
        )
        debugLog("Created personal ethereum wallet with address: ${ethereumWallet.address()}")

        val multiSigPassword = "123456"
        val multiSigKeyPair = ECKeyPair.create(BigInteger.valueOf(74587348957389457))
        ethereumMultiSigWallet = nl.tudelft.trustchain.common.ethereum.EthereumWeb3jWallet(
            web3j,
            walletDirectory,
            multiSigKeyPair,
            multiSigPassword
        )
        debugLog("Created multi-signature ethereum wallet with address: ${ethereumMultiSigWallet.address()}")
    }

    private fun onGetBitcoin() {
        val bitcoinAmount = bitcoin_amount_field.text.toString().toDouble()
        val ethereumAmount = (BTC_TO_ETH_RATIO * bitcoinAmount)
        bitcoin_amount_field.setText(bitcoinAmount.toString())
        if ((bitcoinAmount * 100000000).toLong() > bitcoinMultiSigWallet.balance().value) {
            showAlertDialog("Insufficient bitcoin in pool", "The liquidity pool contain insufficient funds for this trade", null)
            return
        }

        showAlertDialog("GET BITCOIN", "Are you sure you want to trade $ethereumAmount ETH for $bitcoinAmount BTC?", Interaction.TRADE_ETH_BTC)
    }

    private fun onGetEthereum() {
        val ethereumAmount = ethereum_amount_field.text.toString().toDouble()
        val bitcoinAmount = (ethereumAmount / BTC_TO_ETH_RATIO)
        ethereum_amount_field.setText(ethereumAmount.toString())
        showAlertDialog("GET ETHEREUM", "Are you sure you want to trade $bitcoinAmount BTC for $ethereumAmount ETH?", Interaction.TRADE_BTC_ETH)
    }

    private fun onProvide() {
        val bitcoinAmount = bitcoin_amount_field.text.toString().toDouble()
        val ethereumAmount = ethereum_amount_field.text.toString().toDouble()
        showAlertDialog("PROVIDE", "Are you sure you want to provide $bitcoinAmount BTC and $ethereumAmount ETH?", Interaction.PROVIDE)
    }

    private fun onAcceptProvide() {
        // Send bitcoin and ethereum to respective multi-sig wallets.
        val bitcoinAmount = bitcoin_amount_field.text.toString().toDouble()
        val bitcoinSatoshis = (bitcoinAmount * 100000000).roundToLong()
        val bitcoinDepositTransaction = bitcoinMultiSigWallet.deposit(Coin.valueOf(bitcoinSatoshis))
        debugLog("Depositing $bitcoinAmount BTC from ${bitcoinWallet.address()} to ${bitcoinMultiSigWallet.address()}.")
        debugLog("Broadcasting the bitcoin deposit transaction to peers.")
        bitcoinWallet.send(bitcoinDepositTransaction)

        val ethereumAmount = ethereum_amount_field.text.toString().toDouble()
        val ethereumGwei = (ethereumAmount * 1000000000).roundToLong()
        debugLog("Depositing $ethereumAmount BTC from ${ethereumWallet.address()} to ${ethereumMultiSigWallet.address()}.")
        debugLog("Broadcasting the ethereum deposit transaction to peers.")
        ethereumWallet.send(ethereumMultiSigWallet.address(), BigInteger.valueOf(ethereumGwei))

        // Broadcast the transaction over trustchain.
        createTrustChainBlock("LIQUIDITY POOL: ${bitcoinWallet.address()}, ${ethereumWallet.address()} is providing $bitcoinAmount BTC and $ethereumAmount ETH")
    }

    private fun onAcceptTrade(interaction: Interaction) {
        when (interaction) {
            Interaction.TRADE_BTC_ETH -> {
                // Send bitcoin to multi-sig wallet.
                val bitcoinAmount = bitcoin_amount_field.text.toString().toDouble()
                val bitcoinSatoshis = (bitcoinAmount * 100000000).roundToLong()
                val depositTransaction = bitcoinMultiSigWallet.deposit(Coin.valueOf(bitcoinSatoshis))
                debugLog("Depositing $bitcoinAmount BTC from ${bitcoinWallet.address()} to ${bitcoinMultiSigWallet.address()}.")
                debugLog("Broadcasting the bitcoin deposit transaction to peers.")
                bitcoinWallet.send(depositTransaction)

                // Receive ethereum on personal wallet.
                val ethereumAmount = ethereum_amount_field.text.toString().toDouble()
                val ethereumGwei = (ethereumAmount * 1000000000).roundToLong()
                debugLog("Withdrawing $ethereumAmount ETH from ${ethereumMultiSigWallet.address()} to ${ethereumWallet.address()}.")
                debugLog("Broadcasting the ethereum withdraw transaction to peers.")
                ethereumMultiSigWallet.send(ethereumWallet.address(), BigInteger.valueOf(ethereumGwei))

                // Broadcast the transaction over trustchain.
                createTrustChainBlock("LIQUIDITY POOL: ${bitcoinWallet.address()} is trading $bitcoinAmount BTC for $ethereumAmount ETH to ${ethereumWallet.address()}")
            }
            Interaction.TRADE_ETH_BTC -> {
                // Send ethereum to multi-sig wallet.
                val ethereumAmount = ethereum_amount_field.text.toString().toDouble()
                val ethereumGwei = (ethereumAmount * 1000000000).roundToLong()
                debugLog("Depositing $ethereumAmount BTC from ${ethereumWallet.address()} to ${ethereumMultiSigWallet.address()}.")
                debugLog("Broadcasting the ethereum deposit transaction to peers.")
                ethereumWallet.send(ethereumMultiSigWallet.address(), BigInteger.valueOf(ethereumGwei))

                //  Receive bitcoin on personal wallet.
                val bitcoinAmount = bitcoin_amount_field.text.toString().toDouble()
                val bitcoinSatoshis = (bitcoinAmount * 100000000).roundToLong()
                debugLog("Withdrawing $bitcoinAmount BTC from ${bitcoinMultiSigWallet.address()} to ${bitcoinWallet.address()}.")
                var bitcoinTransaction = bitcoinMultiSigWallet.startWithdraw(Coin.valueOf(bitcoinSatoshis), bitcoinWallet.address())
                debugLog("Created the bitcoin withdraw transaction.")
                val bitcoinTransactionHashes = bitcoinMultiSigWallet.hash(bitcoinTransaction)
                debugLog("Hashed the bitcoin withdraw transaction.")
                val bitcoinTransactionSignatures = bitcoinWallet.sign(bitcoinTransactionHashes)
                debugLog("Liquidity providers have signed the bitcoin withdraw transaction.")
                bitcoinTransaction = bitcoinMultiSigWallet.endWithdraw(bitcoinTransaction, bitcoinTransactionSignatures)
                debugLog("Broadcasting the bitcoin withdraw transaction to peers.")
                bitcoinWallet.kit.peerGroup().broadcastTransaction(bitcoinTransaction)

                // Broadcast the transaction over trustchain.
                createTrustChainBlock("LIQUIDITY POOL: ${ethereumWallet.address()} is trading $ethereumAmount ETH for $bitcoinAmount BTC to ${bitcoinWallet.address()}")
            }
            else -> return
        }
    }

    private fun showAlertDialog(title: String, message: String, interaction: Interaction?) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        if (interaction != null) {
            builder.setPositiveButton("ACCEPT") { _, _ ->
                when (interaction) {
                    Interaction.PROVIDE -> {
                        onAcceptProvide()
                    }
                    Interaction.TRADE_BTC_ETH -> {
                        onAcceptTrade(interaction)
                    }
                    Interaction.TRADE_ETH_BTC -> {
                        onAcceptTrade(interaction)
                    }
                }
            }
        }
        builder.setNegativeButton("CLOSE") { _, _ ->
            // Do nothing.
        }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun createTrustChainBlock(text: String) {
        val trustChainCommunity = getTrustChainCommunity()
        val trustChainInteractor = TrustChainInteractor(trustChainCommunity)
        trustChainInteractor.createProposalBlock(text)
    }

    private fun debugLog(line: String) {
        debug_log.post {
            debugLogLineNumber++
            val previousLines = debug_log.text.toString()
            debug_log.text = "$previousLines \n $debugLogLineNumber.  $line"
        }
        scroll_view.fullScroll(View.FOCUS_DOWN)
    }

    override fun onDestroy() {
        super.onDestroy()
        bitcoinWallet.kit.stopAsync()
    }
}
