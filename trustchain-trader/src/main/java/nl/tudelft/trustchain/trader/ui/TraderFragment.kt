package nl.tudelft.trustchain.trader.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_trader.*
import kotlinx.coroutines.*
import nl.tudelft.trustchain.common.constants.Currency
import nl.tudelft.trustchain.common.messaging.TradePayload
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.trader.R
import nl.tudelft.trustchain.trader.ai.NaiveBayes
import nl.tudelft.trustchain.trader.databinding.FragmentTraderBinding
import nl.tudelft.trustchain.trader.ui.payload.PayloadItem
import nl.tudelft.trustchain.trader.ui.payload.PayloadItemRenderer
import kotlin.math.roundToInt

@ExperimentalUnsignedTypes
class TraderFragment : BaseFragment(R.layout.fragment_trader) {
    private val adapterAccepted = ItemAdapter()
    private val adapterDeclined = ItemAdapter()
    private var isTrading = true
    private lateinit var ai: NaiveBayes

    private val binding by viewBinding(FragmentTraderBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterAccepted.registerRenderer(PayloadItemRenderer{})
        adapterDeclined.registerRenderer(PayloadItemRenderer{})

    }

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.acceptedPayloads.adapter = adapterAccepted
        binding.acceptedPayloads.layoutManager = LinearLayoutManager(context)
        binding.acceptedPayloads.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))

        binding.declinedPayloads.adapter = adapterDeclined
        binding.declinedPayloads.layoutManager = LinearLayoutManager(context)
        binding.declinedPayloads.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))

        val marketCommunity = getMarketCommunity()
        marketCommunity.addListener(TradePayload.Type.ASK, ::askListener)
        marketCommunity.addListener(TradePayload.Type.BID, ::bidListener)

        switchTrader.setOnClickListener {
            if (!isTrading) {
                TrustChainTraderActivity.acceptedPayloads.add(
                    TradePayload(
                        trustchain.getMyPublicKey(),
                        Currency.DYMBE_DOLLAR,
                        Currency.BTC,
                        43.0,
                        13.0,
                        TradePayload.Type.ASK
                    )
                )
            }
            isTrading = !isTrading
        }
        loadCurrentPayloads((TrustChainTraderActivity.acceptedPayloads), "accepted")
        loadCurrentPayloads((TrustChainTraderActivity.declinedPayloads), "declined")
        ai = NaiveBayes(resources.openRawResource(R.raw.trustchain_trade_data_v7_int))
    }

    private fun loadCurrentPayloads(
        payloads: List<TradePayload>,
        adapterString: String
    ) {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val items = withContext(Dispatchers.IO) {
                    payloads.map {
                        PayloadItem(
                            it.publicKey,
                            it.primaryCurrency,
                            it.secondaryCurrency,
                            it.amount,
                            it.price,
                            it.type
                        )
                    }
                }
                if (adapterString == "accepted") {
                    adapterAccepted.updateItems(items)
                }else if (adapterString == "declined") {
                    adapterDeclined.updateItems(items)
                }

                binding.imgEmpty.isVisible = items.isEmpty() && (TrustChainTraderActivity.acceptedPayloads).isEmpty() && (TrustChainTraderActivity.declinedPayloads).isEmpty()

                delay(1000)
            }
        }
    }

    private fun askListener(payload: TradePayload) {
        Log.d(
            "PayloadFragment::onViewCreated",
            "New ask came in! They are selling ${payload.amount} ${payload.primaryCurrency}. The price is ${payload.price} ${payload.secondaryCurrency} per ${payload.primaryCurrency}"
        )
        val type = 1
        if (ai.predict(payload.amount!!.roundToInt() / payload.price!!.roundToInt(), type) == 1){
            (TrustChainTraderActivity.PayloadsList).acceptedPayloads.add(payload)
        } else {
            (TrustChainTraderActivity.PayloadsList).declinedPayloads.add(payload)
        }
    }
    private fun bidListener(payload: TradePayload) {
        Log.d(
            "PayloadFragment::onViewCreated",
            "New bid came in! They are asking ${payload.amount} ${payload.primaryCurrency}. The price is ${payload.price} ${payload.secondaryCurrency} per ${payload.primaryCurrency}"
        )
        val type = 0
        if (ai.predict(payload.amount!!.roundToInt() / payload.price!!.roundToInt(), type) == 2){
            (TrustChainTraderActivity.PayloadsList).acceptedPayloads.add(payload)
        } else {
            (TrustChainTraderActivity.PayloadsList).declinedPayloads.add(payload)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val marketCommunity = getMarketCommunity()
        marketCommunity.removeListener(TradePayload.Type.ASK, ::askListener)
        marketCommunity.removeListener(TradePayload.Type.BID, ::bidListener)
    }

}
