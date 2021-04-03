package nl.tudelft.trustchain.liquidity.data

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository

class EurLiquidityWallet(private val transactionRepository: TransactionRepository, private val publicKey: PublicKey) : LiquidityWallet {

    override val coinName: String = "BTC"

    /**
     * Register listeners for when the owner receives a Join/Transaction block.
     */
    override fun initializePool() {
        transactionRepository.trustChainCommunity.addListener(TransactionRepository.BLOCK_TYPE_JOIN, object :
            BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("EuroTokenBlock", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
        transactionRepository.trustChainCommunity.addListener(TransactionRepository.BLOCK_TYPE_TRANSFER, object :
            BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("EuroTokenBlock", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }
}


