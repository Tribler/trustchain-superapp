package nl.tudelft.trustchain.musicdao.core.ipv8.blocks.artist_announce

import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction

data class ArtistAnnounceBlock(
    val publicKey: String,
    val bitcoinAddress: String,
    val name: String,
    val biography: String,
    val socials: String,
    val protocolVersion: String,
) {
    fun toTrustChainTransaction(): MutableMap<String, String> {
        return mutableMapOf(
            "publicKey" to publicKey,
            "bitcoinAddress" to bitcoinAddress,
            "name" to name,
            "biography" to biography,
            "socials" to socials,
            "protocolVersion" to socials
        )
    }

    companion object {
        val BLOCK_TYPE = "artist_announce"

        fun fromTrustChainTransaction(transaction: TrustChainTransaction): ArtistAnnounceBlock {
            return ArtistAnnounceBlock(
                publicKey = transaction["publicKey"] as String,
                bitcoinAddress = transaction["bitcoinAddress"] as String,
                name = transaction["name"] as String,
                biography = transaction["biography"] as String,
                socials = transaction["socials"] as String,
                protocolVersion = transaction["protocolVersion"] as String
            )
        }
    }
}
