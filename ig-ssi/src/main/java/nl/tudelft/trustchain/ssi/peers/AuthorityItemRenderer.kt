package nl.tudelft.trustchain.ssi.peers

import android.annotation.SuppressLint
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_authority.view.*
import kotlinx.android.synthetic.main.item_peer.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.R
import java.util.*

class AuthorityItemRenderer(
    private val onItemClick: (AuthorityItem) -> Unit,
    private val removeButtonOnClick: (AuthorityItem) -> Unit,
) : ItemLayoutRenderer<AuthorityItem, View>(
    AuthorityItem::class.java
) {
    @SuppressLint("SetTextI18n")
    override fun bindView(item: AuthorityItem, view: View) = with(view) {
        authorityKey.text = item.publicKey.keyToBin().copyOfRange(0, 20).toHex()
        authorityHash.text = item.publicKeyHash

        removeButton.setOnClickListener {
            removeButtonOnClick(item)
        }

        setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_authority
    }
}
