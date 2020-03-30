package nl.tudelft.trustchain.payloadgenerator.ui

import nl.tudelft.trustchain.common.ui.BaseFragment
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.fragment_payload.*
import nl.tudelft.trustchain.common.constants.Currency
import nl.tudelft.trustchain.common.messaging.TradePayload
import nl.tudelft.trustchain.payloadgenerator.R.layout.fragment_payload
import nl.tudelft.trustchain.payloadgenerator.util.getMyPublicKey

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [PayloadFragment.OnListFragmentInteractionListener] interface.
 */
class PayloadFragment : BaseFragment(fragment_payload) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val marketCommunity = getMarketCommunity()
        val trustchain = getTrustChainCommunity()
        marketCommunity.addListener(TradePayload.Type.ASK, ::askListener)

        buttonask.setOnClickListener {
            marketCommunity.broadcast(
                TradePayload(
                    trustchain.getMyPublicKey(),
                    Currency.BTC,
                    Currency.DYMBE_DOLLAR,
                    10.0,
                    5.0,
                    TradePayload.Type.ASK
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val marketCommunity = getMarketCommunity()
        marketCommunity.removeListener(TradePayload.Type.ASK, ::askListener)
    }

    private fun askListener(payload: TradePayload) {
        Log.d(
            "PayloadFragment::onViewCreated",
            "New ask came in! They are selling ${payload.amount} ${payload.primaryCurrency}. The price is ${payload.price} ${payload.secondaryCurrency} per ${payload.primaryCurrency}"
        )
    }
}
