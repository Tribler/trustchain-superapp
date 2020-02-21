package nl.tudelft.ipv8.attestation.trustchain

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import io.mockk.mockk
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPayload
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationErrors
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.hexToBytes
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
        val privateKey = getPrivateKey()
        val block = TrustChainBlock(
            "custom",
            "hello".toByteArray(Charsets.US_ASCII),
            privateKey.pub().keyToBin(),
            0u,
            ANY_COUNTERPARTY_PK,
            0u,
            GENESIS_HASH,
            EMPTY_SIG,
            Date()
        )
        block.sign(privateKey)
        val payload = HalfBlockPayload.fromHalfBlock(block, sign = false)
        val message = payload.serialize()
        Assert.assertTrue(privateKey.pub().verify(block.signature, message))
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
}
