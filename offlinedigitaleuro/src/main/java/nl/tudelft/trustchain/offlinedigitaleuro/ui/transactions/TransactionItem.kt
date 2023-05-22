package nl.tudelft.trustchain.offlinedigitaleuro.ui.transactions

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.offlinedigitaleuro.db.Transactions

class TransactionItem(val transaction: Transactions) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is TransactionItem
            && transaction.public_key.contentEquals(other.transaction.public_key)
            && transaction.amount == other.transaction.amount
    }
}
