package nl.tudelft.trustchain.detoks

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_token.view.*
import java.time.format.DateTimeFormatter

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun bindView(item: TokenItem, view: View) = with(view) {
        println("tokenview")
        valueToken.text = item.token.value.toString()
        latestTimestamp.text = item.token.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        currentOwner.text = item.token.firstRecipient.toString()
//        if (displayAs == "user") {
        verifyButton.visibility = View.INVISIBLE
        historyButton.visibility = View.INVISIBLE
//        } else {
//            historyButton.setOnClickListener {
//                listener.onHistoryClick(item.token, displayAs)
//            }
//            verifyButton.setOnClickListener {
//                listener.onVerifyClick(item.token, displayAs)
//            }
//        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_token
    }
}
