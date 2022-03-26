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
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
import nl.tudelft.trustchain.common.ui.BaseFragment
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.RegTestParams
import java.math.BigDecimal

const val LOG = "I Atomic Swap"


class SwapFragment : BaseFragment(R.layout.fragment_atomic_swap) {

    private val adapter = ItemAdapter()
    val atomicSwapCommunity = IPv8Android.getInstance().getOverlay<AtomicSwapCommunity>()!!

    private var _binding: FragmentAtomicSwapBinding? = null

    private var _model: SwapViewModel? = null

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

                    // Generate a new receive key
                    val freshKey = WalletHolder.bitcoinWallet.freshReceiveKey()
                    val freshKeyString = freshKey.pubKey.toHex()
                    // Add it in the swap data together with offer id and amount
                    WalletHolder.bitcoinSwap.addInitialRecipientSwapdata(
                        trade.offerId.toLong(),
                        freshKey.pubKey,
                        trade.toAmount
                    )
                    // Send an accept message back
                    atomicSwapCommunity.sendAcceptMessage(peer, trade.offerId, freshKeyString)
                    Log.d(LOG, "Bob accepted a trade offer from Alice")
                }

                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }


        // ALICE RECEIVES AN ACCEPT AND CREATES A TRANSACTION THAT CAN BE CLAIMED BY BOB
        atomicSwapCommunity.setOnAccept { accept, peer ->

            // create the swap transaction
            val (transaction, _) = WalletHolder.bitcoinSwap.startSwapTx(
                accept.offerId.toLong(),
                WalletHolder.bitcoinWallet,
                accept.publicKey.hexToBytes()
            )
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

        }

        WalletHolder.swapTransactionConfidenceListener.setOnTransactionConfirmed {

            // extract data of the swap
            if (WalletHolder.bitcoinSwap.swapStorage.containsKey(it.offerId.toLong())) {
                val data: SwapData.CreatorSwapData =
                    WalletHolder.bitcoinSwap.swapStorage.getValue(it.offerId.toLong()) as SwapData.CreatorSwapData

                // extract the original transaction
                if (data.initiateTx != null) {
                    val d = OnAcceptReturn(
                        data.secretHash!!.toHex(),
                        data.initiateTx.toHex(),
                        data.keyUsed!!.toHex()
                    )

                    atomicSwapCommunity.sendInitiateMessage(it.peer!!, it.offerId, d)
                    Log.d(LOG, "Alice's transaction is confirmed")
                }
            }
        }


        // BOB GETS NOTIFIED WHEN ALICE FINISHES HER TRANSACTION AND CREATES HIS OWN TRANSACTION
        atomicSwapCommunity.setOnInitiate { initiateMessage, peer ->

            // update swap data
            WalletHolder.bitcoinSwap.updateRecipientSwapData(
                initiateMessage.offerId.toLong(),
                initiateMessage.hash.hexToBytes(),
                initiateMessage.publicKey.hexToBytes(),
                initiateMessage.txId.hexToBytes()
            )
            // craete a swap transaction
            val transaction = WalletHolder.bitcoinSwap.createSwapTxForInitiator(
                initiateMessage.offerId.toLong(),
                initiateMessage.publicKey.hexToBytes(),
                WalletHolder.bitcoinWallet
            )
            // add a listener on transaction
            WalletHolder.swapTransactionConfidenceListener.addTransactionRecipient(
                TransactionConfidenceEntry(
                    transaction.txId.toString(),
                    initiateMessage.offerId,
                    peer
                )
            )
            // broadcast transaction
            WalletHolder.walletAppKit.peerGroup().broadcastTransaction(transaction)
            // log
            Log.d(LOG, "Bob created a transaction claimable by Alice")
            Log.d(LOG, transaction.toString())
        }

        WalletHolder.swapTransactionConfidenceListener.setOnTransactionRecipientConfirmed {
            if (WalletHolder.bitcoinSwap.swapStorage.containsKey(it.offerId.toLong())) {


                // start watching the script => to reveal the secret
                val (addressToWatch, script) = WalletHolder.bitcoinSwap.getAddresToBeWatched(
                    it.offerId.toLong(),
                    WalletHolder.bitcoinWallet
                )
                WalletHolder.swapTransactionBroadcastListener.addWatchedAddress(
                    TransactionListenerEntry(
                        addressToWatch,
                        it.offerId,
                        script
                    )
                )
                // extract the transaction
                val tx =
                    WalletHolder.bitcoinWallet.getTransaction(Sha256Hash.wrap(it.transactionId))!!
                // send complete message
                atomicSwapCommunity.sendCompleteMessage(
                    it.peer!!,
                    it.offerId,
                    tx.bitcoinSerialize().toHex()
                )
                Log.d(LOG, "Bob's transaction is confirmed")
            }
        }


        // ALICE GETS NOTIFIED THAT BOB'S TRANSACTION IS COMPLETE AND CLAIMS HER MONEY
        atomicSwapCommunity.setOnComplete { completeMessage, peer ->
            val data: SwapData.CreatorSwapData =
                WalletHolder.bitcoinSwap.swapStorage.getValue(completeMessage.offerId.toLong()) as SwapData.CreatorSwapData
            if (data.initiateTx != null) {
                val tx =
                    Transaction(RegTestParams(), completeMessage.txId.hexToBytes())
                WalletHolder.bitcoinWallet.commitTx(tx)
                val transaction = WalletHolder.bitcoinSwap.createClaimTxForInitiator(
                    completeMessage.offerId.toLong(),
                    tx.txId.bytes,
                    WalletHolder.bitcoinWallet
                )

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
            }
        }


        // BOB GETS NOTIFIED THAT ALICE CLAIMED THE MONEY AND REVEALED THE SECRET -> HE CLAIMS THE MONEY
        WalletHolder.swapTransactionBroadcastListener.setOnSecretRevealed { secret, offerId ->
            val data: SwapData.RecipientSwapData =
                WalletHolder.bitcoinSwap.swapStorage.getValue(offerId.toLong()) as SwapData.RecipientSwapData

            if (data.initiateTx != null) {
                val originalTransaction = Transaction(RegTestParams(), data.initiateTx)
                print(originalTransaction)

                val claimTransaction = WalletHolder.bitcoinSwap.createClaimTxTest(
                    originalTransaction,
                    secret,
                    offerId.toLong(),
                    WalletHolder.bitcoinWallet
                )

                WalletHolder.swapTransactionConfidenceListener.addTransactionClaimed(
                    TransactionConfidenceEntry(
                        claimTransaction.txId.toString(),
                        offerId,
                        null
                    )
                )
                WalletHolder.walletAppKit.peerGroup().broadcastTransaction(claimTransaction)
                Log.d(LOG, "Bob observed the secret")
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
        val fromCurrencyAmount = binding.fromCurrencyInput.text.toString().toBigDecimal()
        val toCurrencyAmount = binding.toCurrencyInput.text.toString().toBigDecimal()

        // TODO Implement making swap offer
        val input = "$fromCurrencyAmount $fromCurrency -> $toCurrencyAmount $toCurrency"
        val x = fromCurrencyAmount.toDouble() * 100000000
        WalletHolder.bitcoinSwap.addSwapData(1, Coin.valueOf(x.toLong()))
        atomicSwapCommunity.broadcastTradeOffer(
            1,
            fromCurrency.toString(),
            toCurrency.toString(),
            fromCurrencyAmount.toString(),
            toCurrencyAmount.toString()
        )
        Toast.makeText(requireContext(), input, Toast.LENGTH_SHORT).show()
    }
}
