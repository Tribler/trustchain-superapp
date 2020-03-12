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
import nl.tudelft.ipv8.util.toHex
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

            val currentBalance = trustchain.database.getBalance(trustchain.getMyPublicKey())
            txtBalance.text = "Your balance: ${currentBalance}"

            trustchain.registerTransactionValidator(
                BlockTypes.DEMO_TX_BLOCK.value,
                object : TransactionValidator {
                    override fun validate(
                        block: TrustChainBlock,
                        database: TrustChainStore
                    ): Boolean {
                        // Self signed blocks print money, they are always valid
                        if (block.isSelfSigned) {
                            return true
                        }

                        val amount = block.getAmount()

                        if (block.isProposal) {
                            val balance =
                                database.getBalance(block.publicKey, block.sequenceNumber - 1u)
                            return balance > amount
                        } else {
                            // Is agreement block
                            val proposalBlock = database.getLinked(block) ?: return false
                            if (block.transaction == proposalBlock.transaction) {
                                return true
                            }
                            val balance =
                                database.getBalance(block.linkPublicKey, block.sequenceNumber - 1u)
                            return balance > amount
                        }
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
