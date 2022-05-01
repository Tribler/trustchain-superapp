package nl.tudelft.trustchain.valuetransfer.ui.trust

import android.view.View
import android.widget.TextView
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity

class TrustItemRenderer(
    private val parentActivity: ValueTransferMainActivity,
) : ItemLayoutRenderer<TrustItem, View>(
    TrustItem::class.java
) {
    override fun bindView(item: TrustItem, view: View) = with(view) {

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvScore = view.findViewById<TextView>(R.id.tvScore)
        val tvKey = view.findViewById<TextView>(R.id.tvKey)

        // Bind the name, the score and the key values to the UI.
        tvName.text = getName(item.key) ?: parentActivity.getString(R.string.text_trust_peer_unknown)
        tvScore.text = parentActivity.getString(R.string.text_trust_score_format, item.score)
        tvKey.text = item.key.keyToBin().toHex()
    }

    /**
     * Retrieve the name of a contact based on the publicKey.
     * @param publicKey the public key of the contact
     * @return the name of the contact or null
     */
    private fun getName(publicKey: PublicKey): String? {
        val contactState = parentActivity.getStore<PeerChatStore>()!!
            .getContactState(publicKey)?.identityInfo
        val identityInitials = contactState?.initials
        val identitySurname = contactState?.surname

        return if (identityInitials != null && identitySurname != null) {
            StringBuilder().append(identityInitials).append(" ").append(identitySurname)
                .toString()
        } else null
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_trust
    }
}
