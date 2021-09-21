package nl.tudelft.trustchain.valuetransfer.ui.exchange

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.common.eurotoken.Transaction

data class ExchangeTransactionItem(
    val transaction: Transaction,
    val canSign: Boolean,
    val status: BlockStatus? = null,

) : Item() {
    override fun areItemsTheSame(other: Item): Boolean {
        return other is ExchangeTransactionItem &&
            transaction.block == other.transaction.block
    }

    enum class BlockStatus {
        WAITING_FOR_SIGNATURE,
        SELF_SIGNED,
        SIGNED
    }
}
