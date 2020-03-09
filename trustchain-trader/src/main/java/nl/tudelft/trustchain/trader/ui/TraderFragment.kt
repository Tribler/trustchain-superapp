package nl.tudelft.trustchain.trader.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.trader.R

class TraderFragment : BaseFragment(R.layout.fragment_trader) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenStarted {
            val trustchain = getTrustChainCommunity()

            trustchain.registerTransactionValidator("demo_tx_block", object : TransactionValidator {
                override fun validate(
                    block: TrustChainBlock,
                    database: TrustChainStore
                ): Boolean {
                    return block.transaction["amount"] != null
                }
            })

            trustchain.addListener("demo_tx_block", object : BlockListener {
                override fun onBlockReceived(block: TrustChainBlock) {
                    Log.d(
                        "TrustChainDemo",
                        "onBlockReceived: ${block.blockId} ${block.transaction}"
                    )
                }
            })
        }
    }
}
