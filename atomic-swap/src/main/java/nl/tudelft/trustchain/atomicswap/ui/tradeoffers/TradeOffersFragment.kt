package nl.tudelft.trustchain.atomicswap.ui.tradeoffers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import nl.tudelft.trustchain.atomicswap.AtomicSwapActivity
import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.atomicswap.databinding.FragmentAtomicTradeOffersBinding
import nl.tudelft.trustchain.common.ui.BaseFragment

class TradeOffersFragment : BaseFragment(R.layout.fragment_atomic_trade_offers) {

    private val parentActivity get() = activity as AtomicSwapActivity

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

        initializeUi(binding)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeUi(binding: FragmentAtomicTradeOffersBinding) {
        binding.tradeOffersRecyclerView.adapter = parentActivity.tradeOffersAdapter

        binding.refreshTradeOffersButton.setOnClickListener {
            refreshTradeOffers()
        }
    }

    private fun refreshTradeOffers() {
        Toast.makeText(requireContext(), R.string.toast_refreshing_trade_offers, Toast.LENGTH_SHORT)
            .show()
        parentActivity.refreshTradeOffers()
    }

}
