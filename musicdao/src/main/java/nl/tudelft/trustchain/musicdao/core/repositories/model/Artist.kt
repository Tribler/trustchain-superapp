package nl.tudelft.trustchain.musicdao.core.repositories.model

data class Artist(
    val publicKey: String,
    val bitcoinAddress: String,
    val name: String,
    val biography: String,
    val socials: String,
    val releaseIds: List<String>
)
