package nl.tudelft.trustchain.atomicswap.ui.tradeoffers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.atomicswap.databinding.FragmentAtomicTradeOffersBinding
import nl.tudelft.trustchain.atomicswap.ui.enums.Currency
import nl.tudelft.trustchain.atomicswap.ui.enums.TradeOfferStatus
import nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list.TradeOfferItem
import nl.tudelft.trustchain.atomicswap.ui.tradeoffers.list.TradeOfferItemRenderer
import nl.tudelft.trustchain.common.ui.BaseFragment
import java.math.BigDecimal

class TradeOffersFragment : BaseFragment(R.layout.fragment_atomic_trade_offers) {

    private var _binding: FragmentAtomicTradeOffersBinding? = null
    private var _tradeOffersAdapter: ItemAdapter? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val tradeOffersAdapter get() = _tradeOffersAdapter!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAtomicTradeOffersBinding.inflate(inflater, container, false)
        _tradeOffersAdapter = ItemAdapter()

        initializeUi(binding)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _tradeOffersAdapter = null
    }

    private fun initializeUi(binding: FragmentAtomicTradeOffersBinding) {
        val renderer = TradeOfferItemRenderer(requireContext())
        tradeOffersAdapter.registerRenderer(renderer)
        binding.tradeOffersRecyclerView.adapter = tradeOffersAdapter

        binding.refreshTradeOffersButton.setOnClickListener {
            refreshTradeOffers()
        }

        lifecycleScope.launchWhenStarted {
            delay(3000)
            generateExampleTradeOffers()
        }
    }

    private fun refreshTradeOffers() {
        Toast.makeText(requireContext(), R.string.toast_refreshing_trade_offers, Toast.LENGTH_SHORT)
            .show()
        // TODO: Fetch new information from Trustchain and update the adapter
    }

    private fun generateExampleTradeOffers() {
        val example1 = TradeOfferItem(
            id = "a",
            status = TradeOfferStatus.OPEN,
            fromCurrency = Currency.BITCOIN,
            toCurrency = Currency.ETHEREUM,
            fromCurrencyAmount = BigDecimal("1.0"),
            toCurrencyAmount = BigDecimal("1.0")
        )
        val example2 = TradeOfferItem(
            id = "b",
            status = TradeOfferStatus.IN_PROGRESS,
            fromCurrency = Currency.BITCOIN,
            toCurrency = Currency.BITCOIN,
            fromCurrencyAmount = BigDecimal("1.0"),
            toCurrencyAmount = BigDecimal("1.0")
        )
        val example3 = TradeOfferItem(
            id = "c",
            status = TradeOfferStatus.COMPLETED,
            fromCurrency = Currency.ETHEREUM,
            toCurrency = Currency.ETHEREUM,
            fromCurrencyAmount = BigDecimal("1.0"),
            toCurrencyAmount = BigDecimal("1.0")
        )

        tradeOffersAdapter.updateItems(listOf(example1, example2, example3))
    }

}
