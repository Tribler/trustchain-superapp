package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.toHex
import org.junit.Assert
import org.junit.Test

class MessagesTest {

    @Test
    fun ser_string() {
        val input = "210214bff3ba68d53bf47b029192a3ff841a14974f06b418368347eca254139762f5ac2102c39c2eb831bc354939e70955d360350d23dc65c13f5dd7380886fe2ddf954175ba529c6982012088a9140f29b5431fd985d12c6074072f98fd0ae7939d8887690114b2"
        val expected = "68210214bff3ba68d53bf47b029192a3ff841a14974f06b418368347eca254139762f5ac2102c39c2eb831bc354939e70955d360350d23dc65c13f5dd7380886fe2ddf954175ba529c6982012088a9140f29b5431fd985d12c6074072f98fd0ae7939d8887690114b2"
        val actual = Messages.ser_string(input).toHex()

        Assert.assertEquals(expected, actual)
    }
}
