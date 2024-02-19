package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.common.valuetransfer.extensions.exitEnterView
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.DialogContactBinding
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactChatFragment
import nl.tudelft.trustchain.valuetransfer.ui.exchange.ExchangeTransactionItemRenderer
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityAttributeItem
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityAttributeItemRenderer
import nl.tudelft.trustchain.valuetransfer.util.ContactImage
import nl.tudelft.trustchain.valuetransfer.util.ContactState
import nl.tudelft.trustchain.valuetransfer.util.DividerItemDecorator
import nl.tudelft.trustchain.valuetransfer.util.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.util.copyToClipboard
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import nl.tudelft.trustchain.valuetransfer.util.toExchangeTransactionItem

class ContactInfoDialog(
    val publicKey: PublicKey
) : VTDialogFragment() {
    private val contact: LiveData<Contact?> by lazy {
        getContactStore().getContactFromPublickey(publicKey).asLiveData()
    }

    private val contactState: LiveData<ContactState?> by lazy {
        getPeerChatStore().getContactStateFlow(publicKey).asLiveData()
    }

    private val contactImage: LiveData<ContactImage?> by lazy {
        getPeerChatStore().getContactImageFlow(publicKey).asLiveData()
    }

    private val publicKeyString = publicKey.keyToBin().toHex()

    private val adapterIdentityAttributes = ItemAdapter()
    private val adapterTransactions = ItemAdapter()

    private val itemsAttributes: LiveData<List<Item>> by lazy {
        getPeerChatStore().getAttachmentsOfType(
            publicKey,
            MessageAttachment.TYPE_IDENTITY_ATTRIBUTE
        ).map { attachments ->
            createAttributeItems(attachments)
        }.asLiveData()
    }

    private var transactionsItems: List<Transaction> = emptyList()
    private var transactionShowCount: Int = 5

    private lateinit var rvTransactions: RecyclerView
    private lateinit var buttonMoreTransactions: ImageView
    private lateinit var showIdentityAttributes: TextView
    private lateinit var showTransactions: TextView
    private lateinit var noAttributesView: TextView
    private lateinit var noTransactionsView: TextView

    private lateinit var bottomSheetDialog: Dialog

    init {
        lifecycleScope.launchWhenCreated {
            while (isActive) {
                refreshTransactions()

                delay(2000)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        adapterTransactions.registerRenderer(
            ExchangeTransactionItemRenderer(false, parentActivity) {
                ExchangeTransactionDialog(it).show(
                    parentFragmentManager,
                    ExchangeTransactionDialog.TAG
                )
            }
        )

        return activity?.let {
            bottomSheetDialog = Dialog(requireContext(), R.style.FullscreenDialog)

            @Suppress("DEPRECATION")
            bottomSheetDialog.window?.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            val binding = DialogContactBinding.inflate(layoutInflater)
            val view = binding.root

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.setCancelable(false)
            bottomSheetDialog.setCanceledOnTouchOutside(false)

            binding.ivClose.setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            val contactIdenticonView = binding.ivIdenticon
            val contactImageView = binding.ivContactImage
            val nickName = binding.tvNickNameValue
            val nickNameEdit = binding.ivNickNameEdit
            val verificationStatus = binding.llVerificationStatus
            val verifiedStatus = binding.tvVerifiedStatus
            val notVerifiedStatus = binding.tvNotVerifiedStatus
            val verifiedInfoIcon = binding.ivVerifiedInfoIcon
            val verifiedInfo = binding.tvVerifiedInfo
            val identityName = binding.tvIdentityNameValue
            val identityNameInfoIcon = binding.ivIdentityNameInfoIcon
            val identityNameInfo = binding.tvIdentityNameInfo

            nickNameEdit.setOnClickListener {
                contact.value.let {
                    val dialogContactRename =
                        ContactRenameDialog(it ?: Contact("", publicKey)).newInstance(123)
                    @Suppress("DEPRECATION")
                    dialogContactRename.setTargetFragment(this, 1)

                    bottomSheetDialog.hide()

                    @Suppress("DEPRECATION")
                    dialogContactRename.show(requireFragmentManager().beginTransaction(), "dialog")
                }
            }

            showIdentityAttributes = binding.tvShowIdentityAttributes
            showTransactions = binding.tvShowTransactions
            val identityAttributesView = binding.clIdentityAttributes
            val transactionsView = binding.clTransactions
            noAttributesView = binding.tvNoAttributes
            noTransactionsView = binding.tvNoTransactions

            showIdentityAttributes.setOnClickListener {
                if (identityAttributesView.isVisible) return@setOnClickListener
                showIdentityAttributes.apply {
                    setTypeface(null, Typeface.BOLD)
                    background =
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.pill_rounded_selected
                        )
                }
                showTransactions.apply {
                    setTypeface(null, Typeface.NORMAL)
                    background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded)
                }
                transactionsView.exitEnterView(requireContext(), identityAttributesView, false)
            }

            showTransactions.setOnClickListener {
                if (transactionsView.isVisible) return@setOnClickListener
                showIdentityAttributes.apply {
                    setTypeface(null, Typeface.NORMAL)
                    background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded)
                }
                showTransactions.apply {
                    setTypeface(null, Typeface.BOLD)
                    background =
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.pill_rounded_selected
                        )
                }
                identityAttributesView.exitEnterView(requireContext(), transactionsView)
            }

            val rvIdentityAttributes = binding.rvContactIdentityAttributes
            rvTransactions = binding.rvContactTransactions
            buttonMoreTransactions = binding.btnShowMoreTransactions

            verifiedInfoIcon.setOnClickListener {
                verifiedInfo.isVisible = !verifiedInfo.isVisible
            }

            contact.observe(
                this,
                Observer {
                    nickName.text = it?.name ?: resources.getString(R.string.text_unknown_contact)
                }
            )

            identityNameInfoIcon.setOnClickListener {
                identityNameInfo.isVisible = !identityNameInfo.isVisible
            }

            adapterIdentityAttributes.registerRenderer(
                IdentityAttributeItemRenderer(
                    2
                ) {
                    copyToClipboard(requireContext(), it.value, it.name)
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(
                            R.string.snackbar_copied_clipboard,
                            it.name
                        )
                    )
                }
            )

            rvIdentityAttributes.apply {
                adapter = adapterIdentityAttributes
                layoutManager = LinearLayoutManager(context)
                val drawable =
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.divider_identity_attribute,
                        requireContext().theme
                    )
                addItemDecoration(DividerItemDecorator(drawable!!) as RecyclerView.ItemDecoration)
            }

            itemsAttributes.observe(
                this,
                Observer {
                    adapterIdentityAttributes.updateItems(it)
                    noAttributesView.isVisible = it.isEmpty()
                }
            )

            rvTransactions.apply {
                adapter = adapterTransactions
                layoutManager = LinearLayoutManager(requireContext())
                val drawable =
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.divider_transaction,
                        requireContext().theme
                    )
                addItemDecoration(DividerItemDecorator(drawable!!) as RecyclerView.ItemDecoration)
            }

            buttonMoreTransactions.setOnClickListener {
                transactionShowCount += 5
                buttonMoreTransactions.isVisible = false
            }

            contactState.observe(
                this,
                Observer { contactState ->
                    if (contactState?.identityInfo == null) {
                        identityName.text = "-"
                        verificationStatus.isVisible = false
                        return@Observer
                    }

                    contactState.identityInfo.let {
                        identityName.text =
                            if (it.initials != null && it.surname != null) {
                                "${it.initials} ${it.surname}"
                            } else {
                                "-"
                            }

                        verificationStatus.isVisible = true
                        verifiedStatus.isVisible = it.isVerified
                        notVerifiedStatus.isVisible = !it.isVerified
                        verifiedInfo.isVisible = !it.isVerified
                        identityNameInfo.isVisible = !it.isVerified
                    }
                }
            )

            contactImage.observe(
                this,
                Observer { contactImage ->
                    if (contactImage?.image == null) {
                        generateIdenticon(
                            publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                            getColorByHash(requireContext(), publicKeyString),
                            resources
                        ).let {
                            contactImageView.isVisible = false
                            contactIdenticonView.apply {
                                isVisible = true
                                setImageBitmap(it)
                            }
                        }
                    } else {
                        contactIdenticonView.isVisible = false
                        contactImageView.apply {
                            isVisible = true
                            setImageBitmap(contactImage.image)
                        }
                    }
                }
            )

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        }
            ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    private suspend fun refreshTransactions() {
        var items: List<Item>

        withContext(Dispatchers.IO) {
            transactionsItems =
                getTransactionRepository().getTransactionsBetweenMeAndOther(
                    publicKey,
                    getTrustChainHelper()
                )
            items = createTransactionItems(transactionsItems.take(transactionShowCount))
        }

        adapterTransactions.updateItems(items)
        rvTransactions.setItemViewCacheSize(items.size)
        buttonMoreTransactions.isVisible = transactionsItems.size >= transactionShowCount

        noTransactionsView.isVisible = items.isEmpty()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        @Suppress("DEPRECATION")
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                ContactChatFragment.RENAME_CONTACT ->
                    if (data != null) {
                        bottomSheetDialog.show()
                    }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun createAttributeItems(attachments: List<MessageAttachment>): List<Item> {
        return attachments.map { attachment ->
            IdentityAttributeItem(
                IdentityAttribute.deserialize(attachment.content, 0).first
            )
        }
            .asSequence()
            .distinctBy {
                it.attribute.name
            }.toList()
    }

    private fun createTransactionItems(transactions: List<Transaction>): List<Item> {
        val myPk = getTrustChainCommunity().myPeer.publicKey
        val blocks = getTrustChainHelper().getChainByUser(myPk.keyToBin())

        return transactions.map { transaction ->
            transaction.toExchangeTransactionItem(myPk, blocks)
        }
    }

    companion object {
        const val TAG = "contact_info_dialog"
    }
}
