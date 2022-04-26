package nl.tudelft.trustchain.atomicswap

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.swap.Currency
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.swap.WalletHolder
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import nl.tudelft.trustchain.atomicswap.ui.swap.LOG
import nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list.TradeOfferItem
import nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list.TradeOfferItemRenderer
import nl.tudelft.trustchain.common.BaseActivity
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder

class AtomicSwapActivity : BaseActivity() {

    private var _tradeOffersAdapter: ItemAdapter? = null

    override val navigationGraph get() = R.navigation.atomic_swap_navigation_graph
    override val bottomNavigationMenu get() = R.menu.atomic_swap_menu

    private val atomicSwapCommunity = IPv8Android.getInstance().getOverlay<AtomicSwapCommunity>()!!

    val tradeOffersAdapter get() = _tradeOffersAdapter!!

    var trades = mutableListOf<Trade>()
    var tradeOffers = mutableListOf<Pair<Trade, Peer>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAtomicSwapCallbacks()
        initializeTradesAdapter()
    }

    override fun onDestroy() {
        super.onDestroy()
        _tradeOffersAdapter = null
    }

    fun refreshTradeOffers() {
        Log.i(LOG, "Refreshing trades")
        // TODO: implement fetching already created trades
    }

    fun updateTradeOffersAdapter() {
        lifecycleScope.launch(Dispatchers.Main) {
            val openTrades = tradeOffers.map { TradeOfferItem.fromTrade(it.first) }
            tradeOffersAdapter.updateItems(openTrades)
        }
    }

    private fun initializeTradesAdapter() {
        _tradeOffersAdapter = ItemAdapter()

        val renderer = TradeOfferItemRenderer(
            context = this,
            acceptCallback = { acceptTrade(it) }
        )
        tradeOffersAdapter.registerRenderer(renderer)
    }

    private fun configureAtomicSwapCallbacks() {
        // BOB RECEIVES A TRADE OFFER AND ACCEPTS IT
        atomicSwapCommunity.setOnTrade { trade, peer ->
            try {
                Log.i(LOG, "Received new trade offer: " + trade.offerId)
                val newTrade = Trade(
                    trade.offerId.toLong(),
                    TradeOfferStatus.OPEN,
                    Currency.fromString(trade.toCoin),
                    trade.toAmount,
                    Currency.fromString(trade.fromCoin),
                    trade.fromAmount
                )
                tradeOffers.add(Pair(newTrade, peer))
                updateTradeOffersAdapter()
            } catch (e: Exception) {
                Log.d(LOG, e.stackTraceToString())
            }
        }

        // ALICE RECEIVES AN ACCEPT AND CREATES A TRANSACTION THAT CAN BE CLAIMED BY BOB
        atomicSwapCommunity.setOnAccept { accept, peer ->
            try {
                val trade = trades.first { it.id == accept.offerId.toLong() }
                trade.setOnAccept(accept.btcPubKey.hexToBytes(), accept.ethAddress)
                Log.d(LOG, "RECEIVED ACCEPT FROM ${peer.mid}")

                val tradeOfferItem = tradeOffers.first { it.first.id == trade.id }
                tradeOfferItem.first.status = TradeOfferStatus.IN_PROGRESS
                updateTradeOffersAdapter()

                if (trade.myCoin == Currency.ETH) {
                    val txid = WalletHolder.ethSwap.createSwap(trade)
                    val secretHash = trade.secretHash
                    val myPubKey = trade.myPubKey

                    if (secretHash == null || myPubKey == null) {
                        error("Some fields are not initialised")
                    }
                    val dataToSend = OnAcceptReturn(
                        secretHash = secretHash.toHex(),
                        txid,
                        myPubKey.toHex(),
                        WalletHolder.ethereumWallet.address()
                    )
                    atomicSwapCommunity.sendInitiateMessage(peer, trade.id.toString(), dataToSend)

                    Log.d(
                        LOG,
                        "Alice created an ethereum transaction claimable by bob with id : $txid"
                    )

                } else if (trade.myCoin == Currency.BTC) {
                    Log.d(LOG, "generated secret : ${trade.secret?.toHex()}")
                    val (transaction, _) = WalletHolder.bitcoinSwap.createSwapTransaction(trade)

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
                    Log.d(
                        LOG,
                        "Alice created a bitcoin transaction claimable by Bob with id: ${transaction.txId}"
                    )
                }
            } catch (e: Exception) {
                Log.e(LOG, "Error has occurred when creating a transaction", e)
            }
        }

        WalletHolder.swapTransactionConfidenceListener.setOnTransactionConfirmed { entry ->

            try {
                val trade = trades.first { it.id == entry.offerId.toLong() }

                val secretHash = trade.secretHash
                val myTransaction = trade.myBitcoinTransaction
                val myPubKey = trade.myPubKey
                val myAddress = trade.myAddress

                if (secretHash == null || myTransaction == null || myPubKey == null || myAddress == null) {
                    error("Some fields are not initialised")
                }

                val dataToSend = OnAcceptReturn(
                    secretHash.toHex(),
                    myTransaction.toHex(),
                    myPubKey.toHex(),
                    myAddress
                )

                atomicSwapCommunity.sendInitiateMessage(entry.peer!!, entry.offerId, dataToSend)
                Log.d(LOG, "Alice's transaction is confirmed")

            } catch (e: Exception) {
                Log.d(LOG, e.stackTraceToString())
            }
        }


        // BOB GETS NOTIFIED WHEN ALICE FINISHES HER TRANSACTION AND CREATES HIS OWN TRANSACTION
        atomicSwapCommunity.setOnInitiate { initiateMessage, peer ->
            Log.d(LOG, "Bob received initiat message from alice.")

            try {

                // update the trade
                val trade = trades.first { it.id == initiateMessage.offerId.toLong() }
                trade.setOnInitiate(
                    initiateMessage.btcPublickey.hexToBytes(),
                    initiateMessage.hash.hexToBytes(),
                    initiateMessage.txId.hexToBytes(),
                    initiateMessage.ethAddress
                )


                if (trade.myCoin == Currency.ETH) {
                    Log.d(LOG, "secret hash ${trade.secretHash?.toHex()}")
                    val txid = WalletHolder.ethSwap.createSwap(trade)
                    Log.d(
                        LOG,
                        "get swap : ${WalletHolder.ethSwap.getSwap(trade.secretHash ?: error("")).amount}"
                    )
                    WalletHolder.ethSwap.setOnClaimed(
                        trade.secretHash ?: error("we don't know the secret hash")
                    ) { secret ->
                        trade.setOnSecretObserved(secret)
                        if (trade.counterpartyCoin == Currency.BTC) {
                            val tx = WalletHolder.bitcoinSwap.createClaimTransaction(trade)
                            WalletHolder.walletAppKit.peerGroup().broadcastTransaction(tx)

                            WalletHolder.swapTransactionConfidenceListener.addTransactionClaimed(
                                TransactionConfidenceEntry(
                                    tx.txId.toString(),
                                    trade.id.toString(),
                                    null
                                )
                            )
                            Log.d(
                                LOG,
                                "Bobs ether was claimed by Alice and the secret was revealed. Bob is now claiming Alice's bitcoin"
                            )
                        }
                    }
                    atomicSwapCommunity.sendCompleteMessage(
                        peer,
                        trade.id.toString(),
                        txid
                    )

                    Log.d(
                        LOG,
                        "Bob receive initiate from ALice and locked ether for alice to claim"
                    )

                } else if (trade.myCoin == Currency.BTC) {

                    // create a swap transaction
                    val (transaction, scriptToWatch) = WalletHolder.bitcoinSwap.createSwapTransaction(
                        trade
                    )

                    // add a listener on transaction
                    WalletHolder.swapTransactionConfidenceListener.addTransactionRecipient(
                        TransactionConfidenceEntry(
                            transaction.txId.toString(),
                            initiateMessage.offerId,
                            peer
                        )
                    )

                    // observe Alice spending the transaction
                    val watchedAddress =
                        ScriptBuilder.createP2SHOutputScript(scriptToWatch).getToAddress(
                            RegTestParams.get()
                        )
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
                }

            } catch (e: Exception) {
                Log.d(LOG, e.stackTraceToString())
            }
        }

        WalletHolder.swapTransactionConfidenceListener.setOnTransactionRecipientConfirmed { entry ->

            try {
                val trade = trades.first { it.id == entry.offerId.toLong() }
                val myTransaction =
                    trade.myBitcoinTransaction ?: error("Some fields are not initialized")


                // send complete message
                atomicSwapCommunity.sendCompleteMessage(
                    entry.peer!!,
                    entry.offerId,
                    myTransaction.toHex()
                )
                Log.d(LOG, "Bob's transaction is confirmed")

                val tradeOffer = tradeOffers.first { it.first.id == trade.id }
                tradeOffer.first.status = TradeOfferStatus.COMPLETED
                updateTradeOffersAdapter()
                atomicSwapCommunity.sendRemoveTradeMessage(trade.id.toString())

            } catch (e: Exception) {
                Log.d(LOG, e.stackTraceToString())
            }

        }


        // ALICE GETS NOTIFIED THAT BOB'S TRANSACTION IS COMPLETE AND CLAIMS HER MONEY
        atomicSwapCommunity.setOnComplete { completeMessage, peer ->

            try {
                val trade = trades.first { it.id == completeMessage.offerId.toLong() }
                val trustChainCommunity =
                    IPv8Android.getInstance().getOverlay<TrustChainCommunity>()!!

                if (trade.counterpartyCoin == Currency.ETH) {
                    val receipt = WalletHolder.ethSwap.claimSwap(
                        trade.secret ?: error("cannot claim swap, we don't know the secret")
                    )
                    Log.d(
                        LOG,
                        "Alice received a complete message from Bob and is now claiming Bob's ether."
                    )
                    Log.d(LOG, "tx receipt : $receipt")
                } else if (trade.counterpartyCoin == Currency.BTC) {
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
                    val tchain_trans = mapOf(
                        AtomicSwapTrustchainConstants.TRANSACTION_FROM_COIN to trade.myCoin.toString(),
                        AtomicSwapTrustchainConstants.TRANSACTION_TO_COIN to trade.counterpartyCoin.toString(),
                        AtomicSwapTrustchainConstants.TRANSACTION_FROM_AMOUNT to trade.myAmount,
                        AtomicSwapTrustchainConstants.TRANSACTION_TO_AMOUNT to trade.counterpartyAmount,
                        AtomicSwapTrustchainConstants.TRANSACTION_OFFER_ID to trade.id
                    )
                    val publicKey = peer.publicKey.keyToBin()
                    trustChainCommunity.createProposalBlock(
                        AtomicSwapTrustchainConstants.ATOMIC_SWAP_COMPLETED_BLOCK,
                        tchain_trans,
                        publicKey
                    )
                    Log.d(LOG, "Alice created a trustchain proposal block")
                }

                val tradeOffer = tradeOffers.first { it.first.id == trade.id }
                tradeOffer.first.status = TradeOfferStatus.COMPLETED
                updateTradeOffersAdapter()
                atomicSwapCommunity.sendRemoveTradeMessage(trade.id.toString())

            } catch (e: Exception) {
                Log.d(LOG, e.stackTraceToString())
            }
        }


        // BOB GETS NOTIFIED THAT ALICE CLAIMED THE MONEY AND REVEALED THE SECRET -> HE CLAIMS THE MONEY
        WalletHolder.swapTransactionBroadcastListener.setOnSecretRevealed { secret, offerId ->

            try {
                val trade = trades.first { it.id == offerId.toLong() }
                trade.setOnSecretObserved(secret)

                if (trade.counterpartyCoin == Currency.BTC) {
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
                } else if (trade.counterpartyCoin == Currency.ETH) {
                    WalletHolder.ethSwap.claimSwap(secret)
                    Log.d(LOG, "Bob claimed Ethereum. From a secret from bitcoin.")
                }

                val tradeOffer = tradeOffers.first { it.first.id == offerId.toLong() }
                tradeOffer.first.status = TradeOfferStatus.COMPLETED
                updateTradeOffersAdapter()
                atomicSwapCommunity.sendRemoveTradeMessage(trade.id.toString())

            } catch (e: Exception) {
                Log.d(LOG, e.stackTraceToString())
            }
        }


        // END OF SWAP
        WalletHolder.swapTransactionConfidenceListener.setOnClaimedConfirmed {
            Log.d(LOG, "The claim transaction is confirmed")
            val offerId = it.offerId
            val tradeOffer = tradeOffers.first { it.first.id == offerId.toLong() }
            tradeOffer.first.status = TradeOfferStatus.COMPLETED
            updateTradeOffersAdapter()
            atomicSwapCommunity.sendRemoveTradeMessage(offerId)
        }

        atomicSwapCommunity.setOnRemove { removeMessage, _ ->
            try {
                val myTrade = trades.find { it.id == removeMessage.offerId.toLong() }
                /* Only remove the trade if you weren't involved in it */
                if (myTrade == null) {
                    tradeOffers.remove(tradeOffers.first { it.first.id == removeMessage.offerId.toLong() })
                    updateTradeOffersAdapter()
                }
            } catch (e: Exception) {
                Log.d(LOG, e.stackTraceToString())
            }
        }
    }

    /* call this when user accepts trade offer from Trade Offers screen */
    private fun acceptTrade(tradeOfferItem: TradeOfferItem) {
        val tradeOffer = tradeOffers.find { it.first.id == tradeOfferItem.id }
        if (tradeOffer != null) {
            val trade = tradeOffer.first
            val peer = tradeOffer.second

            trade.status = TradeOfferStatus.IN_PROGRESS
            updateTradeOffersAdapter()
            val newTrade = trade.copy()
            trades.add(newTrade)

            newTrade.setOnTrade()
            val myPubKey = newTrade.myPubKey ?: error("Some fields are not initialized")
            val myAddress = newTrade.myAddress ?: error("Some fields are not initialized")
            atomicSwapCommunity.sendAcceptMessage(
                peer,
                trade.id.toString(),
                myPubKey.toHex(),
                myAddress
            )
            Log.d(LOG, "Bob accepted a trade offer from Alice")
            Log.d(LOG, "SENDING ACCEPT TO PEER ${peer.mid}")

        }
    }

}
