package nl.tudelft.trustchain.atomicswap.ui.tradeoffers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.atomicswap.databinding.FragmentAtomicTradeOffersBinding
import nl.tudelft.trustchain.common.ui.BaseFragment

class TradeOffersFragment : BaseFragment(R.layout.fragment_atomic_trade_offers) {

    private var _binding: FragmentAtomicTradeOffersBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAtomicTradeOffersBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
