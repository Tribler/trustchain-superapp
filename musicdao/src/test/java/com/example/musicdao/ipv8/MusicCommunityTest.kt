package com.example.musicdao.ipv8

import com.frostwire.jlibtorrent.Sha1Hash
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.sqldelight.Database

import org.junit.Assert
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class MusicCommunityTest {
    private fun createTrustChainStore(): TrustChainSQLiteStore {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        val database = Database(driver)
        return TrustChainSQLiteStore(database)
    }

    private fun getCommunity(): MusicCommunity {
        val settings = TrustChainSettings()
        val store = createTrustChainStore()
        val community = MusicCommunity.Factory(settings = settings, database = store).create()
        val newKey = JavaCryptoProvider.generateKey()
        community.myPeer = Peer(newKey)
        community.endpoint = spyk(EndpointAggregator(mockk(relaxed = true), null))
        community.network = Network()
        community.maxPeers = 20
        return community
    }

    @Test
    fun localKeywordSearch() = runBlockingTest {
        val crawler = TrustChainCrawler()
        val trustChainCommunity = spyk(getCommunity())
        crawler.trustChainCommunity = trustChainCommunity

        val newKey = JavaCryptoProvider.generateKey()

        val block = TrustChainBlock(
            "publish_release",
            TransactionEncoding.encode(
                mapOf(
                    "magnet" to "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c",
                    "title" to "title",
                    "artists" to "artists",
                    "date" to "date",
                    "torrentInfoName" to "torrentInfoName"
                )
            ),
            newKey.pub().keyToBin(),
            1u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        trustChainCommunity.database.addBlock(block)

        val peer = Peer(newKey)
        crawler.crawlChain(peer, 1u)

        Assert.assertEquals(1, trustChainCommunity.database.getAllBlocks().size)

        Assert.assertNotNull(trustChainCommunity.localKeywordSearch("title"))
        Assert.assertNotNull(trustChainCommunity.localKeywordSearch("artists"))
        Assert.assertNull(trustChainCommunity.localKeywordSearch("somethingelse"))
    }

    @Test
    fun performRemoteKeywordSearch() {
        val trustChainCommunity = spyk(getCommunity())

        val newKey2 = JavaCryptoProvider.generateKey()
        val neighborPeer = Peer(newKey2)
        every {
            trustChainCommunity.getPeers()
        } returns listOf(neighborPeer)

        val count = trustChainCommunity.performRemoteKeywordSearch("keyword", 1u, ANY_COUNTERPARTY_PK)
        Assert.assertEquals(count, 1)
    }

    @Test
    fun sendSwarmHealthMessage() {
        val musicCommunity = spyk(getCommunity())
        val swarmHealth = SwarmHealth(Sha1Hash.max().toString(), 1.toUInt(), 0.toUInt())
        Assert.assertFalse(musicCommunity.sendSwarmHealthMessage(swarmHealth))
        val newKey = JavaCryptoProvider.generateKey()
        val neighborPeer = Peer(newKey)
        every {
            musicCommunity.getPeers()
        } returns listOf(neighborPeer)
        Assert.assertTrue(musicCommunity.sendSwarmHealthMessage(swarmHealth))
        every {
            musicCommunity.getPeers()
        } returns listOf(neighborPeer, neighborPeer)
        Assert.assertTrue(musicCommunity.sendSwarmHealthMessage(swarmHealth))
    }

    @Test
    fun communicateReleaseBlocks() {
        val community = spyk(getCommunity())
        val newKey = JavaCryptoProvider.generateKey()

        val block = TrustChainBlock(
            "publish_release",
            TransactionEncoding.encode(
                mapOf(
                    "magnet" to "magnet:?xt=urn:btih:a83cc13bf4a07e85b938dcf06aa707955687ca7c",
                    "title" to "title",
                    "artists" to "artists",
                    "date" to "date",
                    "torrentInfoName" to "torrentInfoName"
                )
            ),
            newKey.pub().keyToBin(),
            1u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        community.database.addBlock(block)

        // 0 peers: we have 1 block, but no one to send it to
        Assert.assertEquals(community.communicateReleaseBlocks(), 0)
        val newKey2 = JavaCryptoProvider.generateKey()
        val neighborPeer = Peer(newKey2)
        every {
            community.getPeers()
        } returns listOf(neighborPeer)
        // 1 peer: send to 1 person, because we have 1 block
        Assert.assertEquals(community.communicateReleaseBlocks(), 1)
    }
}
