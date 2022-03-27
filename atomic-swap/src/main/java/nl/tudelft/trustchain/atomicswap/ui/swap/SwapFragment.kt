package nl.tudelft.trustchain.atomicswap.ui.swap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.*
import nl.tudelft.trustchain.atomicswap.databinding.FragmentAtomicSwapBinding
import nl.tudelft.trustchain.atomicswap.swap.Currency
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
import nl.tudelft.trustchain.common.ui.BaseFragment
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import java.math.BigDecimal
import kotlin.random.Random

const val LOG = "I Atomic Swap"


class SwapFragment : BaseFragment(R.layout.fragment_atomic_swap) {

    private val adapter = ItemAdapter()
    val atomicSwapCommunity = IPv8Android.getInstance().getOverlay<AtomicSwapCommunity>()!!

    private var _binding: FragmentAtomicSwapBinding? = null

    private var _model: SwapViewModel? = null

    private var trades = mutableListOf<Trade>()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val model get() = _model!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAtomicSwapCallbacks()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAtomicSwapBinding.inflate(inflater, container, false)
        _model = ViewModelProvider(this).get(SwapViewModel::class.java)

        initializeUi(binding, model)
        return binding.root
    }

    private fun configureAtomicSwapCallbacks() {


        // BOB RECEIVES A TRADE OFFER AND ACCEPTS IT
        atomicSwapCommunity.setOnTrade { trade, peer ->

            lifecycleScope.launch(Dispatchers.Main) {

                val alertDialogBuilder = AlertDialog.Builder(this@SwapFragment.requireContext())
                alertDialogBuilder.setTitle("Received Trade Offer")
                alertDialogBuilder.setMessage(trade.toString())
                alertDialogBuilder.setPositiveButton("Accept") { _, _ ->

                    val newTrade = Trade(trade.offerId.toLong(), Currency.fromString(trade.toCoin), trade.toAmount, Currency.fromString(trade.fromCoin), trade.fromAmount)
                    trades.add(newTrade)

                    if (newTrade.myCoin == Currency.BTC){
                        newTrade.setOnTrade()
                        val myPubKey = newTrade.myPubKey ?: error("Some fields are not initialized")
                        atomicSwapCommunity.sendAcceptMessage(peer, trade.offerId, myPubKey.toHex() )
                        Log.d(LOG, "Bob accepted a trade offer from Alice")
                    }
                }

                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }


        // ALICE RECEIVES AN ACCEPT AND CREATES A TRANSACTION THAT CAN BE CLAIMED BY BOB
        atomicSwapCommunity.setOnAccept { accept, peer ->

            try {
                val trade  = trades.first { it.id == accept.offerId.toLong() }
                trade.setOnAccept(accept.publicKey.hexToBytes())

                val (transaction,_) = WalletHolder.bitcoinSwap.createSwapTransaction(trade)
                trade.myBitcoinTransaction = transaction.bitcoinSerialize()

                // add a confidence listener
                WalletHolder.swapTransactionConfidenceListener.addTransactionInitiator(
                    TransactionConfidenceEntry(
                        transaction.txId.toString(),
                        accept.offerId,
                        peer
                    )
                )
                // broadcast the transaction
                WalletHolder.walletAppKit.peerGroup().broadcastTransaction(transaction)
                // log
                Log.d(LOG, "Alice created a transaction claimable by Bob")
                Log.d(LOG, transaction.toString())

            } catch (e:Exception){
                e.printStackTrace()
            }
        }

        WalletHolder.swapTransactionConfidenceListener.setOnTransactionConfirmed {entry ->

            try {
                val trade  = trades.first { it.id == entry.offerId.toLong() }

                val secretHash = trade.secretHash
                val myTransaction = trade.myBitcoinTransaction
                val myPubKey = trade.myPubKey

                if(secretHash == null || myTransaction == null || myPubKey == null){
                    error("Some fields are not initialised")
                }

                val dataToSend = OnAcceptReturn(
                    secretHash.toHex(),
                    myTransaction.toHex(),
                    myPubKey.toHex()
                )

                atomicSwapCommunity.sendInitiateMessage(entry.peer!!, entry.offerId, dataToSend)
                Log.d(LOG, "Alice's transaction is confirmed")

            } catch (e:Exception){
                e.printStackTrace()
            }
        }


        // BOB GETS NOTIFIED WHEN ALICE FINISHES HER TRANSACTION AND CREATES HIS OWN TRANSACTION
        atomicSwapCommunity.setOnInitiate { initiateMessage, peer ->

            try {

                // update the trade
                val trade  = trades.first { it.id == initiateMessage.offerId.toLong() }
                trade.setOnInitiate(initiateMessage.publicKey.hexToBytes(), initiateMessage.hash.hexToBytes(), initiateMessage.txId.hexToBytes())

                // create a swap transaction
                val (transaction,scriptToWatch) = WalletHolder.bitcoinSwap.createSwapTransaction(trade)
                trade.myBitcoinTransaction = transaction.bitcoinSerialize()

                // add a listener on transaction
                WalletHolder.swapTransactionConfidenceListener.addTransactionRecipient(
                    TransactionConfidenceEntry(
                        transaction.txId.toString(),
                        initiateMessage.offerId,
                        peer
                    )
                )

                // observe Alice spending the transaction
                val watchedAddress = ScriptBuilder.createP2SHOutputScript(scriptToWatch).getToAddress(RegTestParams.get())
                WalletHolder.swapTransactionBroadcastListener.addWatchedAddress(
                    TransactionListenerEntry(
                        watchedAddress,
                        initiateMessage.offerId,
                        scriptToWatch
                    )
                )

                // broadcast transaction
                WalletHolder.walletAppKit.peerGroup().broadcastTransaction(transaction)

                // log
                Log.d(LOG, "Bob created a transaction claimable by Alice")
                Log.d(LOG, transaction.toString())
                Log.d(LOG, "Bob's started observing the address $watchedAddress")


            } catch (e:Exception){
                e.printStackTrace()
            }
        }

        WalletHolder.swapTransactionConfidenceListener.setOnTransactionRecipientConfirmed {entry ->

            try {
                val trade  = trades.first { it.id == entry.offerId.toLong() }
                val myTransaction = trade.myBitcoinTransaction

                if (myTransaction == null){
                    error("Some fields are not initialized")
                }


                // send complete message
                atomicSwapCommunity.sendCompleteMessage(
                    entry.peer!!,
                    entry.offerId,
                    myTransaction.toHex()
                )
                Log.d(LOG, "Bob's transaction is confirmed")


            } catch (e:Exception){
                e.printStackTrace()
            }

        }


        // ALICE GETS NOTIFIED THAT BOB'S TRANSACTION IS COMPLETE AND CLAIMS HER MONEY
        atomicSwapCommunity.setOnComplete { completeMessage, peer ->

            try {
                val trade  = trades.first { it.id == completeMessage.offerId.toLong() }

                val tx = Transaction(RegTestParams(), completeMessage.txId.hexToBytes())
                WalletHolder.bitcoinWallet.commitTx(tx)
                trade.setOnComplete(completeMessage.txId.hexToBytes())
                val transaction = WalletHolder.bitcoinSwap.createClaimTransaction(trade)

                WalletHolder.swapTransactionConfidenceListener.addTransactionClaimed(
                    TransactionConfidenceEntry(
                        transaction.txId.toString(),
                        completeMessage.offerId,
                        peer
                    )
                )
                WalletHolder.walletAppKit.peerGroup().broadcastTransaction(transaction)
                Log.d(LOG, "Alice created a claim transaction")
                Log.d(LOG, transaction.toString())

            } catch (e:Exception){
                e.printStackTrace()
            }
        }


        // BOB GETS NOTIFIED THAT ALICE CLAIMED THE MONEY AND REVEALED THE SECRET -> HE CLAIMS THE MONEY
        WalletHolder.swapTransactionBroadcastListener.setOnSecretRevealed { secret, offerId ->

            try {
                val trade  = trades.first { it.id == offerId.toLong() }
                trade.setOnSecretObserved(secret)

                val transaction = WalletHolder.bitcoinSwap.createClaimTransaction(trade)

                WalletHolder.swapTransactionConfidenceListener.addTransactionClaimed(
                    TransactionConfidenceEntry(
                        transaction.txId.toString(),
                        offerId,
                        null
                    )
                )
                WalletHolder.walletAppKit.peerGroup().broadcastTransaction(transaction)
                Log.d(LOG, "Bob created a claim transaction")
                Log.d(LOG, transaction.toString())


            } catch (e:Exception){
                e.printStackTrace()
            }
        }


        // END OF SWAP
        WalletHolder.swapTransactionConfidenceListener.setOnClaimedConfirmed {
            Log.d(LOG, "The claim transaction is confirmed")
            lifecycleScope.launch(Dispatchers.Main) {

                val alertDialogBuilder = AlertDialog.Builder(this@SwapFragment.requireContext())
                alertDialogBuilder.setTitle("The Swap is complete")
                alertDialogBuilder.setMessage(it.offerId.toString())
                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }
    }

    private fun initializeUi(binding: FragmentAtomicSwapBinding, model: SwapViewModel) {
        val fromCurrencySpinner = binding.fromCurrencySpinner
        val toCurrencySpinner = binding.toCurrencySpinner

        val fromCurrencyInput = binding.fromCurrencyInput
        val toCurrencyInput = binding.toCurrencyInput

        val createSwapOfferButton = binding.createSwapOfferButton

        model.createSwapOfferEnabled.observe(viewLifecycleOwner) {
            createSwapOfferButton.isEnabled = it
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.currency_codes,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            fromCurrencySpinner.adapter = adapter
            toCurrencySpinner.adapter = adapter
        }

        fromCurrencyInput.addTextChangedListener { validateInput() }
        toCurrencyInput.addTextChangedListener { validateInput() }

        createSwapOfferButton.setOnClickListener { createSwapOffer() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _model = null
    }

    private fun validateInput() {
        val fromCurrencyAmount = binding.fromCurrencyInput.text.toString().toBigDecimalOrNull()
        val toCurrencyAmount = binding.toCurrencyInput.text.toString().toBigDecimalOrNull()

        model.setSwapOfferEnabled(
            fromCurrencyAmount != null && fromCurrencyAmount > BigDecimal.ZERO
                && toCurrencyAmount != null && toCurrencyAmount > BigDecimal.ZERO
        )
    }

    private fun createSwapOffer() {
        val fromCurrency = binding.fromCurrencySpinner.selectedItem
        val toCurrency = binding.toCurrencySpinner.selectedItem

        // Already validated before
        val fromCurrencyAmount = binding.fromCurrencyInput.text.toString()
        val toCurrencyAmount = binding.toCurrencyInput.text.toString()

        val trade = Trade(Random.nextLong(), Currency.fromString(fromCurrency.toString()), fromCurrencyAmount, Currency.fromString(toCurrency.toString()), toCurrencyAmount)
        trades.add(trade)
        atomicSwapCommunity.broadcastTradeOffer(
            trade.id.toInt(),
            fromCurrency.toString(),
            toCurrency.toString(),
            fromCurrencyAmount,
            toCurrencyAmount
        )

        val input = "$fromCurrencyAmount $fromCurrency -> $toCurrencyAmount $toCurrency"
        Toast.makeText(requireContext(), input, Toast.LENGTH_SHORT).show()
    }
}
