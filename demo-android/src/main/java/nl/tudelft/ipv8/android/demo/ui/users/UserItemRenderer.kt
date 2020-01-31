package nl.tudelft.ipv8.android.demo.ui.users

import android.annotation.SuppressLint
import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_user.view.*
import nl.tudelft.ipv8.android.demo.R

class UserItemRenderer(
    private val onItemClick: (UserItem) -> Unit
) : ItemLayoutRenderer<UserItem, View>(UserItem::class.java) {
    @SuppressLint("SetTextI18n")
    override fun bindView(item: UserItem, view: View) = with(view) {
        txtPeerId.text = item.peerId
        txtChainHeight.text = context.resources.getQuantityString(R.plurals.x_blocks,
            item.chainHeight.toInt(), item.chainHeight)
        setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_user
    }
}
