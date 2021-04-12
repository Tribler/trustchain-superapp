package nl.tudelft.trustchain.payloadgenerator.ui.payload

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_payload.*
import kotlinx.coroutines.*
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.trustchain.common.constants.Currency
import nl.tudelft.trustchain.common.messaging.TradePayload
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.payloadgenerator.R
import nl.tudelft.trustchain.payloadgenerator.databinding.FragmentPayloadBinding
import nl.tudelft.trustchain.payloadgenerator.ui.TrustChainPayloadGeneratorActivity
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * A fragment representing a list of Payloads.
 */
class PayloadFragment : BaseFragment(R.layout.fragment_payload) {

    private val adapter = ItemAdapter()
    private var isAutoSending = false

    private val binding: FragmentPayloadBinding by viewBinding(FragmentPayloadBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            PayloadItemRenderer {

                trustchain.createAcceptTxProposalBlock(
                    it.primaryCurrency,
                    it.secondaryCurrency,
                    it.amount?.toFloat(),
                    it.price?.toFloat(),
                    it.type,
                    it.publicKey
                )
                Log.d("PayloadFragment", "TX block send to: ${it.publicKey}!")
            }
        )
    }

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewPayload.adapter = adapter
        binding.recyclerViewPayload.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewPayload.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        switchAutoMessage.setOnClickListener {
            Log.d("SwitchAutoMessage", "button switched")
            if (!isAutoSending) {
                isAutoSending = true
                sendAutoMessages()
            } else {
                isAutoSending = false
            }
        }

        val marketCommunity = getMarketCommunity()
        marketCommunity.addListener(TradePayload.Type.ASK, ::askListener)
        marketCommunity.addListener(TradePayload.Type.BID, ::bidListener)

        var amount = arguments?.getDouble("amount")
        var price = arguments?.getDouble("price")
        val type = arguments?.getString("type")
        if (amount == null) {
            amount = 0.0
        }
        if (price == null) {
            price = 0.0
        }
        if (amount != 0.0 && price != 0.0) {
            val payloadSerializable =
                createPayloadSerializable(amount, price, type)
            val payload = createPayload(amount, price, type)
            (TrustChainPayloadGeneratorActivity.PayloadsList).payloads.add(0, payload)
            recyclerViewPayload.layoutManager!!.smoothScrollToPosition(
                recyclerViewPayload,
                RecyclerView.State(),
                0
            )
            marketCommunity.broadcast(payloadSerializable)
        }

        buttonPayload.setOnClickListener {
            view.findNavController().navigate(R.id.action_payloadFragment_to_payloadCreateFragment)
        }
        loadCurrentPayloads((TrustChainPayloadGeneratorActivity.PayloadsList).payloads)
    }

    private fun sendAutoMessages() {
        thread(start = true) {
            while (isAutoSending) {
                val marketCommunity = getMarketCommunity()
                val r = java.util.Random()
                val typeInt = Random.nextInt(0, 2)
                var type = "Bid"
                var amount = r.nextGaussian() * 15 + 100
                var price = 1.0
                if (typeInt == 1) {
                    type = "Ask"
                    amount = 1.0
                    price = r.nextGaussian() * 15 + 100
                }
                amount = String.format("%.2f", amount).toDouble()
                price = String.format("%.2f", price).toDouble()
                val payloadSerializable =
                    createPayloadSerializable(amount, price, type)
                val payload = createPayload(amount, price, type)
                marketCommunity.broadcast(payloadSerializable)
                Log.d("PayloadFragment", "message send!")
                (TrustChainPayloadGeneratorActivity.PayloadsList).payloads.add(0, payload)
                recyclerViewPayload.layoutManager!!.smoothScrollToPosition(
                    recyclerViewPayload,
                    RecyclerView.State(),
                    0
                )
                Thread.sleep(1000)
            }
        }
    }

    private fun createPayload(
        amount: Double,
        price: Double,
        type: String?
    ): TradePayload {
        var primaryCurrency = Currency.DYMBE_DOLLAR
        var secondaryCurrency = Currency.BTC
        var type2 = TradePayload.Type.BID
        if (type.equals("Ask")) {
            primaryCurrency = Currency.BTC
            secondaryCurrency = Currency.DYMBE_DOLLAR
            type2 = TradePayload.Type.ASK
        }
        return TradePayload(
            trustchain.getMyPublicKey(),
            primaryCurrency,
            secondaryCurrency,
            amount,
            price,
            type2
        )
    }

    private fun createPayloadSerializable(
        amount: Double,
        price: Double,
        type: String?
    ): Serializable {
        var primaryCurrency = Currency.DYMBE_DOLLAR
        var secondaryCurrency = Currency.BTC
        var type2 = TradePayload.Type.BID
        if (type.equals("Ask")) {
            primaryCurrency = Currency.BTC
            secondaryCurrency = Currency.DYMBE_DOLLAR
            type2 = TradePayload.Type.ASK
        }
        return TradePayload(
            trustchain.getMyPublicKey(),
            primaryCurrency,
            secondaryCurrency,
            amount,
            price,
            type2
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        val marketCommunity = getMarketCommunity()
        marketCommunity.removeListener(TradePayload.Type.ASK, ::askListener)
        marketCommunity.removeListener(TradePayload.Type.BID, ::bidListener)
        isAutoSending = false
    }

    private fun askListener(payload: TradePayload) {
        Log.d(
            "PayloadFragment",
            "New ask came in! They are selling ${payload.amount} ${payload.primaryCurrency}. The price is ${payload.price} ${payload.secondaryCurrency} per ${payload.primaryCurrency}"
        )
        if (!(TrustChainPayloadGeneratorActivity.PayloadsList).payloads.contains(payload)) {
            TrustChainPayloadGeneratorActivity.payloads.add(0, payload)
            recyclerViewPayload.layoutManager!!.smoothScrollToPosition(
                recyclerViewPayload,
                RecyclerView.State(),
                0
            )
        }
    }

    private fun bidListener(payload: TradePayload) {
        Log.d(
            "PayloadFragment",
            "New bid came in! They are asking ${payload.amount} ${payload.primaryCurrency}. The price is ${payload.price} ${payload.secondaryCurrency} per ${payload.primaryCurrency}"
        )
        if (!(TrustChainPayloadGeneratorActivity.PayloadsList).payloads.contains(payload)) {
            (TrustChainPayloadGeneratorActivity.PayloadsList).payloads.add(0, payload)
            recyclerViewPayload.layoutManager!!.smoothScrollToPosition(
                recyclerViewPayload,
                RecyclerView.State(),
                0
            )
        }
    }

    private fun loadCurrentPayloads(payloads: List<TradePayload>) {
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
                adapter.updateItems(items)

                binding.imgEmpty.isVisible = items.isEmpty()

                delay(1000)
            }
        }
    }
}
