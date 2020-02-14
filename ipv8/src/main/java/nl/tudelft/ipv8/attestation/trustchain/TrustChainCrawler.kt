package nl.tudelft.ipv8.attestation.trustchain

import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore

private val logger = KotlinLogging.logger {}

class TrustChainCrawler {
    lateinit var trustChainCommunity: TrustChainCommunity
    private val database: TrustChainStore
        get() {
            return trustChainCommunity.database
        }

    private val chainCrawlCache: MutableMap<Peer, ChainCrawl> = mutableMapOf()

    /**
     * Crawl the whole chain of a specific peer.
     *
     * @param latestBlockNum The latest block number of the peer in question, if available.
     */
    suspend fun crawlChain(peer: Peer, latestBlockNum: UInt? = null) {
        if (chainCrawlCache[peer] != null) {
            logger.info { "Skipping crawl of peer $peer, another crawl is pending" }
            return
        }

        withTimeout(CHAIN_CRAWL_TIMEOUT) {
            val chainCrawl = ChainCrawl(
                peer,
                latestBlockNum
            )
            chainCrawlCache[peer] = chainCrawl
            sendNextPartialChainCrawlRequest(chainCrawl)
        }

        chainCrawlCache.remove(peer)
    }

    /**
     * Send the next partial crawl request, if we are not done yet.
     */
    private suspend fun sendNextPartialChainCrawlRequest(crawl: ChainCrawl) {
        val lowestUnknown = database.getLowestSequenceNumberUnknown(crawl.peer.publicKey.keyToBin())

        if (crawl.knownChainLength != null) {
            if (crawl.knownChainLength + 1u != lowestUnknown.toUInt()) {
                // We know we are still missing some blocks
                val latestBlock = database.getLatest(crawl.peer.publicKey.keyToBin())
                if (latestBlock == null) {
                    // We have no knowledge of this peer but we have the length of the chain.
                    // Simply send a request from the genesis block to the known chain length.
                    performPartialChainCrawl(crawl, LongRange(1L, crawl.knownChainLength.toLong()))
                } else if (lowestUnknown.toUInt() == latestBlock.sequenceNumber + 1u) {
                    // It seems that we filled all gaps in the database
                    if (latestBlock.sequenceNumber < crawl.knownChainLength) {
                        // Check whether we can do one final request if we still don't have the
                        // whole chain.
                        val crawlRange = LongRange(
                            latestBlock.sequenceNumber.toLong() + 1,
                            crawl.knownChainLength.toLong()
                        )
                        performPartialChainCrawl(crawl, crawlRange)
                    }
                    return
                }

                val lowestRangeUnknown = database.getLowestRangeUnknown(crawl.peer.publicKey.keyToBin())
                performPartialChainCrawl(crawl, lowestRangeUnknown)
            }
        } else {
            // Do we know the chain length of the crawled peer? If not, make sure we get to know
            // this first.
            // TODO: do we really need to know this?
            val blocks = trustChainCommunity.sendCrawlRequest(
                crawl.peer,
                crawl.peer.publicKey.keyToBin(),
                LongRange(-1, -1)
            )

            if (blocks.isNotEmpty()) {
                val nextCrawl = crawl.copy(
                    knownChainLength = blocks.first().sequenceNumber
                )
                sendNextPartialChainCrawlRequest(nextCrawl)
            }
        }
    }

    /**
     * Perform a partial crawl request for a specific range.
     */
    private suspend fun performPartialChainCrawl(crawl: ChainCrawl, range: LongRange) {
        val nextCrawl = when {
            crawl.currentRequestRange != range -> {
                // We are performing a new request
                crawl.copy(
                    currentRequestRange = range,
                    currentRequestAttempts = 0
                )
            }
            crawl.currentRequestAttempts < MAX_CRAWL_REQUEST_ATTEMPTS -> {
                // This is a retry
                crawl.copy(
                    currentRequestAttempts = crawl.currentRequestAttempts + 1
                )
            }
            else -> {
                // We already tried the same request three times, bail out
                return
            }
        }

        trustChainCommunity.sendCrawlRequest(crawl.peer, crawl.peer.publicKey.keyToBin(), range)
        sendNextPartialChainCrawlRequest(nextCrawl)
    }

    companion object {
        private const val CHAIN_CRAWL_TIMEOUT = 120_000L
        private const val MAX_CRAWL_REQUEST_ATTEMPTS = 3
    }

    data class ChainCrawl(
        val peer: Peer,
        val knownChainLength: UInt?,
        val currentRequestRange: LongRange? = null,
        var currentRequestAttempts: Int = 0
    )
}
