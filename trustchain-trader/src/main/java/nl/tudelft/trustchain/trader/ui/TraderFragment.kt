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
import androidx.recyclerview.widget.RecyclerView
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_trader.*
import kotlinx.coroutines.*
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
import java.lang.Exception
import kotlin.math.roundToInt

@ExperimentalUnsignedTypes
class TraderFragment : BaseFragment(R.layout.fragment_trader) {

    private val adapterAccepted = ItemAdapter()
    private val adapterDeclined = ItemAdapter()
    private var isTrading = true
    lateinit var ai: NaiveBayes

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
        }
        loadCurrentPayloads((TrustChainTraderActivity.acceptedPayloads), "accepted")
        loadCurrentPayloads((TrustChainTraderActivity.declinedPayloads), "declined")
        ai = NaiveBayes(resources.openRawResource(R.raw.ai_trading_data))
    }

    private fun loadCurrentPayloads(
        payloads: List<TradePayload>,
        adapterString: String
    ) {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                try {
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
                        val adapterCount = adapterAccepted.itemCount
                        adapterAccepted.updateItems(items)
                        if (adapterCount != adapterAccepted.itemCount) {
                            acceptedPayloads.layoutManager!!.smoothScrollToPosition(acceptedPayloads, RecyclerView.State(), 0)
                        }
                    } else if (adapterString == "declined") {
                        val adapterCount = adapterDeclined.itemCount
                        adapterDeclined.updateItems(items)
                        if (adapterCount != adapterDeclined.itemCount) {
                            declinedPayloads.layoutManager!!.smoothScrollToPosition(
                                declinedPayloads,
                                RecyclerView.State(),
                                0
                            )
                        }
                    }
                    if (isTrading) {
                        binding.imgEmpty.isVisible = false
                        binding.loadingLayout.isVisible =
                            items.isEmpty() && (TrustChainTraderActivity.acceptedPayloads).isEmpty() && (TrustChainTraderActivity.declinedPayloads).isEmpty()
                    } else {
                        binding.loadingLayout.isVisible = false
                        binding.imgEmpty.isVisible =
                            items.isEmpty() && (TrustChainTraderActivity.acceptedPayloads).isEmpty() && (TrustChainTraderActivity.declinedPayloads).isEmpty()
                        delay(1000)
                    }
                }catch (e: Exception) {
                    Log.d("LoadCurrentPayloads exception", e.toString())
                }
            }
        }
    }

    fun askListener(payload: TradePayload) {
        Log.d(
            "PayloadFragment::onViewCreated",
            "New ask came in! They are selling ${payload.amount} ${payload.primaryCurrency}. The price is ${payload.price} ${payload.secondaryCurrency} per ${payload.primaryCurrency}"
        )
        val receivedPayload = TradePayload(payload.publicKey, payload.secondaryCurrency,
            payload.primaryCurrency, payload.price, payload.amount, payload.type)
        if (isTrading) {
            val type = ai.predict(payload.price!!.roundToInt() / payload.amount!!.roundToInt())
            if (type == 0) {
                accept(receivedPayload, type)
            } else if (type == 2) {
                val price = round(payload.price!!.roundToInt() / payload.amount!!.roundToInt())
                if (ai.predict(price) == 0) {
                    Log.d(
                        "PayloadFragment::onViewCreated",
                        "Accepted!"
                    )
                    accept(receivedPayload, 0)
                }
            } else {
                (TrustChainTraderActivity.PayloadsList).declinedPayloads.add(0, receivedPayload)
                if((TrustChainTraderActivity.PayloadsList).declinedPayloads.lastIndex>25){
                    (TrustChainTraderActivity.PayloadsList).declinedPayloads.removeAt((TrustChainTraderActivity.PayloadsList).declinedPayloads.lastIndex)
                }
            }
        }
    }

    fun bidListener(payload: TradePayload) {
        Log.d(
            "PayloadFragment::onViewCreated",
            "New bid came in! They are asking ${payload.amount} ${payload.primaryCurrency}. The price is ${payload.price} ${payload.secondaryCurrency} per ${payload.primaryCurrency}"
        )
        val receivedPayload = TradePayload(payload.publicKey, payload.secondaryCurrency,
            payload.primaryCurrency, payload.price, payload.amount, payload.type)
        if (isTrading) {
            val type = ai.predict(payload.amount!!.roundToInt() / payload.price!!.roundToInt())
            if (type == 1) {
                accept(receivedPayload, type)
            } else if (type == 2) {
                val price = round(payload.amount!!.roundToInt() / payload.price!!.roundToInt())
                if (ai.predict(price) == 1) {
                    accept(receivedPayload, 1)
                }
            } else {
                (TrustChainTraderActivity.PayloadsList).declinedPayloads.add(0, receivedPayload)
                if((TrustChainTraderActivity.PayloadsList).declinedPayloads.lastIndex>25){
                    (TrustChainTraderActivity.PayloadsList).declinedPayloads.removeAt((TrustChainTraderActivity.PayloadsList).declinedPayloads.lastIndex)
                }
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
        if((TrustChainTraderActivity.PayloadsList).acceptedPayloads.lastIndex>25){
            (TrustChainTraderActivity.PayloadsList).acceptedPayloads.removeAt((TrustChainTraderActivity.PayloadsList).acceptedPayloads.lastIndex)
        }

        if (type == 1) {
            updateWallet(payload.price!!, payload.amount!!, type)
        } else if (type == 0) {
            updateWallet(payload.amount!!, payload.price!!, type)
        }
    }
    private fun updateWallet(DD: Double, BTC: Double, type: Int) {
        if (type == 0) {
            amountDD -= DD
            amountBTC += BTC
        } else if (type == 1) {
            amountDD += DD
            amountBTC -= BTC
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
