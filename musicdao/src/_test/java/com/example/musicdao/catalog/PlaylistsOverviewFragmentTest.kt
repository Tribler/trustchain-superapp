package com.example.musicdao.catalog

import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert
import org.junit.Test
import java.util.*

class PlaylistsOverviewFragmentTest {
    private val privateKey =
        JavaCryptoProvider.keyFromPrivateBin("4c69624e61434c534b3a069c289bd6031de93d49a8c35c7b2f0758c77c7b24b97842d08097abb894d8e98ba8a91ebc063f0687909f390b7ed9ec1d78fcc462298b81a51b2e3b5b9f77f2".hexToBytes())
    private val bitcoinPublicKey = "some-key"
    private val wellStructuredBlock = TrustChainBlock(
        "publish_release",
        TransactionEncoding.encode(
            mapOf(
                "magnet" to "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c",
                "title" to "title",
                "artists" to "artists",
                "date" to "date",
                "torrentInfoName" to "torrentInfoName",
                "publisher" to bitcoinPublicKey
            )
        ),
        privateKey.pub().keyToBin(),
        GENESIS_SEQ,
        ANY_COUNTERPARTY_PK,
        UNKNOWN_SEQ,
        GENESIS_HASH,
        EMPTY_SIG,
        Date(0)
    )

    private val wrongStructuredBlock = TrustChainBlock(
        "publish_release",
        TransactionEncoding.encode(
            mapOf(
                "magnet" to "",
                "title" to "",
                "artists" to "",
                "date" to "",
                "torrentInfoName" to ""
            )
        ),
        privateKey.pub().keyToBin(),
        GENESIS_SEQ,
        ANY_COUNTERPARTY_PK,
        UNKNOWN_SEQ,
        GENESIS_HASH,
        EMPTY_SIG,
        Date(0)
    )

    @Test
    fun showAllReleases() {
        val releaseOverviewFragment = PlaylistsOverviewFragment()
        val releaseBlocks = mapOf<TrustChainBlock, Int>(
            wellStructuredBlock to 0,
            wrongStructuredBlock to 0
        )
        Assert.assertEquals(
            1,
            releaseOverviewFragment.refreshReleaseBlocks(releaseBlocks)
        )
    }
}
