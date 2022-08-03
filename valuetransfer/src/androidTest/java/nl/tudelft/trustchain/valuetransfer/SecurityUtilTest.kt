package nl.tudelft.trustchain.valuetransfer

import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.tudelft.trustchain.valuetransfer.util.SecurityUtil
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyPair

@RunWith(AndroidJUnit4::class)
class SecurityUtilTest {
    val input: String = "test"
    val keys: KeyPair = SecurityUtil.generateKey()
    var signature: String = ""

    @Before
    fun setup() {
        signature = SecurityUtil.sign(input, keys.private)
    }

    @Test
    fun SignatureValidation() {
        assertEquals(SecurityUtil.validate(input, signature, keys.public), true)
    }

    @Test
    fun DetectInputTampering() {
        val tamperedInput = input + "extra"
        assertEquals(SecurityUtil.validate(tamperedInput, signature, keys.public), false)
    }
}
