package nl.tudelft.trustchain.valuetransfer.dialogs
import org.junit.Assert.*
import org.junit.Test
class ExchangeTransferMoneyLinkDialogTest {
    val exchange = ExchangeTransferMoneyLinkDialog("10", true, "Hi")
    @Test
    @Suppress("IllegalIdentifier")
    fun `empty_IBAN_returns_false`() {
        val expected = exchange.isValidIban("Hi")
        assertEquals(expected, false)
    }
    @Test
    fun `empty_IBAN_returns_true`() {
        val expected = exchange.isValidIban("NL73RABO0305326805")
        assertEquals(expected, true)
    }
    @Test
    fun `Too_large_transaction_amount`() {
        val expected = exchange.isValidTransactionAmount("199999999999999.99")
        assertEquals(expected, "Invalid")
    }
    @Test
    fun `Random_transaction_amount`() {
        val expected = exchange.isValidTransactionAmount("Hi")
        assertEquals(expected, "Invalid")
    }
    @Test
    fun `Valid_transaction_amount`() {
        val expected = exchange.isValidTransactionAmount("10.00")
        assertEquals(expected, "Valid")
    }
    @Test
    fun `Valid_but_large_transaction_amount`() {
        val expected = exchange.isValidTransactionAmount("2000.00")
        assertEquals(expected, "Valid but large")
    }
}
