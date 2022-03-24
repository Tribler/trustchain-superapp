package nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list

import com.mattskala.itemadapter.Item
import nl.tudelft.trustchain.atomicswap.ui.enums.Currency
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import java.math.BigDecimal

class TradeOfferItem(
    val status: TradeOfferStatus,
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val fromCurrencyAmount: BigDecimal,
    val toCurrencyAmount: BigDecimal
) : Item()
