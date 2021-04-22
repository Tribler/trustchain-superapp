package nl.tudelft.trustchain.currencyii.util.taproot

import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MessagesTest {

    @Test
    fun ser_string() {
        val input = "210214bff3ba68d53bf47b029192a3ff841a14974f06b418368347eca254139762f5ac2102c39c2eb831bc354939e70955d360350d23dc65c13f5dd7380886fe2ddf954175ba529c6982012088a9140f29b5431fd985d12c6074072f98fd0ae7939d8887690114b2".hexToBytes()
        val expected = "68210214bff3ba68d53bf47b029192a3ff841a14974f06b418368347eca254139762f5ac2102c39c2eb831bc354939e70955d360350d23dc65c13f5dd7380886fe2ddf954175ba529c6982012088a9140f29b5431fd985d12c6074072f98fd0ae7939d8887690114b2"
        val actual = Messages.serString(input).toHex()

        assertEquals(expected, actual)
    }

    @Test
    fun ser_string_A() {
        val input = "2103148fe4ff9ee1cfdf53c422e621bf2608e215a824ace294b4776b2ab7db49d000ac".hexToBytes()
        val expected = "232103148fe4ff9ee1cfdf53c422e621bf2608e215a824ace294b4776b2ab7db49d000ac"
        val actual = Messages.serString(input).toHex()

        assertEquals(expected, actual)
    }

    @Test
    fun ser_string_B() {
        val input = "21027f293ae45f4b27d6120079f43fcaca248c19db824724338f392289c34789e6c9ac".hexToBytes()
        val expected = "2321027f293ae45f4b27d6120079f43fcaca248c19db824724338f392289c34789e6c9ac"
        val actual = Messages.serString(input).toHex()

        assertEquals(expected, actual)
    }

    @Test
    fun ser_string_C() {
        val input = "210247c1ef4926e56835b8bf975ba14608d2ced2e82b6659ad820aa431d7a6299c1dac".hexToBytes()
        val expected = "23210247c1ef4926e56835b8bf975ba14608d2ced2e82b6659ad820aa431d7a6299c1dac"
        val actual = Messages.serString(input).toHex()

        assertEquals(expected, actual)
    }
}
