package nl.tudelft.ipv8.attestation.trustchain.payload

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import nl.tudelft.ipv8.attestation.trustchain.EMPTY_SIG
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert
import org.junit.Test

private val lazySodium = LazySodiumJava(SodiumJava())

class HalfBlockPairPayloadTest {
    @Test
    fun serializeAndDeserialize() {
        val key = LibNaClSK.fromBin("4c69624e61434c534b3a054b2367b4854a8bf2d12fcd12158a6731fcad9cfbff7dd71f9985eb9f064c8118b1a89c931819d3482c73ebd9be9ee1750dc143981f7a481b10496c4e0ef982".hexToBytes(), lazySodium)
        val block1 = HalfBlockPayload(
            key.pub().keyToBin(),
            2u,
            key.pub().keyToBin(),
            0u,
            ByteArray(32),
            EMPTY_SIG,
            "test",
            ByteArray(10),
            0u
        )
        val block2 = HalfBlockPayload(
            key.pub().keyToBin(),
            3u,
            key.pub().keyToBin(),
            2u,
            ByteArray(32),
            EMPTY_SIG,
            "test2",
            ByteArray(10),
            0u
        )
        val payload = HalfBlockPairPayload(block1, block2)
        val serialized = payload.serialize()

        val (deserialized, size) = HalfBlockPairPayload.deserialize(serialized)

        Assert.assertEquals(serialized.size, size)

        Assert.assertArrayEquals(payload.block1.publicKey, deserialized.block1.publicKey)
        Assert.assertEquals(payload.block1.sequenceNumber, deserialized.block1.sequenceNumber)
        Assert.assertArrayEquals(payload.block1.linkPublicKey, deserialized.block1.linkPublicKey)
        Assert.assertEquals(payload.block1.linkSequenceNumber, deserialized.block1.linkSequenceNumber)
        Assert.assertArrayEquals(payload.block1.previousHash, deserialized.block1.previousHash)
        Assert.assertArrayEquals(payload.block1.signature, deserialized.block1.signature)
        Assert.assertEquals(payload.block1.blockType, deserialized.block1.blockType)
        Assert.assertArrayEquals(payload.block1.transaction, deserialized.block1.transaction)
        Assert.assertEquals(payload.block1.timestamp, deserialized.block1.timestamp)

        Assert.assertArrayEquals(payload.block2.publicKey, deserialized.block2.publicKey)
        Assert.assertEquals(payload.block2.sequenceNumber, deserialized.block2.sequenceNumber)
        Assert.assertArrayEquals(payload.block2.linkPublicKey, deserialized.block2.linkPublicKey)
        Assert.assertEquals(payload.block2.linkSequenceNumber, deserialized.block2.linkSequenceNumber)
        Assert.assertArrayEquals(payload.block2.previousHash, deserialized.block2.previousHash)
        Assert.assertArrayEquals(payload.block2.signature, deserialized.block2.signature)
        Assert.assertEquals(payload.block2.blockType, deserialized.block2.blockType)
        Assert.assertArrayEquals(payload.block2.transaction, deserialized.block2.transaction)
        Assert.assertEquals(payload.block2.timestamp, deserialized.block2.timestamp)
    }
}
