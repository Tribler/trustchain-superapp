package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert
import org.junit.Test

class CTransactionTest {

    @Test
    fun ser_string() {
        val input = "210214bff3ba68d53bf47b029192a3ff841a14974f06b418368347eca254139762f5ac2102c39c2eb831bc354939e70955d360350d23dc65c13f5dd7380886fe2ddf954175ba529c6982012088a9140f29b5431fd985d12c6074072f98fd0ae7939d8887690114b2".hexToBytes()
        val expected = "sighashA = TaprootSignatureHash(spending_tx,\n" +
            "                               [tx.vout[0]],\n" +
            "                               SIGHASH_ALL_TAPROOT,\n" +
            "                               input_index=0,\n" +
            "                               scriptpath=True,\n" +
            "                               tapscript=TapLeafA.script)\n" +
            "print(sighashA.hex())\n" +
            "signatureA = privkeyA.sign_schnorr(sighashA)\n" +
            "\n" +
            "print(\"Signature for TapLeafA: {}\\n\".format(signatureA.hex()))"
        val actual = Messages.serString(input).toHex()

        Assert.assertEquals(expected, actual)
    }
}
