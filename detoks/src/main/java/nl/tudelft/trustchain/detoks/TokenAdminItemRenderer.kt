package nl.tudelft.trustchain.detoks

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_token_admin.view.*

class TokenAdminItemRenderer() : ItemLayoutRenderer<TokenAdminItem, View>(
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
