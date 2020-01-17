package nl.tudelft.ipv8.keyvault

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ECCryptoTest {
    @Test
    fun generate() {
        val key = ECCrypto.generateKey()
        Assert.assertTrue(key is LibNaClSK)
    }
}
