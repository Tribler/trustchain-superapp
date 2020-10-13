package nl.tudelft.trustchain.eurotoken.ui.transactions

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_transaction.view.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.eurotoken.R
import java.text.SimpleDateFormat

class TransactionItemRenderer : ItemLayoutRenderer<TransactionItem, View>(
    TransactionItem::class.java) {
    private val dateFormat = SimpleDateFormat.getDateTimeInstance()

    override fun bindView(item: TransactionItem, view: View) = with(view) {
        txtAmount.text = TransactionRepository.prettyAmount(item.transaction.amount)

        val contact: Contact?
        val peer: PublicKey
        if (item.transaction.outgoing) {
            peer = item.transaction.receiver
            imageInOut.setImageResource(R.drawable.ic_baseline_outgoing_24)
            imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.red));
        } else{
            peer = item.transaction.sender
            imageInOut.setImageResource(R.drawable.ic_baseline_incoming_24)
            imageInOut.setColorFilter(ContextCompat.getColor(getContext(), R.color.green));
        }
        contact = ContactStore.getInstance(view.context).getContactFromPublicKey(peer)

        txtName.text = contact?.name ?: ""
        txtPeerId.text = peer.keyToHash().toHex()
        txtDate.text = dateFormat.format(item.transaction.timestamp)
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_transaction
    }

}
