package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.view.View
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentTestBinding

class TestFragment : BaseFragment(R.layout.fragment_test) {

    private val binding by viewBinding(FragmentTestBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var community: OurCommunity
    private lateinit var trustchainCommunity: TrustChainCommunity

    private val BLOCK_TYPE = "our_test_block"

    private var index = 0;

    private fun debugLog(txt: String) {
        val textView = binding.debugTextView;
        textView.text = textView.text.toString() + "${txt}\n"
    }

    private fun createProposal(recipient: Peer) {
        val transaction = mapOf("msg" to index)
        debugLog("Proposing block: ${transaction["msg"]} to ${recipient.key.keyToBin().toHex().take(10)}...")
        trustchainCommunity.createProposalBlock(
            BLOCK_TYPE,
            transaction,
            recipient.publicKey.keyToBin()
        )
        index += 1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        community = IPv8Android.getInstance().getOverlay()!!
        trustchainCommunity = IPv8Android.getInstance().getOverlay()!!
        ipv8 = IPv8Android.getInstance()

        binding.debugClearButton.setOnClickListener {
            binding.debugTextView.text = ""
        }

        binding.button1.setOnClickListener {
            createProposal(ipv8.myPeer)
        }

        trustchainCommunity.addListener(BLOCK_TYPE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {

                debugLog("Received block ${block.transaction["msg"]}")

                // TODO validate integrity...

                // if valid, create agreement block

            }
        })

        trustchainCommunity.registerTransactionValidator(BLOCK_TYPE, object : TransactionValidator {
            override fun validate(
                block: TrustChainBlock,
                database: TrustChainStore
            ): ValidationResult {

                if (block.isProposal) {
                    debugLog("received a proposal to validate from ${block.publicKey.toHex().take(10)}...")
                    return ValidationResult.Valid
                }

                return ValidationResult.Invalid(listOf("Unexpected block"));
            }
        })

        trustchainCommunity.registerBlockSigner(BLOCK_TYPE, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                debugLog("create agreement block")
                trustchain.createAgreementBlock(block, block.transaction)
            }
        })


    }
}
