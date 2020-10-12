package nl.tudelft.trustchain.eurotoken.ui.transactions

import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_transaction.view.*
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.eurotoken.R
import java.text.SimpleDateFormat

class TransactionItemRenderer : ItemLayoutRenderer<TransactionItem, View>(
    TransactionItem::class.java) {
    private val dateFormat = SimpleDateFormat.getDateTimeInstance()

    override fun bindView(item: TransactionItem, view: View) = with(view) {
        val amount = item.block.transaction["amount"] as Long
        txtAmount.text = amount.toString()
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_transaction
    }
}
