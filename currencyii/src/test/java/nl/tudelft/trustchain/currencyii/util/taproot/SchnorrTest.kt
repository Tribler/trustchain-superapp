package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.trustchain.currencyii.util.taproot.Schnorr.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import java.math.BigInteger


class SchnorrTest {

    @ParameterizedTest
    @CsvFileSource(
        files = arrayOf("src/test/java/nl/tudelft/trustchain/currencyii/util/taproot/test-vectors.csv"),
        numLinesToSkip = 1
    )
    fun schnorrTestCSVFile(
        index: Int, secKey: String?, publicKey: String, message: String,
        signature: String, result: Boolean, comment: String?
    ) {
        val msg = hexStringToByteArray(message)
        val pubKey = hexStringToByteArray(publicKey)

        if (secKey != null && !secKey.isEmpty()) {
            val seckeyNum = BigInteger(secKey, 16)
            val sig_actual: String = bytesToHex(schnorr_sign(msg, seckeyNum))

            assertEquals(signature, sig_actual, "Failed signing for test $index, expected $signature but got: $sig_actual")
        }

        val result_actual = schnorr_verify(msg, pubKey, hexStringToByteArray(signature))

        assertEquals(result, result_actual, "Failed verification for test $index, expected: $result but got: $result_actual, explanation: $comment")
    }
}
