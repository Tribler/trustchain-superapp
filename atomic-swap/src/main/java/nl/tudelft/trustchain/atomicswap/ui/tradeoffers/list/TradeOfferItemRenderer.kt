package nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list

import android.content.Context
import android.view.View
import android.widget.Toast
import com.mattskala.itemadapter.BindingItemRenderer
import nl.tudelft.trustchain.atomicswap.databinding.ItemTradeOfferBinding
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus

class TradeOfferItemRenderer(val context: Context) :
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

        if (item.status == TradeOfferStatus.OPEN) {
            binding.acceptButton.setOnClickListener {
                // TODO: Accept trade offer
                Toast.makeText(context, "Accepting trade offer", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            binding.acceptButton.visibility = View.GONE
        }
    }

}
