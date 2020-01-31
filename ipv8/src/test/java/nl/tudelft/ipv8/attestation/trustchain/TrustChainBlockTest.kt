package nl.tudelft.ipv8.attestation.trustchain

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import nl.tudelft.ipv8.attestation.trustchain.payload.HalfBlockPayload
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
        val message = HalfBlockPayload.fromHalfBlock(block, sign = false).serialize()
        Assert.assertTrue(privateKey.pub().verify(block.signature, message))
    }
}
