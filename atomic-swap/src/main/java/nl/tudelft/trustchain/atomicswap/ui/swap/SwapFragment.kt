package nl.tudelft.trustchain.atomicswap.ui.swap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_peers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.atomicswap.databinding.FragmentPeersBinding
import nl.tudelft.trustchain.atomicswap.ui.peers.AddressItem
import nl.tudelft.trustchain.atomicswap.ui.peers.PeerItem
import nl.tudelft.trustchain.common.ui.BaseFragment
import androidx.core.view.isVisible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.atomicswap.*
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.RegTestParams
import nl.tudelft.trustchain.atomicswap.ui.wallet.WalletHolder as WalletHolder


class SwapFragment : BaseFragment(R.layout.fragment_peers) {
    private val adapter = ItemAdapter()
    val atomicSwapCommunity = IPv8Android.getInstance().getOverlay<AtomicSwapCommunity>()!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//
//        setContentView(R.layout.fragment_peers)
//
//        adapter.registerRenderer(PeerItemRenderer {
//            // NOOP
//        })
//
//        adapter.registerRenderer(AddressItemRenderer {
//            // NOOP
//        })
//
//        recyclerView.adapter = adapter
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
//

        loadNetworkInfo()
        configureAtomicSwapCallbacks()
        //receiveGossips()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding =  FragmentPeersBinding.inflate(inflater, container, false)
        binding.button.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                atomicSwapCommunity.broadcastTradeOffer(1,1.0)
            }
        }
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

                    val freshKey = WalletHolder.bitcoinWallet.freshReceiveKey()
                    val freshKeyString = freshKey.pubKey.toHex()
                    WalletHolder.bitcoinSwap.addInitialRecipientSwapdata(
                        trade.offerId.toLong(),
                        freshKey.pubKey,
                        trade.toAmount
                    )
                    atomicSwapCommunity.sendAcceptMessage(peer, trade.offerId, freshKeyString)
                }
                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }



        // ALICE RECEIVES AN ACCEPT AND CREATES A TRANSACTION THAT CAN BE CLAIMED BY BOB
        atomicSwapCommunity.setOnAccept { accept, peer ->
            lifecycleScope.launch(Dispatchers.Main) {

                val alertDialogBuilder = AlertDialog.Builder(this@SwapFragment.requireContext())
                alertDialogBuilder.setTitle("Received Accept")
                alertDialogBuilder.setMessage(accept.toString())
                alertDialogBuilder.setPositiveButton("Create transaction") { _, _ ->

                    val (transaction, _) = WalletHolder.bitcoinSwap.startSwapTx(
                        accept.offerId.toLong(),
                        WalletHolder.bitcoinWallet,
                        accept.publicKey.hexToBytes(),
                        "1"
                    )

                    WalletHolder.monitor.addTransactionToListener(
                        TransactionMonitorEntry(
                            transaction.txId.toString(),
                            accept.offerId,
                            peer
                        )
                    )
                    WalletHolder.walletAppKit.peerGroup().broadcastTransaction(transaction)

                    Log.d("Transaction M Swap", "Alice created a transaction claimable by Bob")
                    Log.d("Transaction M Swap", transaction.toString())
                }
                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }

        WalletHolder.monitor.setOnTransactionConfirmed {
            if (WalletHolder.bitcoinSwap.swapStorage.containsKey(it.offerId.toLong())) {
                val data: SwapData.CreatorSwapData =
                    WalletHolder.bitcoinSwap.swapStorage.getValue(it.offerId.toLong()) as SwapData.CreatorSwapData
                if (data.initiateTxId != null) {
                    val d = OnAcceptReturn(
                        data.secretHash.toHex(),
                        data.initiateTxId.toHex(),
                        data.keyUsed.toHex()
                    )


                    lifecycleScope.launch(Dispatchers.Main) {
                        val alertDialogBuilder =
                            AlertDialog.Builder(this@SwapFragment.requireContext())
                        alertDialogBuilder.setTitle("You transaction is confirmed")
                        alertDialogBuilder.setPositiveButton("Notify partner") { _, _ ->
                            atomicSwapCommunity.sendInitiateMessage(it.peer!!, it.offerId, d)
                        }
                        alertDialogBuilder.setCancelable(true)
                        alertDialogBuilder.show()
                    }
                }
            }
        }




        // BOB GETS NOTIFIED WHEN ALICE FINISHES HER TRANSACTION AND CREATES HIS OWN TRANSACTION
        atomicSwapCommunity.setOnInitiate { initiateMessage, peer ->
            lifecycleScope.launch(Dispatchers.Main) {

                val alertDialogBuilder = AlertDialog.Builder(this@SwapFragment.requireContext())
                alertDialogBuilder.setTitle("You counterparty has published his transaction")
                alertDialogBuilder.setMessage(initiateMessage.toString())
                alertDialogBuilder.setPositiveButton("Create my own transaction") { _, _ ->
                    WalletHolder.bitcoinSwap.updateRecipientSwapData(
                        initiateMessage.offerId.toLong(),
                        initiateMessage.hash.hexToBytes(),
                        initiateMessage.publicKey.hexToBytes(),
                        initiateMessage.txId.hexToBytes()
                    )
                    val transaction = WalletHolder.bitcoinSwap.createSwapTxForInitiator(
                        initiateMessage.offerId.toLong(),
                        initiateMessage.publicKey.hexToBytes(),
                        WalletHolder.bitcoinWallet
                    )

                    WalletHolder.monitor.addTransactionToRecipientListener(
                        TransactionMonitorEntry(
                            transaction.txId.toString(),
                            initiateMessage.offerId,
                            peer
                        )
                    )
                    WalletHolder.walletAppKit.peerGroup().broadcastTransaction(transaction)
                    Log.d("Transaction M Swap", "Bob created a transaction claimable by Alice")
                    Log.d("Transaction M Swap", transaction.toString())
                }
                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }

        WalletHolder.monitor.setOnTransactionRecipientConfirmed {
            if (WalletHolder.bitcoinSwap.swapStorage.containsKey(it.offerId.toLong())) {
//                val data: SwapData.RecipientSwapData = WalletHolder.bitcoinSwap.swapStorage.getValue(it.offerId.toLong()) as SwapData.RecipientSwapData

                lifecycleScope.launch(Dispatchers.Main) {
                    val alertDialogBuilder = AlertDialog.Builder(this@SwapFragment.requireContext())
                    alertDialogBuilder.setTitle("You transaction is confirmed")
                    alertDialogBuilder.setPositiveButton("Notify partner") { _, _ ->

                        val (addressToWatch, script) = WalletHolder.bitcoinSwap.getAddresToBeWatched(
                            it.offerId.toLong(),
                            WalletHolder.bitcoinWallet
                        )

                        WalletHolder.transationListener.addWatchedAddress(
                            TransactionListenerEntry(
                                addressToWatch,
                                it.offerId,
                                script
                            )
                        )

                        val tx = WalletHolder.bitcoinWallet.getTransaction(Sha256Hash.wrap(it.transactionId))!!
                        atomicSwapCommunity.sendCompleteMessage(
                            it.peer!!,
                            it.offerId,
                            tx.bitcoinSerialize().toHex()
                        )
                    }
                    alertDialogBuilder.setCancelable(true)
                    alertDialogBuilder.show()
                }
            }
        }




        // ALICE GETS NOTIFIED THAT BOB'S TRANSACTION IS COMPLETE AND CLAIMS HER MONEY
        atomicSwapCommunity.setOnComplete { completeMessage, peer ->
            lifecycleScope.launch(Dispatchers.Main) {

                val alertDialogBuilder = AlertDialog.Builder(this@SwapFragment.requireContext())
                alertDialogBuilder.setTitle("You counterparty has published his transaction")
                alertDialogBuilder.setPositiveButton("Claim money") { _, _ ->
                    val data: SwapData.CreatorSwapData =
                        WalletHolder.bitcoinSwap.swapStorage.getValue(completeMessage.offerId.toLong()) as SwapData.CreatorSwapData
                    if (data.initiateTxId != null) {
                        val tx =
                            Transaction(RegTestParams(), completeMessage.publicKey.hexToBytes())
                        WalletHolder.bitcoinWallet.commitTx(tx)
                        val transaction = WalletHolder.bitcoinSwap.createClaimTxForInitiator(
                            completeMessage.offerId.toLong(),
                            tx.txId.bytes,
                            WalletHolder.bitcoinWallet
                        )

                        WalletHolder.monitor.addClaimedTransactionListener(
                            TransactionMonitorEntry(
                                transaction.txId.toString(),
                                completeMessage.offerId,
                                peer
                            )
                        )
                        WalletHolder.walletAppKit.peerGroup().broadcastTransaction(transaction)
                        Log.d("Transaction M Swap", "Alice created a claim transaction")
                        Log.d("Transaction M Swap", transaction.toString())
                    }
                }
                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }

        WalletHolder.monitor.setOnClaimedConfirmed {
            lifecycleScope.launch(Dispatchers.Main) {
                val alertDialogBuilder = AlertDialog.Builder(this@SwapFragment.requireContext())
                alertDialogBuilder.setTitle("You claim transaction is confirmed")
                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }





        // BOB GETS NOTIFIED THAT ALICE CLAIMED THE MONEY AND REVEALED THE SECRET -> HE CLAIMS THE MONEY
        WalletHolder.transationListener.setOnSecretRevealed { secret, offerId ->
            lifecycleScope.launch(Dispatchers.Main) {
                val alertDialogBuilder = AlertDialog.Builder(this@SwapFragment.requireContext())
                alertDialogBuilder.setTitle("The secret is revealed for trade " + offerId)
                alertDialogBuilder.setMessage(secret.toHex())
                alertDialogBuilder.setPositiveButton("Claim money") { _, _ ->
                    val data: SwapData.RecipientSwapData =
                        WalletHolder.bitcoinSwap.swapStorage.getValue(offerId.toLong()) as SwapData.RecipientSwapData

                    if (data.initiateTxId != null) {
                        val originalTransaction = Transaction(RegTestParams(), data.initiateTxId)
                        print(originalTransaction)

                        val claimTransaction = WalletHolder.bitcoinSwap.createClaimTxTest(
                            originalTransaction,
                            secret,
                            offerId.toLong(),
                            WalletHolder.bitcoinWallet
                        )

                        WalletHolder.monitor.addClaimedTransactionListener(
                            TransactionMonitorEntry(
                                claimTransaction.txId.toString(),
                                offerId,
                                null
                            )
                        )
                        WalletHolder.walletAppKit.peerGroup().broadcastTransaction(claimTransaction)
                    }
                }
                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
                }
            }
        }








    private fun loadNetworkInfo() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val peers = atomicSwapCommunity.getPeers()

                val discoveredAddresses = atomicSwapCommunity.network
                    .getWalkableAddresses(atomicSwapCommunity.serviceId)

                val discoveredBluetoothAddresses = atomicSwapCommunity.network
                    .getNewBluetoothPeerCandidates()
                    .map { it.address }

                val peerItems = peers.map {
                    PeerItem(
                        it
                    )
                }

                val addressItems = discoveredAddresses.map { address ->
                    val contacted = atomicSwapCommunity.discoveredAddressesContacted[address]
                    AddressItem(
                        address,
                        null,
                        contacted
                    )
                }

                val bluetoothAddressItems = discoveredBluetoothAddresses.map { address ->
                    AddressItem(
                        address,
                        null,
                        null
                    )
                }

                val items = peerItems + bluetoothAddressItems + addressItems

                for (peer in peers) {
                    Log.d("AtomicSwapCommunity", "FOUND PEER with id " + peer.mid)
                }


                adapter.updateItems(items)
                txtCommunityName.text = atomicSwapCommunity.javaClass.simpleName
                txtPeerCount.text = "${peers.size} peers"
                val textColorResId = if (peers.isNotEmpty()) R.color.green else R.color.red
                val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
                txtPeerCount.setTextColor(textColor)
                imgEmpty.isVisible = items.isEmpty()

                delay(3000)
            }
        }
    }
}
