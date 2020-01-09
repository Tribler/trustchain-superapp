package nl.tudelft.ipv8.keyvault

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibNaClSKTest {
    @Test
    fun generate() {
        val key = LibNaClSK.generate()
        Assert.assertEquals(32, key.privateKey.size)
        Assert.assertEquals(32, key.signSeed.size)
    }

    @Test
    fun keyToBin() {
        val key = LibNaClSK.generate()
        val bin = key.keyToBin()
        Assert.assertEquals(74, bin.size)
    }

    @Test
    fun keyToHash() {
        val key = LibNaClSK.generate()
        val hash = key.keyToHash()
        Assert.assertEquals(40, hash.length)
    }

    @Test
    fun sign() {
        val key = LibNaClSK.generate()
        val msg = "Hello!".toByteArray(Charsets.UTF_8)
        val signature = key.sign(msg)
        Assert.assertEquals(64, signature.size)
    }
}
