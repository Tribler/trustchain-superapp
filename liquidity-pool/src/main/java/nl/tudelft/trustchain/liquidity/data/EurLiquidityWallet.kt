package nl.tudelft.trustchain.liquidity.data

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository

class EurLiquidityWallet(private val transactionRepository: TransactionRepository, private val publicKey: PublicKey) : LiquidityWallet {

    private lateinit var pool: LiquidityPool
    override val coinName: String = "BTC"

    /**
     * Register listeners for when the owner receives a Join/Transaction block.
     */
    override fun initializePool(/*pool: LiquidityPool*/) {
        transactionRepository.trustChainCommunity.addListener(TransactionRepository.BLOCK_TYPE_JOIN, object :
            BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("EuroTokenBlock", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
        // TODO: What happens when we receive a transaction? -> Trade for Bitcoin
        transactionRepository.trustChainCommunity.addListener(TransactionRepository.BLOCK_TYPE_TRANSFER, object :
            BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                Log.d("EuroTokenBlock", "onBlockReceived: ${block.blockId} ${block.transaction}")
            }
        })
    }

    // TODO: Properly convert amount in double to long or the other way around
    override fun startTransaction(amount: Double, address: String) {

    }

}


