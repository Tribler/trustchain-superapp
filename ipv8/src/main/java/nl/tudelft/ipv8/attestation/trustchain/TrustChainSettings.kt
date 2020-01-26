package nl.tudelft.ipv8.attestation.trustchain

data class TrustChainSettings(
    /**
     * The fan-out of the broadcast when a new block is created.
     */
    val broadcastFanout: Int = 25,

    /**
     * How many prior blocks we require before signing a new incoming block.
     */
    val validationRange: Int = 5,

    /**
     * How many blocks at most we allow others to crawl in one batch.
     */
    val maxCrawlBatch: Int = 10
)
