package nl.tudelft.trustchain.common.ethereum

import nl.tudelft.trustchain.common.ethereum.utils.generateWalletPassword
import org.junit.Test
import java.security.SecureRandom

private const val PASSWORD_REGEX = "[a-zA-Z0-9!]+";

class WalletPasswordUtilsTest {

    @Test
    fun generatedWalletPassword_isCorrect() {
        // given
        val random = SecureRandom(ByteArray(0))

        // when
        val password = generateWalletPassword(random)

        // then
        assert(password.matches(Regex(PASSWORD_REGEX)))
    }

}
