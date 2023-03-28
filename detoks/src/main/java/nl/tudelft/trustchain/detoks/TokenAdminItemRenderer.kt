package nl.tudelft.trustchain.detoks

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer

class TokenAdminItemRenderer() : ItemLayoutRenderer<TokenAdminItem, View>(
    TokenAdminItem::class.java
){

    override fun bindView(item: TokenAdminItem, view: View) = with(view) {
        // doSomeStuff
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_token_admin
    }
}
