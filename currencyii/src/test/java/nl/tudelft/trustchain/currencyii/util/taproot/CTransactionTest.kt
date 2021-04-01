package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert
import org.junit.Test

class CTransactionTest {

    @Test
    fun taprootsignature() {
        val hash = "9810b6e1b42d1f5d3c9377c5b1c3b6bb2ee0f96427d9adead6c99626798faac8"
        val publicKey: ByteArray = "001420501761e7ba8b479cc516488d47e8f5d02e52d7".hexToBytes()
        val coutPoint = COutPoint(hash = hash, n = 0)
        val cTxIn = CTxIn(prevout = coutPoint, scriptSig = byteArrayOf(), nSequence = 0)
        val cTxOut = CTxOut(nValue = (0.50000000 * 100_000_000).toLong(), scriptPubKey = publicKey)
        val spending_tx = CTransaction(
            nVersion = 1,
            vin = arrayOf(cTxIn),
            vout = arrayOf(cTxOut),
            wit = CTxWitness(),
            nLockTime = 0
        )

        val publicKey2 =
            "5121003dd5fc3c1766d0a73466a5997da83efcc529107c9ecd0c56e2a28519f0eb3104".hexToBytes()
        val txVout = CTxOut(nValue = (1.00000000 * 100_000_000).toLong(), scriptPubKey = publicKey2)

        val expected =
            "c58660789cf1bbd4c265823168ccdc2e13a5f97c4d2e8742e08e16ee21d0929a"
        val actual =
            TaprootSignatureHash(spending_tx, arrayOf(txVout), SIGHASH_ALL_TAPROOT, input_index = 0).toHex()

        Assert.assertEquals(expected, actual)
    }
}
