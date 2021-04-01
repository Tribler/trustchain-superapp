package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert
import org.junit.Test

class CTransactionTest {

    @Test
    fun taprootsignature() {
        val hash = "6f7780b2e1d8fb77f53dedf284dd4689b1391bddbed34d6c045a23d2eb7ac003"
        val publicKey: ByteArray = "0014bdca1d681e5f2c8564b518ef85d6b9bf9bd43c6e".hexToBytes()
        val coutPoint = COutPoint(hash = hash, n = 0)
        val cTxIn = CTxIn(prevout = coutPoint, scriptSig = byteArrayOf(), nSequence = 0)
        val cTxOut = CTxOut(nValue = 0.50000000, scriptPubKey = publicKey)
        val spending_tx = CTransaction(
            nVersion = 1,
            vin = arrayOf(cTxIn),
            vout = arrayOf(cTxOut),
            wit = CTxWitness(),
            nLockTime = 0
        )

        val publicKey2 =
            "512100e50574addc18523907058a69e4b3db029c19eb13e01d5278568abfada10686d4".hexToBytes()
        val txVout = CTxOut(nValue = 1.00000000, scriptPubKey = publicKey2)

        val expected =
            "0ff9af91b4bf80cbf8305c787984bd8d936d86f971e34f92d4f0931f7b525badd4d242e8ac675033f52235453f09efb6db23b6952a3baf8e390369d745e166ae"
        val actual =
            TaprootSignatureHash(spending_tx, arrayOf(txVout), SIGHASH_ALL_TAPROOT, input_index = 0).toHex()

        Assert.assertEquals(expected, actual)
    }
}
