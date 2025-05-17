package nl.tudelft.trustchain.musicdao.core.services

import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.userTier.UserTierBlockRepository
import javax.inject.Inject

class UserTierService @Inject constructor(
    private val userTierBlockRepository: UserTierBlockRepository
) {
    suspend fun upgradeToPro(userId: String, durationMonths: Int? = null): Boolean {
        val currentTime = System.currentTimeMillis()
        val validUntil = durationMonths?.let {
            currentTime + (it * 30L * 24L * 60L * 60L * 1000L) // Convert months to milliseconds
        }

        val block = userTierBlockRepository.create(
            userId = userId,
            tier = "PRO",
            validFrom = currentTime,
            validUntil = validUntil
        )

        return block != null
    }

    suspend fun downgradeToBasic(userId: String): Boolean {
        val currentTime = System.currentTimeMillis()
        
        val block = userTierBlockRepository.create(
            userId = userId,
            tier = "BASIC",
            validFrom = currentTime,
            validUntil = null
        )

        return block != null
    }
} 