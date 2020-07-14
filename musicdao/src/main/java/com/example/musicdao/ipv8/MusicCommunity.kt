package com.example.musicdao.ipv8

import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore

class MusicCommunity(settings: TrustChainSettings,
                     database: TrustChainStore,
                     crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler) {
    override val serviceId = "29384902d2938f34872398758cf7ca9238ccc333"

    class Factory(
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<MusicCommunity>(MusicCommunity::class.java) {
        override fun create(): MusicCommunity {
            return MusicCommunity(settings, database, crawler)
        }
    }

    override fun bootstrap() {
        super.bootstrap()

        // TODO requires testing
        // Discovering should produce a handshake
        for (address in INITIAL_ADDRESSES) {
            walkTo(address)
        }
    }

    companion object {
        // These are initial addresses for some peers that have initial content,
        // in the case that no content can be found on the first run of the app.
        val INITIAL_ADDRESSES: List<IPv4Address> = listOf(
            IPv4Address("143.179.42.88", 13079)
        )
    }
}
