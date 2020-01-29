package nl.tudelft.ipv8.attestation.trustchain.payload

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import nl.tudelft.ipv8.attestation.trustchain.EMPTY_SIG
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert
import org.junit.Test

private val lazySodium = LazySodiumJava(SodiumJava())

class CrawlRequestPayloadTest {
    @Test
    fun serializeAndDeserialize() {
        val key = LibNaClSK.fromBin("4c69624e61434c534b3a054b2367b4854a8bf2d12fcd12158a6731fcad9cfbff7dd71f9985eb9f064c8118b1a89c931819d3482c73ebd9be9ee1750dc143981f7a481b10496c4e0ef982".hexToBytes(), lazySodium)
        val payload = CrawlRequestPayload(
            key.pub().keyToBin(),
            1L,
            10L,
            123u
        )
        val serialized = payload.serialize()

        val (deserialized, size) = CrawlRequestPayload.deserialize(serialized)

        Assert.assertEquals(serialized.size, size)
        Assert.assertArrayEquals(payload.publicKey, deserialized.publicKey)
        Assert.assertEquals(payload.startSeqNum, deserialized.startSeqNum)
        Assert.assertEquals(payload.endSeqNum, deserialized.endSeqNum)
        Assert.assertEquals(payload.crawlId, deserialized.crawlId)
    }
}
