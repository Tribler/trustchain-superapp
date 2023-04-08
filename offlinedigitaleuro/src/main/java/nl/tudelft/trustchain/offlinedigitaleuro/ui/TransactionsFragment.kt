package nl.tudelft.trustchain.offlinedigitaleuro.ui

import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.offlinedigitaleuro.R
import nl.tudelft.trustchain.offlinedigitaleuro.databinding.TransactionsFragmentBinding

class TransactionsFragment : OfflineDigitalEuroBaseFragment(R.layout.transactions_fragment) {
    private val binding by viewBinding(TransactionsFragmentBinding::bind)
}
