package nl.tudelft.ipv8.attestation.trustchain

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.BaseCommunityTest
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPayload
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.peerdiscovery.Network
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger
import java.util.*

class TrustChainCommunityTest : BaseCommunityTest() {
    private fun getCommunity(): TrustChainCommunity {
        val settings = TrustChainSettings()
        val store = mockk<TrustChainStore>(relaxed = true)
        val community = TrustChainCommunity(settings = settings, database = store)
        community.myPeer = getMyPeer()
        community.endpoint = getEndpoint()
        community.network = Network()
        community.maxPeers = 20
        community.cryptoProvider = JavaCryptoProvider
        community.load()
        return community
    }

    @Test
    fun notifyListeners() {
        val community = getCommunity()
        val universalListener = mockk<BlockListener>(relaxed = true)
        val customListener = mockk<BlockListener>(relaxed = true)
        val custom2Listener = mockk<BlockListener>(relaxed = true)
        community.addListener(null, universalListener)
        community.addListener("custom", customListener)
        community.addListener("custom2", customListener)

        val block = TrustChainBlock(
            "custom",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            0u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        community.notifyListeners(block)

        verify(exactly = 1) { universalListener.onBlockReceived(block) }
        verify(exactly = 1) { customListener.onBlockReceived(block) }
        verify(exactly = 0) { custom2Listener.onBlockReceived(block) }

        community.removeListener(universalListener)
        community.removeListener(customListener, "custom")
        community.removeListener(custom2Listener, "custom2")

        community.notifyListeners(block)

        verify(exactly = 1) { universalListener.onBlockReceived(block) }
        verify(exactly = 1) { customListener.onBlockReceived(block) }
        verify(exactly = 0) { custom2Listener.onBlockReceived(block) }
    }

    @Test
    fun validateAndPersistBlock_valid() {
        val community = getCommunity()
        every { community.database.addBlock(any()) } returns Unit
        every { community.database.getBlockBefore(any()) } returns null
        every { community.database.getBlockAfter(any()) } returns null

        val customListener = mockk<BlockListener>(relaxed = true)
        community.addListener("custom", customListener)

        val validator = mockk<TransactionValidator>()
        every { validator.validate(any(), any()) } returns true
        community.registerTransactionValidator("custom", validator)

        val block = TrustChainBlock(
            "custom",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            GENESIS_SEQ,
            ANY_COUNTERPARTY_PK,
            UNKNOWN_SEQ,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )
        block.sign(getPrivateKey())

        every { community.database.contains(block) } returns false
        community.validateAndPersistBlock(block)
        verify(exactly = 1) { customListener.onBlockReceived(block) }

        every { community.database.contains(block) } returns true
        community.validateAndPersistBlock(block)
        verify(exactly = 1) { customListener.onBlockReceived(block) }
    }

    @Test
    fun createProposalBlock_genesis() {
        val community = getCommunity()
        val store = community.database

        every { store.getLatest(any(), any()) } returns null
        every { store.contains(any()) } returns false
        every { store.addBlock(any()) } returns Unit
        every { store.getBlockBefore(any()) } returns null
        every { store.getBlockAfter(any()) } returns null

        val block = community.createProposalBlock(
            "custom",
            mapOf("test" to 42),
            getPrivateKey().pub().keyToBin()
        )

        Assert.assertEquals("custom", block.type)
        Assert.assertEquals(BigInteger.valueOf(42), block.transaction["test"])
        Assert.assertArrayEquals(getPrivateKey().pub().keyToBin(), block.publicKey)
        Assert.assertEquals(UNKNOWN_SEQ, block.linkSequenceNumber)
        Assert.assertEquals(GENESIS_SEQ, block.sequenceNumber)
        Assert.assertArrayEquals(GENESIS_HASH, block.previousHash)
        Assert.assertNotEquals(EMPTY_SIG, block.signature)
    }

    @Test
    fun createAgreementBlock_genesis() {
        val community = getCommunity()
        val store = community.database

        every { store.getLatest(any(), any()) } returns null
        every { store.contains(any()) } returns false
        every { store.addBlock(any()) } returns Unit
        every { store.getBlockBefore(any()) } returns null
        every { store.getBlockAfter(any()) } returns null

        val block1 = TrustChainBlock(
            "custom",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            GENESIS_SEQ,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )

        val block2 = community.createAgreementBlock(
            block1,
            mapOf("test" to 42)
        )

        Assert.assertEquals("custom", block2.type)
        Assert.assertEquals(BigInteger.valueOf(42), block2.transaction["test"])
        Assert.assertArrayEquals(getPrivateKey().pub().keyToBin(), block2.publicKey)
        Assert.assertEquals(GENESIS_SEQ, block2.linkSequenceNumber)
        Assert.assertEquals(GENESIS_SEQ, block2.sequenceNumber)
        Assert.assertArrayEquals(GENESIS_HASH, block2.previousHash)
        Assert.assertNotEquals(EMPTY_SIG, block2.signature)
    }

    @Test
    fun processHalfBlock_signatureRequest() {
        val community = getCommunity()

        val blockSigner = mockk<BlockSigner>()
        community.registerBlockSigner("test", blockSigner)

        val address = Address("1.2.3.4", 1234)
        val senderKey = JavaCryptoProvider.generateKey()
        val myKey = getPrivateKey()
        val payload = HalfBlockPayload(
            senderKey.pub().keyToBin(),
            GENESIS_SEQ,
            myKey.pub().keyToBin(),
            UNKNOWN_SEQ,
            GENESIS_HASH,
            EMPTY_SIG,
            "test",
            ByteArray(10),
            1581459001000u
        )
        every { community.database.getLinked(any()) } returns null
        community.onHalfBlock(address, payload)

        verify(exactly = 1) { blockSigner.onSignatureRequest(any()) }
    }

    @Test
    fun processHalfBlock_otherPublicKey() {
        val community = getCommunity()

        val blockSigner = mockk<BlockSigner>()
        community.registerBlockSigner("test", blockSigner)

        val address = Address("1.2.3.4", 1234)
        val senderKey = JavaCryptoProvider.generateKey()
        val payload = HalfBlockPayload(
            senderKey.pub().keyToBin(),
            GENESIS_SEQ,
            senderKey.pub().keyToBin(),
            UNKNOWN_SEQ,
            GENESIS_HASH,
            EMPTY_SIG,
            "test",
            ByteArray(10),
            1581459001000u
        )
        every { community.database.getLinked(any()) } returns null
        community.onHalfBlock(address, payload)

        verify { blockSigner wasNot Called }
    }
}
