package nl.tudelft.trustchain.offlinedigitaleuro.ui.transactions

import android.annotation.SuppressLint
import android.view.View
import androidx.core.content.ContextCompat
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_transaction.view.*
import nl.tudelft.trustchain.offlinedigitaleuro.R

class TransactionItemRenderer : ItemLayoutRenderer<TransactionItem, View>(
    TransactionItem::class.java
) {

    @SuppressLint("SetTextI18n")
    override fun bindView(item: TransactionItem, view: View) = with(view) {
        if (item.transaction.amount < 0) {
            txtType.text = "Sent"
            txtAmount.setTextColor(ContextCompat.getColor(context, R.color.errorColor))
        } else {
            txtType.text = "Received"
            txtAmount.setTextColor(ContextCompat.getColor(context, R.color.green_190))
        }
        txtPeerId.text = item.transaction.public_key
        txtAmount.text = item.transaction.amount.toString()
        txtDate.text = item.transaction.transaction_datetime
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_transaction
    }
}

