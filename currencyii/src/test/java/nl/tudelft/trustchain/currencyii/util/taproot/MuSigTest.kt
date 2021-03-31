package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.toHex
import org.bitcoinj.core.ECKey
import org.junit.Test

import org.junit.Assert.*
import java.math.BigInteger

class MuSigTest {

    @Test
    fun generate_musig_key() {

        val key1 = ECKey.fromPrivate(BigInteger("88218786999700320424912157840922001183470238663577897435520060565802125439712"))
        val key2 = ECKey.fromPrivate(BigInteger("11756621930195768229168784074199362003209438395325908648574429387730312779458"))

        val expected = "023dd5fc3c1766d0a73466a5997da83efcc529107c9ecd0c56e2a28519f0eb3104"
        val actual = MuSig.generate_musig_key(listOf(key1, key2)).second.getEncoded(true).toHex()

        assertEquals(expected, actual)
    }
}
