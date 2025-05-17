package nl.tudelft.trustchain.musicdao.core.ipv8.blocks.userTier

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.musicdao.core.ipv8.MusicCommunity
import javax.inject.Inject

class UserTierBlockRepository @Inject constructor(
    private val musicCommunity: MusicCommunity
) {
    fun getBlocksForUser(userPublicKey: String): List<UserTierBlock> {
        return musicCommunity.database.getBlocksWithType(UserTierBlock.BLOCK_TYPE)
            .filter { it.publicKey == userPublicKey }
            .map { toBlock(it) }
    }

    fun create(
        userId: String,
        tier: String,
        validFrom: Long,
        validUntil: Long?
    ): TrustChainBlock? {
        val transaction = mapOf(
            "type" to UserTierBlock.BLOCK_TYPE,
            "userId" to userId,
            "tier" to tier,
            "validFrom" to validFrom,
            "validUntil" to validUntil
        )

        return musicCommunity.createProposalBlock(transaction)
    }

    fun toBlock(block: TrustChainBlock): UserTierBlock {
        val transaction = block.transaction
        return UserTierBlock(
            userId = transaction["userId"] as String,
            tier = transaction["tier"] as String,
            validFrom = (transaction["validFrom"] as Number).toLong(),
            validUntil = (transaction["validUntil"] as? Number)?.toLong()
        )
    }
}
