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

    private var ReceiverName=""
    private var ReceiverPublic=""
    private var Amount=""
    private var Message: String? = null

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

    fun setData(receiver: String?,amount: String,message: String?,public: String)
    {
        if (receiver != null) {
            this.ReceiverName= receiver
        }
        this.ReceiverPublic=public
        this.Amount=amount
        this.Message=message
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        onResume()

        binding.tvPaymentReceiver.text = this.ReceiverName
        binding.tvPaymentAmount.text = this.Amount
        binding.tvPaymentMessage.text = this.Message?:""
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
