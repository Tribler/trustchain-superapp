package nl.tudelft.trustchain.detoks

import android.view.View
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_token.view.*

interface TokenButtonListener {
    fun onVerifyClick(token: Token, access: String)
    fun onHistoryClick(token: Token, access: String)
}

class TokenAdminItemRenderer(
    private val displayAs: String,
    private val listener: TokenButtonListener
) : ItemLayoutRenderer<TokenItem, View>(
    TokenItem::class.java
){

    override fun bindView(item: TokenItem, view: View) = with(view) {
        println("tokenview")
        valueToken.text = item.token.value.toString()
        latestTimestamp.text = item.token.timestamp.toString()
        currentOwner.text = item.token.firstRecipient.toString()
        if (displayAs == "user") {
            verifyButton.visibility = View.INVISIBLE
            historyButton.visibility = View.INVISIBLE
        } else {
            historyButton.setOnClickListener {
                listener.onHistoryClick(item.token, displayAs)
            }
            verifyButton.setOnClickListener {
                listener.onVerifyClick(item.token, displayAs)
            }
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_token
    }
}
