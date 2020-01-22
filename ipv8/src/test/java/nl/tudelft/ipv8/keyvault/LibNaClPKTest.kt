package nl.tudelft.ipv8.keyvault

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert
import org.junit.Test
import kotlin.IllegalArgumentException

class LibNaClPKTest {
    val lazySodium = LazySodiumJava(SodiumJava())

    @Test
    fun getSignatureLength() {
        val key = LibNaClSK.generate(lazySodium).pub()
        Assert.assertEquals(64, key.getSignatureLength())
    }

    @Test
    fun keyToBin() {
        val key = LibNaClSK.generate(lazySodium).pub()
        val bin = key.keyToBin()
        Assert.assertEquals(74, bin.size)

        val str = bin.toString(Charsets.US_ASCII)
        Assert.assertTrue(str.startsWith("LibNaCLPK:"))
    }

    @Test
    fun keyToHash() {
        val key = LibNaClSK.generate(lazySodium).pub()
        val hash = key.keyToHash()
        Assert.assertEquals(40, hash.toHex().length)
    }

    @Test
    fun verify() {
        val key = LibNaClSK.generate(lazySodium)
        val msg = "Hello!".toByteArray(Charsets.UTF_8)
        val signature = key.sign(msg)

        Assert.assertEquals(32, key.privateKey.size)
        Assert.assertEquals(32, key.signSeed.size)

        val pk = key.pub()
        val verified = pk.verify(signature, msg)
        Assert.assertTrue(verified)

        val fakeSignature = "Fake".toByteArray(Charsets.UTF_8)
        val verifiedFake = pk.verify(fakeSignature, msg)
        Assert.assertFalse(verifiedFake)
    }

    @Test
    fun fromBin_success() {
        val hex = "4c69624e61434c504b3af7e62cb0152b1422ad844597a3fd3cafb4a13ae3f1943e9adb45be8bfd2bde69a771a81302db83d5f0ec524cc5f60093702b0dad242824ee1af1a2661425759c"
        val key = LibNaClPK.fromBin(hex.hexToBytes(), lazySodium)
        Assert.assertEquals(hex, key.keyToBin().toHex())
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromBin_invalidLength() {
        val hex = "4c69624e61434c504b3af7e623"
        LibNaClPK.fromBin(hex.hexToBytes(), lazySodium)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromBin_invalidPrefix() {
        val hex = "1c69624e61434c504b3af7e62cb0152b1422ad844597a3fd3cafb4a13ae3f1943e9adb45be8bfd2bde69a771a81302db83d5f0ec524cc5f60093702b0dad242824ee1af1a2661425759c"
        LibNaClPK.fromBin(hex.hexToBytes(), lazySodium)
    }
}
