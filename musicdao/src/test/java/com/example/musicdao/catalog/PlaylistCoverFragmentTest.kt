package com.example.musicdao.catalog

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert
import org.junit.Test
import java.util.*

private val lazySodium = LazySodiumJava(SodiumJava())

/**
 * Part of this code was ported from TrustChainBlockTest, from the
 * nl.tudelft.ipv8.attestation.trustchain package
 */
class PlaylistCoverFragmentTest {
    private val privateKey =
        JavaCryptoProvider.keyFromPrivateBin("4c69624e61434c534b3a069c289bd6031de93d49a8c35c7b2f0758c77c7b24b97842d08097abb894d8e98ba8a91ebc063f0687909f390b7ed9ec1d78fcc462298b81a51b2e3b5b9f77f2".hexToBytes())
    private val block = TrustChainBlock(
        "publish_release",
        TransactionEncoding.encode(
            mapOf(
                "title" to "title",
                "artists" to "artists",
                "date" to "date"
            )
        ),
        privateKey.pub().keyToBin(),
        GENESIS_SEQ,
        ANY_COUNTERPARTY_PK,
        UNKNOWN_SEQ,
        GENESIS_HASH,
        EMPTY_SIG,
        Date(0)
    )

    private fun getPrivateKey(): PrivateKey {
        val privateKey = "81df0af4c88f274d5228abb894a68906f9e04c902a09c68b9278bf2c7597eaf6"
        val signSeed = "c5c416509d7d262bddfcef421fc5135e0d2bdeb3cb36ae5d0b50321d766f19f2"
        return LibNaClSK(privateKey.hexToBytes(), signSeed.hexToBytes(), lazySodium)
    }

    @Test
    fun filter() {
//
//        block.sign(privateKey)
//        val payload = HalfBlockPayload.fromHalfBlock(block, sign = false)
//        val message = payload.serialize()
//
//        Assert.assertTrue(privateKey.pub().verify(block.signature, message))
//        Assert.assertEquals(
//            "4c69624e61434c504b3a80d4a88a7d010d2fb488913b5f1b5644a5eb2edbae589f81ac28e866ffe3c90b3a41a0304dc512874131fc96fa324ca3e6afeaa473dbc1e8da895c7a7c746af80000000130303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030300000000030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303000000004746573740000000b61316432736964326934320000000000000000",
//            message.toHex()
//        )
//        Assert.assertEquals(
//            "3a02014eb9e8ba9753208481db2be600da5ff23904a565a9fe0e9e663ec6774d08ecf77b1214e0cbb58f537ed595dd5ff2bfd0208e621e83674deb8b337e5101",
//            block.signature.toHex()
//        )
        val fragment = PlaylistCoverFragment(block)
        Assert.assertTrue(fragment.filter("title"))
        Assert.assertTrue(fragment.filter("Title"))
        Assert.assertTrue(fragment.filter("arti"))
        Assert.assertTrue(fragment.filter("dat"))
        Assert.assertFalse(fragment.filter("something else"))
    }
}
