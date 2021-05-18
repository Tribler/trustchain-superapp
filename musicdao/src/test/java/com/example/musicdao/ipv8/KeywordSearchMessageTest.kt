package com.example.musicdao.ipv8

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert
import org.junit.Test

private val lazySodium = LazySodiumJava(SodiumJava())

class KeywordSearchMessageTest {
    private val key = LibNaClSK.fromBin(
        "4c69624e61434c534b3a054b2367b4854a8bf2d12fcd12158a6731fcad9cfbff7dd71f9985eb9f064c8118b1a89c931819d3482c73ebd9be9ee1750dc143981f7a481b10496c4e0ef982".hexToBytes(),
        lazySodium
    )
    private val originPublicKey = key.pub().keyToBin()
    private val ttl = 2u
    private val keyword = "keyword"
    private val payload =
        KeywordSearchMessage(originPublicKey, ttl, keyword)

    @Test
    fun checkTTL() {
        Assert.assertTrue(payload.checkTTL())
        Assert.assertFalse(payload.checkTTL())
    }

    @Test
    fun serializeAndDeserialize() {
        val serialized = payload.serialize()
        val (deserialized, size) = com.example.musicdao.ipv8.KeywordSearchMessage.deserialize(
            serialized
        )
        Assert.assertEquals(serialized.size, size)
        Assert.assertEquals(payload.keyword, deserialized.keyword)
        Assert.assertEquals(payload.ttl, deserialized.ttl)
        Assert.assertArrayEquals(payload.originPublicKey, deserialized.originPublicKey)
    }
}
