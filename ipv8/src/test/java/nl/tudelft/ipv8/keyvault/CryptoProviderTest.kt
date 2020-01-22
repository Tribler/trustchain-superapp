package nl.tudelft.ipv8.keyvault

import org.junit.Assert
import org.junit.Test

class CryptoProviderTest {
    @Test
    fun generate() {
        val key = JavaCryptoProvider.generateKey()
        Assert.assertTrue(key is LibNaClSK)
    }
}
