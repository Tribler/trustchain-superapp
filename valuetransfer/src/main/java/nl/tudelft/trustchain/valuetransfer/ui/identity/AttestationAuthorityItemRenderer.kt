package nl.tudelft.trustchain.valuetransfer.ui.identity

import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.ItemIdentityAttestationAuthoritiesBinding
import nl.tudelft.trustchain.valuetransfer.util.AuthorityItem

class AttestationAuthorityItemRenderer(
    private val myPublicKey: String,
    private val onDeleteClickAction: (AuthorityItem) -> Unit,
) : ItemLayoutRenderer<AuthorityItem, View>(
        AuthorityItem::class.java
    ) {
    override fun bindView(
        item: AuthorityItem,
        view: View
    ) = with(view) {
        val binding = ItemIdentityAttestationAuthoritiesBinding.bind(view)
        binding.tvAuthorityAddressHash.text = item.publicKeyHash
        binding.tvAuthorityAddressKey.text = item.publicKey.keyToBin().toHex()

        binding.ivAuthorityDelete.isVisible = item.publicKey.keyToBin().toHex() != myPublicKey

        binding.ivAuthorityDelete.setOnClickListener {
            onDeleteClickAction(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_identity_attestation_authorities
    }
}
