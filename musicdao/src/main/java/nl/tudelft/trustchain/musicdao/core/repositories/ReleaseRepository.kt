package nl.tudelft.trustchain.musicdao.core.repositories

import nl.tudelft.trustchain.musicdao.core.ipv8.ReleaseRequestHandler
import nl.tudelft.trustchain.musicdao.core.ipv8.messages.ReleaseRequestMessage
import javax.inject.Inject

class ReleaseRepository @Inject constructor(
    private val releaseRequestHandler: ReleaseRequestHandler
) {
    suspend fun getFullRelease(releaseId: String, userPublicKey: String): String? {
        val request = ReleaseRequestMessage(
            releaseId = releaseId,
            userPublicKey = userPublicKey
        )

        val response = releaseRequestHandler.handleRequest(request)
        return response.magnetLink
    }
}
