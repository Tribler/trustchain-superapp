package nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list

import android.view.View
import com.mattskala.itemadapter.BindingItemRenderer
import nl.tudelft.trustchain.atomicswap.databinding.ItemTradeOfferBinding
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus

class TradeOfferItemRenderer : BindingItemRenderer<TradeOfferItem, ItemTradeOfferBinding>(
    TradeOfferItem::class.java,
    ItemTradeOfferBinding::inflate
) {

    override fun bindView(item: TradeOfferItem, binding: ItemTradeOfferBinding) {
        binding.fromCurrencyAmount.text = item.fromCurrencyAmount.toPlainString()
        binding.toCurrencyAmount.text = item.toCurrencyAmount.toPlainString()

        binding.fromCurrency.setText(item.fromCurrency.currencyCodeStringResourceId)
        binding.toCurrency.setText(item.toCurrency.currencyCodeStringResourceId)

        binding.status.setText(item.status.stringResourceId)

        if (item.status == TradeOfferStatus.OPEN) {
            binding.acceptButton.setOnClickListener {
                // TODO: Accept trade offer
            }
        } else {
            binding.acceptButton.visibility = View.GONE
        }
    }

}
