package nl.tudelft.ipv8.attestation.trustchain

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import io.mockk.every
import io.mockk.mockk
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPayload
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationErrors
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert
import org.junit.Test
import java.util.*

private val lazySodium = LazySodiumJava(SodiumJava())

class TrustChainBlockTest {
    private fun getPrivateKey(): PrivateKey {
        val privateKey = "81df0af4c88f274d5228abb894a68906f9e04c902a09c68b9278bf2c7597eaf6"
        val signSeed = "c5c416509d7d262bddfcef421fc5135e0d2bdeb3cb36ae5d0b50321d766f19f2"
        return LibNaClSK(privateKey.hexToBytes(), signSeed.hexToBytes(), lazySodium)
    }

    @Test
    fun sign() {
        val privateKey = JavaCryptoProvider.keyFromPrivateBin("4c69624e61434c534b3a069c289bd6031de93d49a8c35c7b2f0758c77c7b24b97842d08097abb894d8e98ba8a91ebc063f0687909f390b7ed9ec1d78fcc462298b81a51b2e3b5b9f77f2".hexToBytes())
        val block = TrustChainBlock(
            "test",
            TransactionEncoding.encode(mapOf("id" to 42)),
            privateKey.pub().keyToBin(),
            GENESIS_SEQ,
            ANY_COUNTERPARTY_PK,
            UNKNOWN_SEQ,
            GENESIS_HASH,
            EMPTY_SIG,
            Date(0)
        )
        block.sign(privateKey)
        val payload = HalfBlockPayload.fromHalfBlock(block, sign = false)
        val message = payload.serialize()

        Assert.assertTrue(privateKey.pub().verify(block.signature, message))
        Assert.assertEquals("4c69624e61434c504b3a80d4a88a7d010d2fb488913b5f1b5644a5eb2edbae589f81ac28e866ffe3c90b3a41a0304dc512874131fc96fa324ca3e6afeaa473dbc1e8da895c7a7c746af80000000130303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030300000000030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303000000004746573740000000b61316432736964326934320000000000000000", message.toHex())
        Assert.assertEquals("3a02014eb9e8ba9753208481db2be600da5ff23904a565a9fe0e9e663ec6774d08ecf77b1214e0cbb58f537ed595dd5ff2bfd0208e621e83674deb8b337e5101", block.signature.toHex())
    }

    @Test
    fun validate_valid() {
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
        val store = mockk<TrustChainStore>(relaxed = true)
        every { store.getBlockBefore(any()) } returns null
        every { store.getBlockAfter(any()) } returns null
        val result = block.validate(store)
        Assert.assertEquals(ValidationResult.PartialNext, result)
    }

    @Test
    fun validate_invalidSeqNum() {
        val block = TrustChainBlock(
            "custom",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            0u,
            ANY_COUNTERPARTY_PK,
            UNKNOWN_SEQ,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )
        block.sign(getPrivateKey())
        val store = mockk<TrustChainStore>(relaxed = true)
        val result = block.validate(store)
        Assert.assertTrue(result is ValidationResult.Invalid)
        Assert.assertTrue(result is ValidationResult.Invalid &&
            result.errors.contains(ValidationErrors.INVALID_SEQUENCE_NUMBER))
    }

    @Test
    fun validate_invalidGenesisHash() {
        val block = TrustChainBlock(
            "custom",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            GENESIS_SEQ,
            ANY_COUNTERPARTY_PK,
            UNKNOWN_SEQ,
            byteArrayOf(42),
            EMPTY_SIG,
            Date()
        )
        block.sign(getPrivateKey())
        val store = mockk<TrustChainStore>(relaxed = true)
        val result = block.validate(store)
        Assert.assertTrue(result is ValidationResult.Invalid)
        Assert.assertTrue(result is ValidationResult.Invalid &&
            result.errors.contains(ValidationErrors.INVALID_GENESIS_HASH))
    }

    @Test
    fun validate_invalidGenesisSeq() {
        val block = TrustChainBlock(
            "custom",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            2u,
            ANY_COUNTERPARTY_PK,
            UNKNOWN_SEQ,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )
        block.sign(getPrivateKey())
        val store = mockk<TrustChainStore>(relaxed = true)
        val result = block.validate(store)
        Assert.assertTrue(result is ValidationResult.Invalid)
        Assert.assertTrue(result is ValidationResult.Invalid &&
            result.errors.contains(ValidationErrors.INVALID_GENESIS_SEQUENCE_NUMBER))
    }

    @Test
    fun validate_invalidSignature() {
        val block = TrustChainBlock(
            "custom",
            "hello".toByteArray(Charsets.US_ASCII),
            getPrivateKey().pub().keyToBin(),
            2u,
            ANY_COUNTERPARTY_PK,
            UNKNOWN_SEQ,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )
        val store = mockk<TrustChainStore>(relaxed = true)
        val result = block.validate(store)
        Assert.assertTrue(result is ValidationResult.Invalid)
        Assert.assertTrue(result is ValidationResult.Invalid &&
            result.errors.contains(ValidationErrors.INVALID_SIGNATURE))
    }

    @Test
    fun getHashNumber() {
        val privateKey = JavaCryptoProvider.keyFromPrivateBin("4c69624e61434c534b3a069c289bd6031de93d49a8c35c7b2f0758c77c7b24b97842d08097abb894d8e98ba8a91ebc063f0687909f390b7ed9ec1d78fcc462298b81a51b2e3b5b9f77f2".hexToBytes())
        val block = TrustChainBlock(
            "test",
            TransactionEncoding.encode(mapOf("id" to 42)),
            privateKey.pub().keyToBin(),
            GENESIS_SEQ,
            ANY_COUNTERPARTY_PK,
            UNKNOWN_SEQ,
            GENESIS_HASH,
            EMPTY_SIG,
            Date(0)
        )
        block.sign(privateKey)

        Assert.assertEquals(16207010, block.hashNumber)
    }
}
