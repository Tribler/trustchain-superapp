package nl.tudelft.trustchain.offlinemoney.ui

import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinemoney.R
import nl.tudelft.trustchain.offlinemoney.databinding.TransactionsFragmentBinding

class TransactionsFragment : OfflineDigitalEuroBaseFragment(R.layout.transactions_fragment) {
    private val binding by viewBinding(TransactionsFragmentBinding::bind)
}
