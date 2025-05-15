package nl.tudelft.trustchain.musicdao.ui.screens.contribute

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.musicdao.core.repositories.model.Artist
import nl.tudelft.trustchain.musicdao.ui.screens.wallet.BitcoinWalletViewModel

object ContributionPool {
    private val pool: MutableMap<Artist, Double> = mutableMapOf()

    fun addContribution(artist: Artist, amount: Double) {
        pool[artist] = pool.getOrDefault(artist, 0.0) + amount
    }

    fun getPooledAmount(artist: Artist): Double {
        return pool.getOrDefault(artist, 0.0)
    }

    fun clearPool(): Map<Artist, Double> {
        val contributions = pool.toMap()
        pool.clear()
        return contributions
    }

    // Distribute pooled contributions in bulk
    suspend fun distributePooledContributions(bitcoinWalletViewModel: BitcoinWalletViewModel): Boolean {
        val pooledContributions = clearPool()
        for ((artist, totalAmount) in pooledContributions) {
            val success = bitcoinWalletViewModel.donate(artist.publicKey, totalAmount.toString())
            if (!success) return false
        }
        return true
    }
}
