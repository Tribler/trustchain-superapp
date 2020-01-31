package nl.tudelft.ipv8.attestation.trustchain.payload

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import nl.tudelft.ipv8.attestation.trustchain.EMPTY_SIG
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert
import org.junit.Test

private val lazySodium = LazySodiumJava(SodiumJava())

class HalfBlockBroadcastayloadTest {
    @Test
    fun serializeAndDeserialize() {
        val key = LibNaClSK.fromBin("4c69624e61434c534b3a054b2367b4854a8bf2d12fcd12158a6731fcad9cfbff7dd71f9985eb9f064c8118b1a89c931819d3482c73ebd9be9ee1750dc143981f7a481b10496c4e0ef982".hexToBytes(), lazySodium)
        val block = HalfBlockPayload(
            key.pub().keyToBin(),
            0u,
            key.pub().keyToBin(),
            0u,
            ByteArray(32),
            EMPTY_SIG,
            "test",
            ByteArray(10),
            0u
        )
        val payload = HalfBlockBroadcastPayload(
            block,
            123u
        )
        val serialized = payload.serialize()

        val (deserialized, size) = HalfBlockBroadcastPayload.deserialize(serialized)

        Assert.assertEquals(serialized.size, size)
        Assert.assertArrayEquals(payload.block.publicKey, deserialized.block.publicKey)
        Assert.assertEquals(payload.block.sequenceNumber, deserialized.block.sequenceNumber)
        Assert.assertArrayEquals(payload.block.linkPublicKey, deserialized.block.linkPublicKey)
        Assert.assertEquals(payload.block.linkSequenceNumber, deserialized.block.linkSequenceNumber)
        Assert.assertArrayEquals(payload.block.previousHash, deserialized.block.previousHash)
        Assert.assertArrayEquals(payload.block.signature, deserialized.block.signature)
        Assert.assertEquals(payload.block.blockType, deserialized.block.blockType)
        Assert.assertArrayEquals(payload.block.transaction, deserialized.block.transaction)
        Assert.assertEquals(payload.block.timestamp, deserialized.block.timestamp)
        Assert.assertEquals(payload.ttl, deserialized.ttl)
    }
}
