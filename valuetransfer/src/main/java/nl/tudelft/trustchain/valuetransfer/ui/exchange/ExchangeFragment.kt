package nl.tudelft.trustchain.valuetransfer.ui.exchange

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.*
import nl.tudelft.ipv8.attestation.trustchain.ANY_COUNTERPARTY_PK
import nl.tudelft.ipv8.attestation.trustchain.UNKNOWN_SEQ
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.util.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentExchangeVtBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.ExchangeTransferMoneyDialog
import org.json.JSONObject

class ExchangeFragment : BaseFragment(R.layout.fragment_exchange_vt) {
    private val binding by viewBinding(FragmentExchangeVtBinding::bind)
    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var transactionRepository: TransactionRepository

    private val adapterTransactions = ItemAdapter()

    private var transactionsItems: List<Transaction> = emptyList()
    private var transactionShowCount: Int = 5
    private var scanIntent: Int = -1
    private var transactionForceUpdate: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exchange_vt, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        parentActivity = requireActivity() as ValueTransferMainActivity
        transactionRepository = parentActivity.getStore()!!

        adapterTransactions.registerRenderer(
            ExchangeTransactionItemRenderer {
                trustchain.createAgreementBlock(it, it.transaction)
                transactionForceUpdate = true
            }
        )

        lifecycleScope.launchWhenStarted {
            while (isActive) {
                refreshTransactions(transactionForceUpdate)

                if (transactionForceUpdate)
                    verifyBalance()

                transactionForceUpdate = false

                delay(1000)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onResume()

        binding.clTransferQR.setOnClickListener {
            scanIntent = TRANSFER_INTENT
            QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan QR Code to transfer EuroToken(s)", vertical = true)
        }

        binding.clTransferToContact.setOnClickListener {
            ExchangeTransferMoneyDialog(null, null, true).show(parentFragmentManager, tag)
        }

        binding.clRequest.setOnClickListener {
            ExchangeTransferMoneyDialog(null, null, false).show(parentFragmentManager, tag)
        }

        binding.clButtonBuy.setOnClickListener {
            scanIntent = BUY_EXCHANGE_INTENT
            QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan Buy EuroToken QR Code from Exchange", vertical = true)
        }

        binding.clButtonSell.setOnClickListener {
            scanIntent = SELL_EXCHANGE_INTENT
            QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan Sell EuroToken QR Code from Exchange", vertical = true)
        }

        binding.ivReloadTransactions.setOnClickListener {
            binding.ivReloadTransactions.isVisible = false
            binding.tvShowMoreTransactions.isVisible = false
            binding.pbLoadingSpinner.isVisible = true
            transactionForceUpdate = true
        }

        binding.tvShowMoreTransactions.setOnClickListener {
            transactionShowCount += 5
            transactionForceUpdate = true
            binding.pbLoadingSpinner.isVisible = true
            binding.ivReloadTransactions.isVisible = false
            binding.tvShowMoreTransactions.isVisible = false
        }

        val onBalanceClickListener = View.OnClickListener {
            binding.tvBalanceAmountTitle.isVisible = !binding.tvBalanceAmountTitle.isVisible
            binding.tvBalanceAmount.isVisible = !binding.tvBalanceAmount.isVisible
            binding.tvBalanceVerifiedAmount.isVisible = !binding.tvBalanceVerifiedAmount.isVisible
        }

        binding.clBalanceRow.setOnClickListener(onBalanceClickListener)
        binding.rvTransactions.adapter = adapterTransactions
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())

        parentActivity.getBalance(false).observe(
            viewLifecycleOwner,
            Observer {
                if (it != binding.tvBalanceAmount.text.toString()) {
                    binding.tvBalanceAmount.text = it
                }
            }
        )

        parentActivity.getBalance(true).observe(
            viewLifecycleOwner,
            Observer {
                if (it != binding.tvBalanceVerifiedAmount.text.toString()) {
                    binding.tvBalanceVerifiedAmount.text = it
                    binding.ivBalanceErrorIcon.isVisible = parentActivity.getBalance(false).value != it
                    binding.pbBalanceUpdating.isVisible = false
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        parentActivity.setActionBarTitle("Exchange")
        parentActivity.toggleActionBar(false)
        parentActivity.toggleBottomNavigation(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.exchange_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionVerifyBalance -> {
                verifyBalance()
                true
            }
            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let { result ->
            try {
                val obj = JSONObject(result)

                when (scanIntent) {
                    TRANSFER_INTENT -> {
                        if (obj.has("payment_id")) {
                            parentActivity.displaySnackbar(requireContext(), "Please scan a transfer QR-code instead of buy or sell", type = ValueTransferMainActivity.SNACKBAR_TYPE_WARNING)
                            return
                        }
                        parentActivity.getQRScanController().transferMoney(obj)
                    }
                    BUY_EXCHANGE_INTENT -> {
                        if (obj.has("amount")) {
                            parentActivity.displaySnackbar(requireContext(), "Please scan a buy QR-code instead of sell", type = ValueTransferMainActivity.SNACKBAR_TYPE_WARNING)
                            return
                        }
                        parentActivity.getQRScanController().exchangeMoney(obj, true)
                    }
                    SELL_EXCHANGE_INTENT -> {
                        if (!obj.has("amount")) {
                            parentActivity.displaySnackbar(requireContext(), "Please scan a sell QR-code instead of buy", type = ValueTransferMainActivity.SNACKBAR_TYPE_WARNING)
                            return
                        }
                        parentActivity.getQRScanController().exchangeMoney(obj, false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                parentActivity.displaySnackbar(requireContext(), "Scanned QR code not in JSON format", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan again", vertical = true)
            }
        }
    }

    private suspend fun refreshTransactions(forceUpdate: Boolean = false) {
        if (forceUpdate) {
            binding.ivReloadTransactions.isVisible = false
            binding.pbLoadingSpinner.isVisible = true
            binding.tvShowMoreTransactions.isVisible = false
        }

        var items: List<Item>

        withContext(Dispatchers.IO) {
            transactionsItems = transactionRepository.getLatestNTransactionsOfType(trustchain, transactionShowCount, ALLOWED_EUROTOKEN_TYPES)
            items = createTransactionItems(transactionsItems)
            parentActivity.setBalance(formatBalance(transactionRepository.getMyBalance()), false)
            parentActivity.setBalance(formatBalance(transactionRepository.getMyVerifiedBalance()), true)
        }

        adapterTransactions.updateItems(items)
        binding.rvTransactions.setItemViewCacheSize(items.size)
        binding.pbBalanceUpdating.isVisible = false
        binding.pbLoadingSpinner.isVisible = false
        binding.ivReloadTransactions.isVisible = true
        binding.tvShowMoreTransactions.isVisible = items.size >= transactionShowCount
    }

    private fun verifyBalance() {
        val gateway = transactionRepository.getGatewayPeer()
        if (gateway != null) {
            transactionRepository.sendCheckpointProposal(gateway)
            parentActivity.displaySnackbar(requireContext(), "Balance verification succeeded")
        } else {
            parentActivity.displaySnackbar(requireContext(), "Balance verification failed", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
        }
    }

    private fun createTransactionItems(transactions: List<Transaction>): List<Item> {
        val myPk = getTrustChainCommunity().myPeer.publicKey.keyToBin()
        val blocks = trustchain.getChainByUser(myPk)

        return transactions
            .map { transaction ->
                val block = transaction.block

                val isAnyCounterpartyPk = block.linkPublicKey.contentEquals(ANY_COUNTERPARTY_PK)
                val isMyPk = block.linkPublicKey.contentEquals(myPk)
                val isProposalBlock = block.linkSequenceNumber == UNKNOWN_SEQ

                // Some other (proposal) block is linked to the current agreement block. This is to find the status of incoming transactions.
                val hasLinkedBlock = blocks.find { it.linkedBlockId == block.blockId } != null

                // Some other (agreement) block is linked to the current proposal block. This is to find the status of outgoing transactions.
                val outgoingIsLinkedBlock = blocks.find { block.linkedBlockId == it.blockId } != null
                val status = when {
                    hasLinkedBlock || outgoingIsLinkedBlock -> ExchangeTransactionItem.BlockStatus.SIGNED
                    block.isSelfSigned -> ExchangeTransactionItem.BlockStatus.SELF_SIGNED
                    isProposalBlock -> ExchangeTransactionItem.BlockStatus.WAITING_FOR_SIGNATURE
                    else -> null
                }

                // Determine whether the transaction/block can be signed
                val canSign = (isAnyCounterpartyPk || isMyPk) &&
                    isProposalBlock &&
                    !block.isSelfSigned &&
                    !hasLinkedBlock

                ExchangeTransactionItem(
                    transaction,
                    canSign,
                    status,
                )
            }
    }

    companion object {
        private val ALLOWED_EUROTOKEN_TYPES = listOf(
            TransactionRepository.BLOCK_TYPE_CREATE,
            TransactionRepository.BLOCK_TYPE_DESTROY,
            TransactionRepository.BLOCK_TYPE_TRANSFER,
        )

        private const val TRANSFER_INTENT = 0
        private const val BUY_EXCHANGE_INTENT = 1
        private const val SELL_EXCHANGE_INTENT = 2
    }
}
