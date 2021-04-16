package nl.tudelft.trustchain.currencyii.util.taproot

import org.bitcoinj.core.ECKey
import org.junit.Test

import org.junit.Assert.*
import java.math.BigInteger

class KeyTest {

    @Test
    fun generate_schnorr_nonce() {
        val nonce = Key.generate_schnorr_nonce(ECKey().privKeyBytes)

        val expected = BigInteger.ONE
        val actual = Schnorr.jacobi(nonce.second.affineYCoord.toBigInteger())

        assertEquals(expected, actual)
    }
}
