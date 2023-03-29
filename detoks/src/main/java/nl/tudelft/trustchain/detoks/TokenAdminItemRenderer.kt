package nl.tudelft.trustchain.detoks

import Wallet
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_token_admin.view.*

interface TokenButtonListener {
    fun onVerifyClick(token: Token)
    fun onHistoryClick(token: Token)
}

class TokenAdminItemRenderer(
    private val wallet: Wallet?,
    private val displayAs: String,
    private val listener: TokenButtonListener) : ItemLayoutRenderer<TokenAdminItem, View>(
    TokenAdminItem::class.java
){

    override fun bindView(item: TokenAdminItem, view: View) = with(view) {
        valueToken.text = "0"
        latestTimestamp.text = ""
        currentOwner.text = ""

        //link verify
        //link history button
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_token_admin
    }
}
