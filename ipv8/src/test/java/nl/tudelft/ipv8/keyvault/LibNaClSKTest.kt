package nl.tudelft.ipv8.keyvault

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert
import org.junit.Test

class LibNaClSKTest {
    val lazySodium = LazySodiumJava(SodiumJava())

    @Test
    fun generate() {
        val key = LibNaClSK.generate(lazySodium)
        Assert.assertEquals(32, key.privateKey.size)
        Assert.assertEquals(32, key.signSeed.size)
    }

    @Test
    fun keyToBin() {
        val key = LibNaClSK.generate(lazySodium)
        val bin = key.keyToBin()
        Assert.assertEquals(74, bin.size)
    }

    @Test
    fun keyToHash() {
        val key = LibNaClSK.generate(lazySodium)
        val hash = key.keyToHash()
        Assert.assertEquals(40, hash.toHex().length)
    }

    @Test
    fun sign() {
        val key = LibNaClSK.generate(lazySodium)
        val msg = "Hello!".toByteArray(Charsets.UTF_8)
        val signature = key.sign(msg)
        Assert.assertEquals(64, signature.size)
    }
}
