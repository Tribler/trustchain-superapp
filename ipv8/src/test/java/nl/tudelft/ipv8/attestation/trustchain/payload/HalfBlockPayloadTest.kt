package nl.tudelft.ipv8.attestation.trustchain.payload

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import nl.tudelft.ipv8.attestation.trustchain.ANY_COUNTERPARTY_PK
import nl.tudelft.ipv8.attestation.trustchain.EMPTY_SIG
import nl.tudelft.ipv8.attestation.trustchain.GENESIS_HASH
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert
import org.junit.Test
import java.util.*

private val lazySodium = LazySodiumJava(SodiumJava())

class HalfBlockPayloadTest {
    private fun getPrivateKey(): PrivateKey {
        return LibNaClSK.fromBin("4c69624e61434c534b3a054b2367b4854a8bf2d12fcd12158a6731fcad9cfbff7dd71f9985eb9f064c8118b1a89c931819d3482c73ebd9be9ee1750dc143981f7a481b10496c4e0ef982".hexToBytes(), lazySodium)
    }

    @Test
    fun fromHalfBlock() {
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
        val payload = HalfBlockPayload.fromHalfBlock(block)
        val block2 = payload.toBlock()
        Assert.assertEquals(block.type, block2.type)
        Assert.assertEquals(block.rawTransaction, block2.rawTransaction)
        Assert.assertEquals(block.publicKey, block2.publicKey)
        Assert.assertEquals(block.sequenceNumber, block2.sequenceNumber)
        Assert.assertEquals(block.linkPublicKey, block2.linkPublicKey)
        Assert.assertEquals(block.linkSequenceNumber, block2.linkSequenceNumber)
        Assert.assertEquals(block.previousHash, block2.previousHash)
        Assert.assertEquals(block.signature, block2.signature)
        Assert.assertEquals(block.timestamp, block2.timestamp)
    }

    @Test
    fun serializeAndDeserialize() {
        val key = getPrivateKey()
        val payload = HalfBlockPayload(
            key.pub().keyToBin(),
            0u,
            key.pub().keyToBin(),
            0u,
            ByteArray(32),
            EMPTY_SIG,
            "test",
            ByteArray(10),
            1581459001000u
        )
        val serialized = payload.serialize()

        val (deserialized, size) = HalfBlockPayload.deserialize(serialized)

        Assert.assertEquals(serialized.size, size)
        Assert.assertArrayEquals(payload.publicKey, deserialized.publicKey)
        Assert.assertEquals(payload.sequenceNumber, deserialized.sequenceNumber)
        Assert.assertArrayEquals(payload.linkPublicKey, deserialized.linkPublicKey)
        Assert.assertEquals(payload.linkSequenceNumber, deserialized.linkSequenceNumber)
        Assert.assertArrayEquals(payload.previousHash, deserialized.previousHash)
        Assert.assertArrayEquals(payload.signature, deserialized.signature)
        Assert.assertEquals(payload.blockType, deserialized.blockType)
        Assert.assertArrayEquals(payload.transaction, deserialized.transaction)
        Assert.assertEquals(payload.timestamp, deserialized.timestamp)
    }
}
