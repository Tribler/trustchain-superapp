package nl.tudelft.ipv8.attestation.trustchain.payload

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import nl.tudelft.ipv8.attestation.trustchain.EMPTY_SIG
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert
import org.junit.Test

class EmptyCrawlResponsePayloadTest {
    @Test
    fun serialize() {
        val payload = EmptyCrawlResponsePayload(
            123u
        )
        val serialized = payload.serialize()

        val (deserialized, size) = EmptyCrawlResponsePayload.deserialize(serialized)

        Assert.assertEquals(serialized.size, size)
        Assert.assertEquals(payload.crawlId, deserialized.crawlId)
    }
}
