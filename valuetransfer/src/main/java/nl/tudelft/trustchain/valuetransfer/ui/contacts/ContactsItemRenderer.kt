package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.ItemContactsBinding
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon

class ContactsItemRenderer(
    private val onChatClick: (Contact) -> Unit,
) : ItemLayoutRenderer<ChatItem, View>(
        ChatItem::class.java
    ) {
    override fun bindView(
        item: ChatItem,
        view: View
    ) = with(view) {
        val binding = ItemContactsBinding.bind(view)
        binding.tvContactName.text = item.contact.name

        if (item.image == null) {
            item.contact.publicKey.keyToBin().toHex().let { publicKeyString ->
                generateIdenticon(
                    publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                    getColorByHash(context, publicKeyString),
                    resources
                ).let {
                    binding.ivContactImage.isVisible = false
                    binding.ivIdenticon.apply {
                        isVisible = true
                        setImageBitmap(it)
                    }
                }
            }
        } else {
            binding.ivIdenticon.isVisible = false
            binding.ivContactImage.apply {
                setImageBitmap(item.image.image)
                isVisible = true
            }
        }

        setOnClickListener {
            onChatClick(item.contact)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contacts
    }
}
