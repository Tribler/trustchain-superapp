package nl.tudelft.trustchain.musicdao.core.ipv8

import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.userTier.UserTierBlock
import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.userTier.UserTierBlockRepository
import javax.inject.Inject

class UserTierVerifier @Inject constructor(
    private val userTierBlockRepository: UserTierBlockRepository
) {
    fun isProUser(userPublicKey: String): Boolean {
        val userTierBlocks = userTierBlockRepository.getBlocksForUser(userPublicKey)
        
        // Get the most recent valid tier block
        val currentTime = System.currentTimeMillis()
        val validTierBlock = userTierBlocks
            .filter { it.validFrom <= currentTime && (it.validUntil == null || it.validUntil > currentTime) }
            .maxByOrNull { it.validFrom }
            
        return validTierBlock?.tier == "PRO"
    }
} 