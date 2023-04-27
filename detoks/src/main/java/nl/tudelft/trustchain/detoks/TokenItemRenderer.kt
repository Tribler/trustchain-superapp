package nl.tudelft.trustchain.detoks

import Wallet
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_token.view.*
import java.time.format.DateTimeFormatter
import nl.tudelft.trustchain.common.ui.*

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
    val values = listOf("0.05", "0.5", "1", "2", "5")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun bindView(item: TokenItem, view: View) = with(view) {
        println("tokenview")
        valueToken.text = values.get(item.token.value.toInt())
        latestTimestamp.text = item.token.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        if(displayAs == "admin")
            currentOwner.text = "      Admin"
        else
            if(item.previousOwner == null){
                currentOwner.text = "   Unknown"
            } else {
                currentOwner.text = "  " + item.previousOwner
            }
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
