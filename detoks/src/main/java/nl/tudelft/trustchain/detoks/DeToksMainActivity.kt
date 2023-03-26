package nl.tudelft.trustchain.detoks

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.detoks.gossiper.GossiperService
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult


class DeToksActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_detoks
    var gossipService: GossiperService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val trustchain = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar!!.hide()

        trustchain.registerTransactionValidator(DeToksCommunity.BLOCK_TYPE,
            object : TransactionValidator {
                override fun validate(
                    block: TrustChainBlock,
                    database: TrustChainStore
                ): ValidationResult {
                    Log.d(DeToksCommunity.LOGGING_TAG, "validator request ${block.transaction}, proposal: ${block.isProposal}")
                    return ValidationResult.Valid

                }
            }
        )

        trustchain.registerBlockSigner(
            DeToksCommunity.BLOCK_TYPE,
            object : BlockSigner {
                override fun onSignatureRequest(block: TrustChainBlock) {
                    Log.d(DeToksCommunity.LOGGING_TAG, "here?")
                    Log.d(DeToksCommunity.LOGGING_TAG, "sig request ${block.transaction}")

                    trustchain.sendBlock(
                        trustchain.createAgreementBlock(
                            block,
                            block.transaction
                        )
                    )
                }
            }
        )

        trustchain.addListener(DeToksCommunity.BLOCK_TYPE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d(DeToksCommunity.LOGGING_TAG, "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })


        Intent(this, GossiperService::class.java).also { intent ->
            startService(intent)
            bindService(intent, gossipConnection, Context.BIND_AUTO_CREATE)
        }

    }

    private val gossipConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as GossiperService.LocalBinder
            gossipService = binder.getService()
        }
        override fun onServiceDisconnected(p0: ComponentName?) {
            gossipService = null
        }

    }
}
