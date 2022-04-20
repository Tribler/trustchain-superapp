package nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list

import android.content.Context
import android.view.View
import com.mattskala.itemadapter.BindingItemRenderer
import nl.tudelft.trustchain.atomicswap.databinding.ItemTradeOfferBinding
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus

class TradeOfferItemRenderer(val context: Context, val acceptCallback: (TradeOfferItem) -> Unit) :
    BindingItemRenderer<TradeOfferItem, ItemTradeOfferBinding>(
        TradeOfferItem::class.java,
        ItemTradeOfferBinding::inflate
    ) {

    override fun bindView(item: TradeOfferItem, binding: ItemTradeOfferBinding) {
        binding.fromCurrencyAmount.text = item.fromCurrencyAmount.toPlainString()
        binding.toCurrencyAmount.text = item.toCurrencyAmount.toPlainString()

        binding.fromCurrency.setText(item.fromCurrency.currencyCodeStringResourceId)
        binding.toCurrency.setText(item.toCurrency.currencyCodeStringResourceId)

        binding.status.setText(item.status.stringResourceId)

        when (item.status) {
            TradeOfferStatus.OPEN -> {
                binding.acceptButton.setOnClickListener {
                    acceptCallback(item)
                }
            }
            TradeOfferStatus.OPEN_BY_CURRENT_USER -> {
                binding.acceptButton.isEnabled = false
            }
            else -> {
                binding.acceptButton.visibility = View.GONE
            }
        }
    }

}
