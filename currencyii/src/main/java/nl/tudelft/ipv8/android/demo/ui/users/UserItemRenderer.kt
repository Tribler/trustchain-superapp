package nl.tudelft.ipv8.android.demo.ui.users

import android.annotation.SuppressLint
import android.text.Html
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
        val discoveredBlocks = context.resources.getQuantityString(
            R.plurals.x_blocks, item.chainHeight.toInt(), item.chainHeight)
        val storedBlocks = context.resources.getQuantityString(
            R.plurals.x_blocks, item.chainHeight.toInt(), item.storedBlocks)
        txtChainHeight.text = Html.fromHtml(context.resources.getString(R.string.discovered, discoveredBlocks), 0)
        txtChainStored.text = Html.fromHtml(context.resources.getString(R.string.stored, storedBlocks), 0)
        setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_user
    }
}
