package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.ui.exchange.ExchangeTransactionItem
import nl.tudelft.trustchain.valuetransfer.util.*
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

class ExchangeTransactionDialog(
    private val transactionItem: ExchangeTransactionItem
) : VTDialogFragment() {
    private val dateFormat = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.ENGLISH)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_exchange_transaction, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val amountView = view.findViewById<TextView>(R.id.tvTransactionAmount)
            val typeView = view.findViewById<TextView>(R.id.tvTransactionType)
            val dateView = view.findViewById<TextView>(R.id.tvTransactionDate)
            val blockStatusView = view.findViewById<TextView>(R.id.tvTransactionBlockStatus)
            val blockStatusColorSignedView = view.findViewById<ImageView>(R.id.ivTransactionBlockStatusColorSigned)
            val blockStatusColorSelfSignedView = view.findViewById<ImageView>(R.id.ivTransactionBlockStatusColorSelfSigned)
            val blockStatusColorWaitingForSignatureView = view.findViewById<ImageView>(R.id.ivTransactionBlockStatusColorWaitingForSignature)
            val fromToTitleView = view.findViewById<TextView>(R.id.tvTransactionFromToTitle)
            val fromToNameView = view.findViewById<TextView>(R.id.tvTransactionFromToName)
            val fromToAddressView = view.findViewById<TextView>(R.id.tvTransactionFromToAddress)
            val messageRowView = view.findViewById<LinearLayout>(R.id.llTransactionMessageRow)
            val messageView = view.findViewById<TextView>(R.id.tvTransactionMessage)
            val rawView = view.findViewById<TextView>(R.id.tvTransactionRaw)
            val sizeView = view.findViewById<TextView>(R.id.tvTransactionSize)
            val blockHashView = view.findViewById<TextView>(R.id.tvTransactionBlockHash)
            val signatureView = view.findViewById<TextView>(R.id.tvTransactionSignature)
            val transactionResendButton = view.findViewById<ConstraintLayout>(R.id.clTransactionResendButton)
            val transactionResendButtonView = view.findViewById<TextView>(R.id.tvTransactionResendButton)
            val transactionSignButton = view.findViewById<ConstraintLayout>(R.id.clTransactionSignButton)
            val transactionSignButtonView = view.findViewById<TextView>(R.id.tvTransactionSignButton)

            val additionalContentView = view.findViewById<LinearLayout>(R.id.llTransactionAdditional)
            val additionalToggleView = view.findViewById<RelativeLayout>(R.id.rlTransactionAdditionalTitleRow)
            val additionalIconHiddenView = view.findViewById<ImageView>(R.id.ivTransactionAdditionalHidden)
            val additionalIconShowedView = view.findViewById<ImageView>(R.id.ivTransactionAdditionalShowed)

            val viewChatView = view.findViewById<ConstraintLayout>(R.id.tvViewChat)
            val viewContactView = view.findViewById<ConstraintLayout>(R.id.tvViewContact)
            viewChatView.apply {
                isVisible = transactionItem.transaction.type == TransactionRepository.BLOCK_TYPE_TRANSFER
                if (isVisible) {
                    findViewById<TextView>(R.id.tvOptionBig).text = resources.getString(R.string.text_string_view_chat)
                    val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_chat)
                    findViewById<ImageView>(R.id.ivOptionBig).setImageDrawable(drawable)
                }
            }
            viewContactView.apply {
                isVisible = transactionItem.transaction.type == TransactionRepository.BLOCK_TYPE_TRANSFER
                if (isVisible) {
                    findViewById<TextView>(R.id.tvOptionBig).text = resources.getString(R.string.text_string_view_contact)
                    val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_contact)
                    findViewById<ImageView>(R.id.ivOptionBig).setImageDrawable(drawable)
                }
            }

            additionalToggleView.setOnClickListener {
                additionalIconHiddenView.isVisible = !additionalIconHiddenView.isVisible
                additionalIconShowedView.isVisible = !additionalIconShowedView.isVisible
                additionalContentView.isVisible = !additionalContentView.isVisible
            }

            val outgoing = if (transactionItem.transaction.type == TransactionRepository.BLOCK_TYPE_TRANSFER) {
                !transactionItem.transaction.outgoing
            } else transactionItem.transaction.outgoing

            val map = transactionItem.transaction.block.transaction.toMap()
            amountView.text = if (map.containsKey("amount")) {
                val amount = (map["amount"] as BigInteger).toLong()
                val plusMin = if (outgoing) "-" else "+"

                "$plusMin${formatBalance(amount)}"
            } else "-"

            listOf(fromToAddressView, messageView, rawView, blockHashView, signatureView).forEach { textView ->
                textView.setOnClickListener {
                    it as TextView
                    when (it.lineCount) {
                        2 -> it.maxLines = 10
                        else -> it.maxLines = 2
                    }
                }
            }

            blockStatusView.text = when (transactionItem.status) {
                ExchangeTransactionItem.BlockStatus.SELF_SIGNED -> {
                    blockStatusColorSelfSignedView.isVisible = true
                    resources.getString(R.string.text_exchange_self_signed)
                }
                ExchangeTransactionItem.BlockStatus.SIGNED -> {
                    blockStatusColorSignedView.isVisible = true
                    resources.getString(R.string.text_exchange_signed)
                }
                ExchangeTransactionItem.BlockStatus.WAITING_FOR_SIGNATURE -> {
                    blockStatusColorWaitingForSignatureView.isVisible = true
                    resources.getString(R.string.text_exchange_waiting_for_signature)
                }
                null -> {
                    resources.getString(R.string.text_exchange_unknown)
                }
            }

            val publicKey = transactionItem.transaction.sender

            when (transactionItem.transaction.type) {
                TransactionRepository.BLOCK_TYPE_CREATE -> {
                    typeView.text = resources.getString(R.string.text_exchange_buy)
                    fromToTitleView.text = resources.getString(R.string.text_from)
                    fromToNameView.text = resources.getString(R.string.text_exchange_eurotoken_exchange)
                    fromToAddressView.text = transactionItem.transaction.sender.keyToBin().toHex()
                }
                TransactionRepository.BLOCK_TYPE_DESTROY -> {
                    typeView.text = resources.getString(R.string.text_exchange_sell)
                    fromToTitleView.text = resources.getString(R.string.text_to)
                    fromToNameView.text = resources.getString(R.string.text_exchange_eurotoken_exchange)
                    fromToAddressView.text = transactionItem.transaction.receiver.keyToBin().toHex()
                }
                TransactionRepository.BLOCK_TYPE_TRANSFER -> {
                    typeView.text = if (outgoing) resources.getString(R.string.text_exchange_transaction_outgoing) else resources.getString(R.string.text_exchange_transaction_incoming)
                    fromToTitleView.text = if (outgoing) resources.getString(R.string.text_to) else resources.getString(R.string.text_from)

                    val contact = ContactStore.getInstance(view.context).getContactFromPublicKey(publicKey)
                    val contactName = contact?.name
                    val unknownName = resources.getString(R.string.text_unknown_contact)

                    val contactState = getPeerChatStore().getContactState(publicKey)?.identityInfo
                    val identityInitials = contactState?.initials
                    val identitySurname = contactState?.surname
                    val identityName = if (identityInitials != null && identitySurname != null) {
                        StringBuilder().append(identityInitials).append(" ").append(identitySurname).toString()
                    } else null

                    fromToNameView.text = when {
                        identityName != null && contactName != null && identityName == contactName -> identityName
                        identityName != null && contactName != null -> StringBuilder().append(identityName).append(" (").append(contactName).append(")").toString()
                        identityName != null && contactName == null -> identityName
                        identityName == null && contactName != null -> contactName
                        else -> unknownName
                    }
                    fromToNameView.apply {
                        contactState?.let { info ->
                            if (info.isVerified) {
                                R.drawable.ic_verified_smaller
                            } else {
                                R.drawable.ic_verified_not_smaller
                            }.let { drawable ->
                                this.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                    0,
                                    0,
                                    drawable,
                                    0
                                )
                            }
                        }
                    }
                    fromToAddressView.text = publicKey.keyToBin().toHex()
                }
            }

            dateView.text = dateFormat.format(transactionItem.transaction.timestamp)

            val isTransfer = transactionItem.transaction.type == TransactionRepository.BLOCK_TYPE_TRANSFER
            messageRowView.isVisible = isTransfer
            if (messageRowView.isVisible) {
                getPeerChatStore().getMessageByTransactionHash(transactionItem.transaction.block.calculateHash())?.message.let { message ->
                    messageView.text = if (message == "" || message == null) "-" else message
                }
            }

            rawView.text = transactionItem.transaction.block.transaction.toString()
            sizeView.text = resources.getString(R.string.x_bytes, transactionItem.transaction.block.rawTransaction.size)
            blockHashView.text = transactionItem.transaction.block.calculateHash().toHex()
            signatureView.text = transactionItem.transaction.block.signature.toHex()

            val trustChainHelper = parentActivity.getStore<TrustChainHelper>()!!
            val transactionRepository = parentActivity.getStore<TransactionRepository>()!!
            val transaction = transactionRepository.getTransactionWithHash(transactionItem.transaction.block.calculateHash())

            transactionResendButton.isVisible = outgoing && transactionItem.transaction.type == TransactionRepository.BLOCK_TYPE_TRANSFER && transaction != null && transactionItem.status == ExchangeTransactionItem.BlockStatus.WAITING_FOR_SIGNATURE

            transactionResendButton.setOnClickListener {
                transactionResendButtonView.text = resources.getString(R.string.btn_transaction_resend_trying)
                val receiver = defaultCryptoProvider.keyFromPublicBin(transactionItem.transaction.block.linkPublicKey)
                val peer = Peer(receiver)
                transactionRepository.trustChainCommunity.sendBlock(transaction!!, peer)

                val resendText = resources.getString(R.string.btn_transaction_resend)

                @Suppress("DEPRECATION")
                Handler().postDelayed(
                    Runnable {
                        transactionResendButton.isVisible = trustChainHelper.getChainByUser(trustChainHelper.getMyPublicKey()).find { it.linkedBlockId == transaction.blockId } == null
                        transactionResendButtonView.text = resendText
                    },
                    2000
                )
            }

            transactionSignButton.isVisible = transactionItem.canSign
            transactionSignButton.setOnClickListener {
                transactionSignButtonView.text = resources.getString(R.string.text_exchange_signing_transaction)
                getTrustChainCommunity().createAgreementBlock(transactionItem.transaction.block, transactionItem.transaction.block.transaction)

                @Suppress("DEPRECATION")
                Handler().postDelayed(
                    Runnable {
                        transactionSignButton.isVisible = false
                    },
                    2000
                )
            }

            viewChatView.setOnClickListener {
                val activeFragment = parentActivity.getActiveFragment()
                if (activeFragment != null) {
                    when (activeFragment.tag) {
                        ValueTransferMainActivity.exchangeFragmentTag -> {
                            parentActivity.closeAllDialogs()
                            val contact = getContactStore().getContactFromPublicKey(publicKey)
                            val identityName = getPeerChatStore().getContactState(publicKey)?.identityInfo?.let {
                                "${it.initials} ${it.surname}"
                            }
                            val args = Bundle().apply {
                                putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, publicKey.keyToBin().toHex())
                                putString(ValueTransferMainActivity.ARG_NAME, contact?.name ?: (identityName ?: resources.getString(R.string.text_unknown_contact)))
                                putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.exchangeFragmentTag)
                            }

                            parentActivity.detailFragment(ValueTransferMainActivity.contactChatFragmentTag, args)
                        }
                        ValueTransferMainActivity.contactChatFragmentTag -> {
                            parentActivity.closeAllDialogs()
                        }
                    }
                }
            }

            viewContactView.setOnClickListener {
                val fragment = parentActivity.getDialogFragment(ContactInfoDialog.TAG)
                if (fragment != null) {
                    fragment as ContactInfoDialog
                    if (fragment.publicKey == publicKey) {
                        bottomSheetDialog.dismiss()
                    } else {
                        parentActivity.closeAllDialogs()
                        ContactInfoDialog(publicKey).show(parentFragmentManager, ContactInfoDialog.TAG)
                    }
                } else {
                    bottomSheetDialog.dismiss()
                    ContactInfoDialog(publicKey).show(parentFragmentManager, ContactInfoDialog.TAG)
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    companion object {
        const val TAG = "exchange_transaction_dialog"
    }
}
