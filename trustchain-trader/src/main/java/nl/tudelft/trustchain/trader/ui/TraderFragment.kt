package nl.tudelft.trustchain.trader.ui

import android.annotation.SuppressLint
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
import nl.tudelft.trustchain.trader.ui.TrustChainTraderActivity.PayloadsList.amountBTC
import nl.tudelft.trustchain.trader.ui.TrustChainTraderActivity.PayloadsList.amountDD
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
        adapterAccepted.registerRenderer(PayloadItemRenderer {})
        adapterDeclined.registerRenderer(PayloadItemRenderer {})
    }

    @SuppressLint("SetTextI18n")
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
        amountFieldDD.text = amountDD.toString()
        amountFieldBTC.text = amountBTC.toString()

        switchTrader.setOnClickListener {
            isTrading = !isTrading
            this.askListener(
                TradePayload(
                    trustchain.getMyPublicKey(),
                    Currency.BTC,
                    Currency.DYMBE_DOLLAR,
                    1.0,
                    80.0,
                    TradePayload.Type.ASK
                )
            )
        }
        loadCurrentPayloads((TrustChainTraderActivity.acceptedPayloads), "accepted")
        loadCurrentPayloads((TrustChainTraderActivity.declinedPayloads), "declined")
        ai = NaiveBayes(resources.openRawResource(R.raw.ai_trading_data))
//        for (i in 0 until 10) {
//            adapterAccepted.updateItems(items)(TradePayload(ByteArray(5),
//                Currency.BTC,
//                Currency.DYMBE_DOLLAR,
//                10.0,
//                1.0,
//                TradePayload.Type.ASK
//            ))
//        }
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
                } else if (adapterString == "declined") {
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
        if (isTrading) {
            val type = ai.predict(payload.price!!.roundToInt() / payload.amount!!.roundToInt())
            if (type == 1) {
                accept(payload, type)
            } else if (type == 2) {
                val price = round(payload.price!!.roundToInt() / payload.amount!!.roundToInt())
                if (ai.predict(price) == 1) {
                    Log.d(
                        "PayloadFragment::onViewCreated",
                        "Accepted!"
                    )
                    accept(payload, 1)
                }
            } else {
                (TrustChainTraderActivity.PayloadsList).declinedPayloads.add(0, payload)
            }
        }
    }

    private fun bidListener(payload: TradePayload) {
        Log.d(
            "PayloadFragment::onViewCreated",
            "New bid came in! They are asking ${payload.amount} ${payload.primaryCurrency}. The price is ${payload.price} ${payload.secondaryCurrency} per ${payload.primaryCurrency}"
        )
        if (isTrading) {
            val type = ai.predict(payload.amount!!.roundToInt() / payload.price!!.roundToInt())
            if (type == 0) {
                accept(payload, type)
            } else if (type == 2) {
                val price = round(payload.amount!!.roundToInt() / payload.price!!.roundToInt())
                if (ai.predict(price) == 0) {
                    accept(payload, 0)
                }
            } else {
                (TrustChainTraderActivity.PayloadsList).declinedPayloads.add(0, payload)
            }
        }
    }
    private fun round(price: Int): Int {
        return when {
            price > 115 -> {
                115
            }
            price < 85 -> {
                85
            }
            else -> {
                price
            }
        }
    }

    private fun accept(payload: TradePayload, type: Int) {
//        Commenting the following two rules will let the AI bot stop sending proposal blocks to the sender of the bid/ask
//        trustchain.createAcceptTxProposalBlock(payload.primaryCurrency,payload.secondaryCurrency,
//            payload.amount?.toFloat(),payload.price?.toFloat(),payload.type, payload.publicKey)

        (TrustChainTraderActivity.PayloadsList).acceptedPayloads.add(0, payload)

        if (type == 0) {
            updateWallet(payload.amount!!, payload.price!!, type)
        } else if (type == 1) {
            updateWallet(payload.price!!, payload.amount!!, type)
        }
    }
    private fun updateWallet(DD: Double, BTC: Double, type: Int) {
        if (type == 0) {
            amountDD += DD
            amountBTC -= BTC
        } else if (type == 1) {
            amountDD -= DD
            amountBTC += BTC
        }
        amountDD = String.format("%.2f", amountDD).toDouble()
        amountBTC = String.format("%.2f", amountBTC).toDouble()
        amountFieldDD.text = amountDD.toString()
        amountFieldBTC.text = amountBTC.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        val marketCommunity = getMarketCommunity()
        marketCommunity.removeListener(TradePayload.Type.ASK, ::askListener)
        marketCommunity.removeListener(TradePayload.Type.BID, ::bidListener)
    }
}
