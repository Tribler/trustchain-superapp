package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.security.KeyChain.getPrivateKey
import android.util.Log
import nl.tudelft.ipv8.IPv8Configuration
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult

class DeToksActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_detoks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar

        actionBar!!.hide()
//        initIPv8()
    }

//    private fun initIPv8() {
//        val config = IPv8Configuration(overlays = listOf(
//            createDiscoveryCommunity(),
//            createDemoCommunity()
//        ), walkerInterval = 5.0)
//
//        IPv8Android.Factory(this)
//            .setConfiguration(config)
//            .setPrivateKey(getPrivateKey())
//            .setServiceClass(TrustChainService::class.java)
//            .init()
//
//        initTrustChain()
//    }
//
//    private fun initTrustChain() {
//        val ipv8 = IPv8Android.getInstance()
//        val trustchain = ipv8.getOverlay<DetoksCommunity>()!!
//
//        trustchain.registerTransactionValidator(BLOCK_TYPE, object : TransactionValidator {
//            override fun validate(
//                block: TrustChainBlock,
//                database: TrustChainStore
//            ): ValidationResult {
//                if (block.transaction["message"] != null || block.isAgreement) {
//                    return ValidationResult.Valid
//                } else {
//                    return ValidationResult.Invalid(listOf(""))
//                }
//            }
//        })
//
//        trustchain.registerBlockSigner(BLOCK_TYPE, object : BlockSigner {
//            override fun onSignatureRequest(block: TrustChainBlock) {
//                trustchain.createAgreementBlock(block, mapOf<Any?, Any?>())
//            }
//        })
//
//        trustchain.addListener(BLOCK_TYPE, object : BlockListener {
//            override fun onBlockReceived(block: TrustChainBlock) {
//                Log.d("DeToks", "onBlockReceived: ${block.blockId} ${block.transaction}")
//            }
//        })
//    }
}
