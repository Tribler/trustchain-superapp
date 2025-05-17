package nl.tudelft.trustchain.musicdao.core.ipv8.blocks.userTier

data class UserTierBlock(
    val userId: String,
    val tier: String,  // "PRO" or "BASIC"
    val validFrom: Long,
    val validUntil: Long?  // null for permanent tiers
) {
    companion object {
        const val BLOCK_TYPE = "user_tier"
    }
} 