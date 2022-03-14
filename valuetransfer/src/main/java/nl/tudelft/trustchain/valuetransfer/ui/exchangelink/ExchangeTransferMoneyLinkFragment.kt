package nl.tudelft.trustchain.valuetransfer.ui.exchangelink

import android.os.Bundle
import android.view.*
import androidx.lifecycle.*
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentExchangeTransferMoneyLinkBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.*
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment

class ExchangeTransferMoneyLinkFragment : VTFragment(R.layout.fragment_exchange_transfer_money_link) {
    private val binding by viewBinding(FragmentExchangeTransferMoneyLinkBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exchange_transfer_money_link, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun initView() {
        parentActivity.apply {
            setActionBarTitle(
                "Payment",
                null
            )
            toggleActionBar(true)
            toggleBottomNavigation(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        onResume()

        binding.tvPaymentReceiver.text = "John Doe"
        binding.tvPaymentAmount.text = "10.50"
        binding.tvPaymentMessage.text = "Pizza party"
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onBackPressed(animated: Boolean = true) {
        val previousFragment = parentFragmentManager.fragments.filter {
            it.tag == ValueTransferMainActivity.walletOverviewFragmentTag
        }

        parentFragmentManager.beginTransaction().apply {
            if (animated) setCustomAnimations(0, R.anim.exit_to_right)
            hide(this@ExchangeTransferMoneyLinkFragment)
            if (animated) setCustomAnimations(R.anim.enter_from_left, 0)
            show(previousFragment[0])
        }.commit()

        (previousFragment[0] as VTFragment).initView()
    }
}
