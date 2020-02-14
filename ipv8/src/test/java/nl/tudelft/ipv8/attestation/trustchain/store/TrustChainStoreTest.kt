package nl.tudelft.ipv8.attestation.trustchain.store

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert
import org.junit.Test
import java.util.*

private val lazySodium = LazySodiumJava(SodiumJava())

class TrustChainStoreTest {
    private fun getPrivateKey(): PrivateKey {
        val privateKey = "81df0af4c88f274d5228abb894a68906f9e04c902a09c68b9278bf2c7597eaf6"
        val signSeed = "c5c416509d7d262bddfcef421fc5135e0d2bdeb3cb36ae5d0b50321d766f19f2"
        return LibNaClSK(privateKey.hexToBytes(), signSeed.hexToBytes(), lazySodium)
    }

    private fun createTrustChainStore(): TrustChainSQLiteStore {
        val driver: SqlDriver = JdbcSqliteDriver(IN_MEMORY)
        Database.Schema.create(driver)
        val database = Database(driver)
        return TrustChainSQLiteStore(database)
    }

    @Test
    fun getBlock() {
        val store = createTrustChainStore()

        val block1 = TrustChainBlock(
            "custom1",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            1u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val block2 = TrustChainBlock(
            "custom2",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            2u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val block3 = TrustChainBlock(
            "custom2",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            3u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        store.addBlock(block1)
        store.addBlock(block2)

        val allBlocks = store.getAllBlocks()
        Assert.assertEquals(2, allBlocks.size)

        val blocksWithType = store.getBlocksWithType("custom1")
        Assert.assertEquals(1, blocksWithType.size)
        Assert.assertEquals(1u, blocksWithType[0].sequenceNumber)

        Assert.assertNotNull(store.get(block1.publicKey, block1.sequenceNumber))
        Assert.assertNotNull(store.getBlockWithHash(block1.calculateHash()))
        Assert.assertTrue(store.contains(block1))
        Assert.assertFalse(store.contains(block3))

        Assert.assertEquals(2, store.getLatestBlocks(block1.publicKey).size)

        val users = store.getUsers()
        Assert.assertEquals(1, users.size)
        Assert.assertEquals(2L, users[0].latestSequenceNumber)

        Assert.assertEquals(2, store.getRecentBlocks().size)
    }

    @Test
    fun getBlockBeforeAfter() {
        val store = createTrustChainStore()

        val block1 = TrustChainBlock(
            "custom1",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            1u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val block2 = TrustChainBlock(
            "custom2",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            2u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        store.addBlock(block1)
        store.addBlock(block2)

        Assert.assertEquals(block1.sequenceNumber,
            store.getBlockBefore(block2)?.sequenceNumber)

        Assert.assertEquals(block1.sequenceNumber,
            store.getBlockBefore(block2, "custom1")?.sequenceNumber)

        Assert.assertEquals(block2.sequenceNumber,
            store.getBlockAfter(block1)?.sequenceNumber)

        Assert.assertEquals(block2.sequenceNumber,
            store.getBlockAfter(block1, "custom2")?.sequenceNumber)
    }

    @Test
    fun getLatest() {
        val publicKey = getPrivateKey().pub().keyToBin()

        val block1 = TrustChainBlock(
            "custom1",
            "hello".toByteArray(Charsets.US_ASCII),
            publicKey,
            1u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val block2 = TrustChainBlock(
            "custom1",
            "hello".toByteArray(Charsets.US_ASCII),
            publicKey,
            2u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val block3 = TrustChainBlock(
            "custom2",
            "hello".toByteArray(Charsets.US_ASCII),
            publicKey,
            3u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val store = createTrustChainStore()
        store.addBlock(block1)
        store.addBlock(block2)
        store.addBlock(block3)

        Assert.assertEquals(block3.sequenceNumber,
            store.getLatest(block2.publicKey)?.sequenceNumber)

        Assert.assertEquals(block2.sequenceNumber,
            store.getLatest(block2.publicKey, "custom1")?.sequenceNumber)

        Assert.assertEquals(3, store.getLatestBlocks(publicKey, limit = 10,
            blockTypes = null).size)

        Assert.assertEquals(2, store.getLatestBlocks(publicKey, limit = 10,
            blockTypes = listOf("custom1")).size)
    }

    @Test
    fun crawl() {
        val publicKey = getPrivateKey().pub().keyToBin()

        val block1 = TrustChainBlock(
            "custom1",
            "hello".toByteArray(Charsets.US_ASCII),
            publicKey,
            1u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val block2 = TrustChainBlock(
            "custom1",
            "hello".toByteArray(Charsets.US_ASCII),
            publicKey,
            2u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val block3 = TrustChainBlock(
            "custom2",
            "hello".toByteArray(Charsets.US_ASCII),
            publicKey,
            10u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val store = createTrustChainStore()
        store.addBlock(block1)
        store.addBlock(block2)
        store.addBlock(block3)

        // Genesis missing
        Assert.assertEquals(1L, store.getLowestSequenceNumberUnknown(ByteArray(32)))
        Assert.assertEquals(LongRange(1, 1), store.getLowestRangeUnknown(ByteArray(32)))

        // 3 to 9 missing
        Assert.assertEquals(3L, store.getLowestSequenceNumberUnknown(publicKey))
        Assert.assertEquals(LongRange(3, 9), store.getLowestRangeUnknown(block1.publicKey))

        Assert.assertEquals(2, store.crawl(publicKey, 1, 5,
            10).size)
    }

    @Test
    fun getLinked() {
        val publicKey = getPrivateKey().pub().keyToBin()

        val block1 = TrustChainBlock(
            "custom1",
            "hello".toByteArray(Charsets.US_ASCII),
            publicKey,
            1u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val block2 = TrustChainBlock(
            "custom1",
            "hello".toByteArray(Charsets.US_ASCII),
            publicKey,
            2u,
            publicKey,
            1u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val block3 = TrustChainBlock(
            "custom1",
            "hello".toByteArray(Charsets.US_ASCII),
            publicKey,
            10u,
            publicKey,
            1u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val store = createTrustChainStore()
        store.addBlock(block1)
        store.addBlock(block2)
        store.addBlock(block3)

        Assert.assertNotNull(store.getLinked(block1))
        Assert.assertEquals(2, store.getAllLinked(block1).size)

        Assert.assertEquals(3, store.getMutualBlocks(publicKey).size)
    }
}
