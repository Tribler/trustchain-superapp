package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.dialog_contact.*
import kotlinx.android.synthetic.main.fragment_exchange_vt.*
import kotlinx.android.synthetic.main.item_contacts_chat.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.attestation.trustchain.ANY_COUNTERPARTY_PK
import nl.tudelft.ipv8.attestation.trustchain.UNKNOWN_SEQ
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.peerchat.entity.ContactImage
import nl.tudelft.trustchain.peerchat.entity.ContactState
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactChatFragment
import nl.tudelft.trustchain.valuetransfer.ui.exchange.ExchangeTransactionItem
import nl.tudelft.trustchain.valuetransfer.ui.exchange.ExchangeTransactionItemRenderer
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityAttributeItem
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityAttributeItemRenderer
import nl.tudelft.trustchain.valuetransfer.util.copyToClipboard
import nl.tudelft.trustchain.common.valuetransfer.extensions.exitEnterView
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon

class ContactInfoDialog(
    private val publicKey: PublicKey
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
        getPeerChatStore().getAttachmentsOfType(publicKey, MessageAttachment.TYPE_IDENTITY_ATTRIBUTE).map { attachments ->
            createAttributeItems(attachments)
        }.asLiveData()
    }

    private var transactionsItems: List<Transaction> = emptyList()
    private var transactionShowCount: Int = 5

    private lateinit var rvTransactions: RecyclerView
    private lateinit var buttonMoreTransactions: Button
    private lateinit var showIdentityAttributes: TextView
    private lateinit var showTransactions: TextView

    private lateinit var bottomSheetDialog: Dialog

    init {
        adapterTransactions.registerRenderer(
            ExchangeTransactionItemRenderer({}, ExchangeTransactionItemRenderer.TYPE_CONTACT_VIEW)
        )

        lifecycleScope.launchWhenCreated {
            while (isActive) {
                refreshTransactions()

                delay(2000)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            bottomSheetDialog = Dialog(requireContext(), R.style.FullscreenDialog)
            bottomSheetDialog.window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            val view = layoutInflater.inflate(R.layout.dialog_contact, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.setCancelable(false)
            bottomSheetDialog.setCanceledOnTouchOutside(false)

            view.findViewById<ImageView>(R.id.ivClose).setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            val contactIdenticonView = view.findViewById<ImageView>(R.id.ivIdenticon)
            val contactImageView = view.findViewById<ImageView>(R.id.ivContactImage)
            val nickName = view.findViewById<TextView>(R.id.tvNickNameValue)
            val nickNameEdit = view.findViewById<ImageView>(R.id.ivNickNameEdit)
            val verificationStatus = view.findViewById<LinearLayout>(R.id.llVerificationStatus)
            val verifiedStatus = view.findViewById<TextView>(R.id.tvVerifiedStatus)
            val notVerifiedStatus = view.findViewById<TextView>(R.id.tvNotVerifiedStatus)
            val verifiedInfoIcon = view.findViewById<ImageView>(R.id.ivVerifiedInfoIcon)
            val verifiedInfo = view.findViewById<TextView>(R.id.tvVerifiedInfo)
            val identityName = view.findViewById<TextView>(R.id.tvIdentityNameValue)
            val identityNameInfoIcon = view.findViewById<ImageView>(R.id.ivIdentityNameInfoIcon)
            val identityNameInfo = view.findViewById<TextView>(R.id.tvIdentityNameInfo)

            nickNameEdit.setOnClickListener {
                contact.value.let {
                    val dialogContactRename = ContactRenameDialog(it ?: Contact("", publicKey)).newInstance(123)
                    dialogContactRename.setTargetFragment(this, 1)

                    bottomSheetDialog.hide()

                    @Suppress("DEPRECATION")
                    dialogContactRename.show(requireFragmentManager().beginTransaction(), "dialog")
                }
            }

            showIdentityAttributes = view.findViewById(R.id.tvShowIdentityAttributes)
            showTransactions = view.findViewById(R.id.tvShowTransactions)
            val identityAttributesView = view.findViewById<NestedScrollView>(R.id.clIdentityAttributes)
            val transactionsView = view.findViewById<NestedScrollView>(R.id.clTransactions)

            showIdentityAttributes.setOnClickListener {
                if (identityAttributesView.isVisible) return@setOnClickListener
                showIdentityAttributes.apply {
                    setTypeface(null, Typeface.BOLD)
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded_selected)
                }
                showTransactions.apply {
                    setTypeface(null, Typeface.NORMAL)
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded)
                }
                transactionsView.exitEnterView(requireContext(), identityAttributesView, false)
            }

            showTransactions.setOnClickListener {
                if (transactionsView.isVisible) return@setOnClickListener
                showIdentityAttributes.apply {
                    setTypeface(null, Typeface.NORMAL)
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded)
                }
                showTransactions.apply {
                    setTypeface(null, Typeface.BOLD)
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded_selected)
                }
                identityAttributesView.exitEnterView(requireContext(), transactionsView)
            }

            val rvIdentityAttributes = view.findViewById<RecyclerView>(R.id.rvContactIdentityAttributes)
            rvTransactions = view.findViewById(R.id.rvContactTransactions)
            buttonMoreTransactions = view.findViewById(R.id.btnMoreTransactions)

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
                    2,
                    {},
                    {},
                    {
                        copyToClipboard(requireContext(), it.value, it.name)
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(
                                R.string.snackbar_copied_clipboard,
                                it.name
                            ),
                            view = view.rootView
                        )
                    }
                )
            )

            rvIdentityAttributes.apply {
                adapter = adapterIdentityAttributes
                layoutManager = LinearLayoutManager(context)
            }

            itemsAttributes.observe(
                this,
                Observer {
                    adapterIdentityAttributes.updateItems(it)
                }
            )

            rvTransactions.apply {
                adapter = adapterTransactions
                layoutManager = LinearLayoutManager(requireContext())
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

                    contactState.identityInfo?.let {
                        identityName.text = if (it.initials != null && it.surname != null) {
                            "${it.initials} ${it.surname}"
                        } else "-"

                        verificationStatus.isVisible = true
                        verifiedStatus.isVisible = it.isVerified
                        notVerifiedStatus.isVisible = !it.isVerified
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
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    private suspend fun refreshTransactions() {
        var items: List<Item>

        withContext(Dispatchers.IO) {
            transactionsItems = getTransactionRepository().getTransactionsBetweenMeAndOther(publicKey, getTrustChainHelper())
            items = createTransactionItems(transactionsItems.take(transactionShowCount))
        }

        adapterTransactions.updateItems(items)
        rvTransactions.setItemViewCacheSize(items.size)
        buttonMoreTransactions.isVisible = transactionsItems.size >= transactionShowCount
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                ContactChatFragment.RENAME_CONTACT -> if (data != null) {
                    bottomSheetDialog.show()
                }
            }
        } else super.onActivityResult(requestCode, resultCode, data)
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
        val myPk = getTrustChainCommunity().myPeer.publicKey.keyToBin()
        val blocks = getTrustChainHelper().getChainByUser(myPk)

        return transactions.map { transaction ->
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
                status
            )
        }
    }
}
