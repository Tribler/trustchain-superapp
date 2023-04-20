package nl.tudelft.trustchain.detoks_engine.trustchain

import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore

class TrustChainTransactionCommunity(
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler) {

    override val serviceId = "12315685d1932a144279f8248fc3db5899c5df8c"

    class Factory(
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<TrustChainTransactionCommunity>(TrustChainTransactionCommunity::class.java) {
        override fun create(): TrustChainTransactionCommunity {
            return TrustChainTransactionCommunity(settings, database, crawler)
        }
    }

}
