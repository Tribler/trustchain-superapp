package nl.tudelft.trustchain.valuetransfer.ui.exchange

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.attestation.trustchain.ANY_COUNTERPARTY_PK
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.UNKNOWN_SEQ
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.util.*
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentExchangeVtBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.TransferMoneyDialog
import org.json.JSONException
import org.json.JSONObject

@OptIn(ExperimentalUnsignedTypes::class)
class ExchangeFragment : BaseFragment(R.layout.fragment_exchange_vt) {

    private val binding by viewBinding(FragmentExchangeVtBinding::bind)

    private val adapterTransactions = ItemAdapter()

    private var blocks: List<TrustChainBlock> = listOf()

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }

    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, gatewayStore)
    }

    private fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    private var transactionTotalCount: Int = 0
    private var transactionShowCount: Int = 10

    init {
        adapterTransactions.registerRenderer(ExchangeTransactionItemRenderer {
            Log.d("TESTJE", "CLICKED TRANSACTION")
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_exchange_vt, container, false)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as ValueTransferMainActivity).setActionBarTitle("Exchange")
        (requireActivity() as ValueTransferMainActivity).toggleActionBar(false)
        (requireActivity() as ValueTransferMainActivity).toggleBottomNavigation(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bnvExchange)
//        val navController = requireActivity().findNavController(R.id.navHostFragment)
//        bottomNavigationView.setupWithNavController(navController)

        lifecycleScope.launchWhenCreated {
            binding.tvBalanceAmount.text = formatBalance(transactionRepository.getMyVerifiedBalance())
        }

        binding.clTransferQR.setOnClickListener {
            Log.d("TESTJE", "SEND QR CLICKED")

            QRCodeUtils(requireContext()).startQRScanner(this, vertical = true)

        }

        binding.clTransferContact.setOnClickListener {
            Log.d("TESTJE", "SEND INPUT CLICKED")
            TransferMoneyDialog(null, true, transactionRepository, getPeerChatCommunity()).show(parentFragmentManager, tag)
        }

        binding.clRequest.setOnClickListener {
            Log.d("TESTJE", "REQUEST CLICKED")
            TransferMoneyDialog(null, false, transactionRepository, getPeerChatCommunity()).show(parentFragmentManager, tag)
        }

        binding.clButtonBuy.setOnClickListener {
            Log.d("TESTJE", "BUY CLICKED")
        }

        binding.clButtonSell.setOnClickListener {
            Log.d("TESTJE", "SELL CLICKED")
        }

        binding.tvShowMoreTransactions.setOnClickListener {
            transactionShowCount += 5
            refreshTransactions()
        }

        binding.rvTransactions.adapter = adapterTransactions
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())

        Handler().post(
            Runnable {
                lifecycleScope.launchWhenCreated {
                    while(isActive) {
                        blocks = trustchain.getChainByUser(trustchain.getMyPublicKey())
                        refreshTransactions()
                        delay(1500)
                    }
                }
            }
        )

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let {
            try {
                val scanResult = QRScanResult(it)
                val publicKey = scanResult.optString("public_key")
                val amount = scanResult.optLong("amount", -1L)
                val type = scanResult.optString("type")

                if(type == "transfer") {
                    if(!transactionRepository.sendTransferProposal(publicKey.hexToBytes(), amount)) {
                        Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Invalid QR", Toast.LENGTH_LONG).show()
                }
            } catch (e: JSONException) {
                Toast.makeText(requireContext(), "Scan failed, try again", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshTransactions() {
        val transactions = getTransactions()
        adapterTransactions.updateItems(
            createTransactionItems(transactions)
        )

        binding.rvTransactions.setItemViewCacheSize(adapterTransactions.itemCount)

        if(transactionShowCount >= transactionTotalCount) {
            binding.tvShowMoreTransactions.visibility = View.GONE
        }

        binding.tvNoTransactions.isVisible = transactionTotalCount == 0
    }

    private fun getTransactions(): List<Transaction> {
        val transactions = transactionRepository.getTransactions()
            .filter { transaction ->
                ALLOWED_EUROTOKEN_TYPES.contains(transaction.type)
            }
        transactionTotalCount = transactions.size

        return transactions.take(transactionShowCount)
    }

    private fun createTransactionItems(transactions: List<Transaction>) : List<Item> {

        val myPk = getTrustChainCommunity().myPeer.publicKey.keyToBin()

        return transactions
            .map { transaction ->

//                Log.d("TESTJE", blocks.find { it.blockId == transaction.block.linkedBlockId}?.transaction.toString())

                var block = transaction.block

                if(!transaction.outgoing) {
                    val linkedBlock = blocks.find{ it.linkedBlockId == block.blockId}
                    if(linkedBlock != null) {
                        block = linkedBlock
                    }
                }

                val isAnyCounterpartyPk = block.linkPublicKey.contentEquals(ANY_COUNTERPARTY_PK)
                val isMyPk = block.linkPublicKey.contentEquals(myPk)
                val isProposalBlock = block.linkSequenceNumber == UNKNOWN_SEQ

                val hasLinkedBlock = blocks.find { it.linkedBlockId == block.blockId } != null
                val canSign = (isAnyCounterpartyPk || isMyPk) &&
                    isProposalBlock &&
                    !block.isSelfSigned &&
                    !hasLinkedBlock
                val status = when {
                    block.isSelfSigned -> ExchangeTransactionItem.BlockStatus.SELF_SIGNED
                    hasLinkedBlock -> ExchangeTransactionItem.BlockStatus.SIGNED
                    isProposalBlock -> ExchangeTransactionItem.BlockStatus.WAITING_FOR_SIGNATURE
                    else -> null
                }



//                val isProposalBlock = transaction.block.isProposal
//                var hasLinkedBlock = isProposalBlock && transactionRepository.trustChainCommunity.database.getLinked(transaction.block) != null

//                val isProposalBlock = transaction.block.linkSequenceNumber == UNKNOWN_SEQ
//                val hasLinkedBlock = blocks.find { it.linkedBlockId == transaction.block.blockId } != null

//                when {
//                    transaction.block.isSelfSigned -> ExchangeTransactionItem.BlockStatus.SELF_SIGNED
//                    hasLinkedBlock -> ExchangeTransactionItem.BlockStatus.SIGNED
//                    isProposalBlock -> ExchangeTransactionItem.BlockStatus.WAITING_FOR_SIGNATURE
//                    else -> null
//                }

                ExchangeTransactionItem(
                    transaction,
                    canSign,
                    status
                )
            }
    }
//
//    private fun createItems(currencies: List<Currency>) : List<Item> {
//        return currencies.map { currency ->
//            CurrencyItem(currency)
//        }
//    }

    companion object {
        private val ALLOWED_EUROTOKEN_TYPES = listOf(
            TransactionRepository.BLOCK_TYPE_CREATE,
            TransactionRepository.BLOCK_TYPE_DESTROY,
            TransactionRepository.BLOCK_TYPE_TRANSFER,
        )

        class QRScanResult(json: String) : JSONObject(json) {
            var public_key = this.optString("public_key")
            var amount = this.optLong("amount", -1L)
            var name = this.optString("name")
            var type = this.optString("type")
        }
    }
}
