package nl.tudelft.trustchain.trader.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_trader.*
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.trader.R
import nl.tudelft.trustchain.trader.constants.BlockTypes
import nl.tudelft.trustchain.trader.util.getAmount
import nl.tudelft.trustchain.trader.util.getBalance
import nl.tudelft.trustchain.trader.util.getMyPublicKey

@ExperimentalUnsignedTypes
class TraderFragment : BaseFragment(R.layout.fragment_trader) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launchWhenStarted {
            val trustchain = getTrustChainCommunity()

            trustchain.registerTransactionValidator(
                BlockTypes.DEMO_TX_BLOCK.value,
                object : TransactionValidator {
                    override fun validate(
                        block: TrustChainBlock,
                        database: TrustChainStore
                    ): Boolean {
                        // Do not validate offline transactions
                        val offline = block.transaction["offline"]?.toString()?.toBoolean()
                        if (offline != null && offline) {
                            return true
                        }

                        // Self signed blocks print money, they are always valid
                        if (block.isSelfSigned) {
                            return true
                        }

                        val amount = block.getAmount()

                        if (block.isProposal) {
                            val balance =
                                database.getBalance(block.linkPublicKey, block.linkSequenceNumber - 1u)
                            return balance > amount
                        } else if (block.isAgreement){
                            val balance =
                                database.getBalance(block.publicKey, block.sequenceNumber - 1u)
                            return balance > amount
                        }
                        return false
                    }
                })

            trustchain.addListener(BlockTypes.DEMO_TX_BLOCK.value, object : BlockListener {
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
