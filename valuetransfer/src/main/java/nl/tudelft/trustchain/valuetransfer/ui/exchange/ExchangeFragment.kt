package nl.tudelft.trustchain.valuetransfer.ui.exchange

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.*
import nl.tudelft.ipv8.attestation.trustchain.ANY_COUNTERPARTY_PK
import nl.tudelft.ipv8.attestation.trustchain.UNKNOWN_SEQ
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.util.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentExchangeVtBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.ExchangeTransferMoneyDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.OptionsDialog
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import org.json.JSONObject

class ExchangeFragment : VTFragment(R.layout.fragment_exchange_vt) {
    private val binding by viewBinding(FragmentExchangeVtBinding::bind)

    private val adapterTransactions = ItemAdapter()

    private var transactionsItems: List<Transaction> = emptyList()
    private var transactionShowCount: Int = 10
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

        adapterTransactions.registerRenderer(
            ExchangeTransactionItemRenderer({
                trustchain.createAgreementBlock(it, it.transaction)
                transactionForceUpdate = true
            }, ExchangeTransactionItemRenderer.TYPE_FULL_VIEW)
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

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()

        binding.clExchangeOptions.setOnClickListener {
            OptionsDialog(R.menu.exchange_options, "Choose Option") { _, item ->
                when (item.itemId) {
                    R.id.actionDeposit -> {
                        scanIntent = DEPOSIT_INTENT
                        QRCodeUtils(requireContext()).startQRScanner(
                            this,
                            promptText = resources.getString(R.string.text_scan_qr_exchange_buy),
                            vertical = true
                        )
                    }
                    R.id.actionWithdraw -> {
                        scanIntent = WITHDRAW_INTENT
                        QRCodeUtils(requireContext()).startQRScanner(
                            this,
                            promptText = resources.getString(R.string.text_scan_qr_exchange_sell),
                            vertical = true
                        )
                    }
                    R.id.actionTransferByQR -> {
                        scanIntent = TRANSFER_INTENT
                        QRCodeUtils(requireContext()).startQRScanner(
                            this,
                            promptText = resources.getString(R.string.text_scan_qr_exchange_transfer),
                            vertical = true
                        )
                    }
                    R.id.actionTransferToContact -> ExchangeTransferMoneyDialog(
                        null,
                        null,
                        true
                    ).show(parentFragmentManager, tag)
                    R.id.actionRequestTransferContact -> ExchangeTransferMoneyDialog(
                        null,
                        null,
                        false
                    ).show(parentFragmentManager, tag)
                }
            }.show(parentFragmentManager, tag)
        }

        binding.ivReloadTransactions.setOnClickListener {
            binding.ivReloadTransactions.isVisible = false
            binding.btnShowMoreTransactions.isVisible = false
            binding.pbLoadingSpinner.isVisible = true
            transactionForceUpdate = true
        }

        binding.btnShowMoreTransactions.setOnClickListener {
            transactionShowCount += 5
            transactionForceUpdate = true
            binding.pbLoadingSpinner.isVisible = true
            binding.ivReloadTransactions.isVisible = false
            binding.btnShowMoreTransactions.isVisible = false
        }

        val onBalanceClickListener = View.OnClickListener {
            binding.tvBalanceAmountTitle.isVisible = !binding.tvBalanceAmountTitle.isVisible
            binding.tvBalanceAmount.isVisible = !binding.tvBalanceAmount.isVisible
            binding.tvBalanceVerifiedAmount.isVisible = !binding.tvBalanceVerifiedAmount.isVisible
        }

        binding.clBalance.setOnClickListener(onBalanceClickListener)
        binding.rvTransactions.adapter = adapterTransactions
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())

        parentActivity.getBalance(false).observe(
            viewLifecycleOwner, {
                if (it != binding.tvBalanceAmount.text.toString()) {
                    binding.tvBalanceAmount.text = it
                }
            }
        )

        parentActivity.getBalance(true).observe(
            viewLifecycleOwner, {
                if (it != binding.tvBalanceVerifiedAmount.text.toString()) {
                    binding.tvBalanceVerifiedAmount.text = it
                    binding.ivBalanceErrorIcon.isVisible = parentActivity.getBalance(false).value != it
                    binding.pbBalanceUpdating.isVisible = false
                }
            }
        )
    }

    override fun initView() {
        parentActivity.apply {
            setActionBarTitle(
                resources.getString(R.string.menu_navigation_exchange),
                null
            )
            toggleActionBar(false)
            toggleBottomNavigation(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let { result ->
            try {
                val obj = JSONObject(result)

                when (scanIntent) {
                    DEPOSIT_INTENT -> {
                        if (obj.has(QRScanController.KEY_AMOUNT)) {
                            parentActivity.displaySnackbar(
                                requireContext(),
                                resources.getString(R.string.snackbar_exchange_scan_buy_not_sell),
                                type = ValueTransferMainActivity.SNACKBAR_TYPE_WARNING
                            )
                            return
                        }
                        getQRScanController().exchangeMoney(obj, true)
                    }
                    WITHDRAW_INTENT -> {
                        if (!obj.has(QRScanController.KEY_AMOUNT)) {
                            parentActivity.displaySnackbar(requireContext(),
                                resources.getString(R.string.snackbar_exchange_scan_sell_not_buy),
                                type = ValueTransferMainActivity.SNACKBAR_TYPE_WARNING
                            )
                            return
                        }
                        getQRScanController().exchangeMoney(obj, false)
                    }
                    TRANSFER_INTENT -> {
                        if (obj.has(QRScanController.KEY_PAYMENT_ID)) {
                            parentActivity.displaySnackbar(
                                requireContext(),
                                resources.getString(R.string.snackbar_exchange_scan_transfer_not_buy_sell),
                                type = ValueTransferMainActivity.SNACKBAR_TYPE_WARNING
                            )
                            return
                        }
                        getQRScanController().transferMoney(obj)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                parentActivity.displaySnackbar(
                    requireContext(),
                    resources.getString(R.string.snackbar_qr_code_not_json_format),
                    type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                )
                QRCodeUtils(requireContext()).startQRScanner(
                    this,
                    promptText = resources.getString(R.string.text_scan_again),
                    vertical = true
                )
            }
        }
    }

    private suspend fun refreshTransactions(forceUpdate: Boolean = false) {
        if (forceUpdate) {
            binding.ivReloadTransactions.isVisible = false
            binding.pbLoadingSpinner.isVisible = true
            binding.btnShowMoreTransactions.isVisible = false
        }

        var items: List<Item>

        withContext(Dispatchers.IO) {
            transactionsItems = getTransactionRepository().getLatestNTransactionsOfType(
                trustchain,
                transactionShowCount,
                ALLOWED_EUROTOKEN_TYPES
            )

            items = createTransactionItems(transactionsItems)

            parentActivity.setBalance(
                formatBalance(
                    getTransactionRepository().getMyBalance()
                ),
                false
            )
            parentActivity.setBalance(
                formatBalance(
                    getTransactionRepository().getMyVerifiedBalance()
                ),
                true
            )
        }

        binding.tvNoTransactions.isVisible = items.isEmpty()
        adapterTransactions.updateItems(items)
        binding.rvTransactions.setItemViewCacheSize(items.size)
        binding.pbBalanceUpdating.isVisible = false
        binding.pbLoadingSpinner.isVisible = false
        binding.ivReloadTransactions.isVisible = true
        binding.btnShowMoreTransactions.isVisible = items.size >= transactionShowCount
    }

    private fun verifyBalance() {
        val gateway = getTransactionRepository().getGatewayPeer()
        if (gateway != null) {
            getTransactionRepository().sendCheckpointProposal(gateway)
            parentActivity.displaySnackbar(
                requireContext(),
                resources.getString(R.string.snackbar_exchange_balance_verification_success)
            )
        } else {
            parentActivity.displaySnackbar(
                requireContext(),
                resources.getString(R.string.snackbar_exchange_balance_verification_error),
                type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
            )
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
        private const val DEPOSIT_INTENT = 1
        private const val WITHDRAW_INTENT = 2
    }
}
