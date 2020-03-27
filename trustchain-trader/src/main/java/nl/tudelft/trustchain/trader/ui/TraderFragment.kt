package nl.tudelft.trustchain.trader.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.trader.R
import nl.tudelft.trustchain.trader.constants.BlockType
import nl.tudelft.trustchain.trader.validators.DDValidator

@ExperimentalUnsignedTypes
class TraderFragment : BaseFragment(R.layout.fragment_trader) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenStarted {
            val trustchain = getTrustChainCommunity()
            trustchain.registerTransactionValidator(BlockType.DEMO_TX_BLOCK.value, DDValidator())

            trustchain.addListener(BlockType.DEMO_TX_BLOCK.value, object : BlockListener {
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
