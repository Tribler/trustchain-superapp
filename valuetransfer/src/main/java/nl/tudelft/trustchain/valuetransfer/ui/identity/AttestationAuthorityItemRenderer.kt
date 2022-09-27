package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_identity_attestation_authorities.view.*
import nl.tudelft.ipv8.util.toHex // OK3
import nl.tudelft.trustchain.ssi.ui.peers.AuthorityItem
import nl.tudelft.trustchain.valuetransfer.R

class AttestationAuthorityItemRenderer(
    private val myPublicKey: String,
    private val onDeleteClickAction: (AuthorityItem) -> Unit,
) : ItemLayoutRenderer<AuthorityItem, View>(
    AuthorityItem::class.java
) {

    override fun bindView(item: AuthorityItem, view: View) = with(view) {
        tvAuthorityAddressHash.text = item.publicKeyHash
        tvAuthorityAddressKey.text = item.publicKey.keyToBin().toHex()

        ivAuthorityDelete.isVisible = item.publicKey.keyToBin().toHex() != myPublicKey

        ivAuthorityDelete.setOnClickListener {
            onDeleteClickAction(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_identity_attestation_authorities
    }
}
