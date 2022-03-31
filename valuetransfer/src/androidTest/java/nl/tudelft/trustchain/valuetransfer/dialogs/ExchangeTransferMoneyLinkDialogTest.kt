package nl.tudelft.trustchain.valuetransfer.dialogs

import org.junit.Assert.*
import org.junit.Test



class ExchangeTransferMoneyLinkDialogTest{
//    TO BE CONTINUED
    val exchange = ExchangeTransferMoneyLinkDialog("10",true,"Hi")
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


}
