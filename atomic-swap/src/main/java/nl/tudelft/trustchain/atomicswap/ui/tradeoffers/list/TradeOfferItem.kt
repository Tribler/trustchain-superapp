package nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.atomicswap.swap.Currency
import nl.tudelft.trustchain.atomicswap.swap.Trade
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import java.math.BigDecimal

class TradeOfferItem(
    val id: Long,
    var status: TradeOfferStatus,
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val fromCurrencyAmount: BigDecimal,
    val toCurrencyAmount: BigDecimal
) : Item() {

    override fun areContentsTheSame(other: Item): Boolean {
        return false
    }

    override fun areItemsTheSame(other: Item): Boolean {
        return other is TradeOfferItem && other.id == id
    }

    companion object {
        fun fromTrade(trade: Trade): TradeOfferItem {
            return TradeOfferItem(
                trade.id,
                trade.status,
                trade.myCoin,
                trade.counterpartyCoin,
                BigDecimal(trade.myAmount),
                BigDecimal(trade.counterpartyAmount)
            )
        }
    }

}
