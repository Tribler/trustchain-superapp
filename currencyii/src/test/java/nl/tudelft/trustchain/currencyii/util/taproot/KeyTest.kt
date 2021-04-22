package nl.tudelft.trustchain.currencyii.util.taproot

import org.bitcoinj.core.ECKey
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigInteger

class KeyTest {

    @Test
    fun generate_schnorr_nonce() {
        val nonce = TaprootUtil.generate_schnorr_nonce(ECKey().privKeyBytes)

        val expected = BigInteger.ONE
        val actual = Schnorr.jacobi(nonce.second.affineYCoord.toBigInteger())

        assertEquals(expected, actual)
    }
}
