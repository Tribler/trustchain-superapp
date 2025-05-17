package nl.tudelft.trustchain.musicdao.core.ipv8.messages

data class ReleaseRequestMessage(
    val releaseId: String,
    val userPublicKey: String,  // Used to verify user's tier in TrustChain
    val requestTimestamp: Long = System.currentTimeMillis()
)

data class ReleaseResponseMessage(
    val releaseId: String,
    val magnetLink: String?  // null if not available
) 