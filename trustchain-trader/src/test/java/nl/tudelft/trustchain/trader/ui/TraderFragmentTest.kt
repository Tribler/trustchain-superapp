package nl.tudelft.trustchain.trader.ui

import android.util.Log
import android.widget.TextView
import io.mockk.*
import kotlinx.android.synthetic.main.fragment_trader.*
import nl.tudelft.trustchain.common.constants.Currency
import nl.tudelft.trustchain.common.messaging.TradePayload
import nl.tudelft.trustchain.trader.ai.NaiveBayes
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalUnsignedTypes
class TraderFragmentTest {
    val fragment = TraderFragment()
    val fragmentMock = spyk(fragment)

    val tradePayload = mockk<TradePayload>()
    val aiMock = mockk<NaiveBayes>(relaxed = true)

    @Before
    fun init() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        fragmentMock.ai = aiMock
    }

//    @Test
    fun testAskListener() {
        every { tradePayload.publicKey } returns (ByteArray(0))
        every { tradePayload.amount } returns (1.0)
        every { tradePayload.price } returns (90.0)
        every { tradePayload.primaryCurrency } returns (Currency.BTC)
        every { tradePayload.secondaryCurrency } returns (Currency.DYMBE_DOLLAR)
        every { tradePayload.type } returns (TradePayload.Type.ASK)
        every { aiMock.predict(any()) } returns 1

        fragmentMock.askListener(tradePayload)

        verify { aiMock.predict(any()) }
    }

//    @Test
    fun testBidListener() {
        every { tradePayload.publicKey } returns (ByteArray(0))
        every { tradePayload.amount } returns (1.0)
        every { tradePayload.price } returns (90.0)
        every { tradePayload.primaryCurrency } returns (Currency.BTC)
        every { tradePayload.secondaryCurrency } returns (Currency.DYMBE_DOLLAR)
        every { tradePayload.type } returns (TradePayload.Type.ASK)
        every { aiMock.predict(any()) } returns 0

        fragmentMock.bidListener(tradePayload)

        verify { aiMock.predict(any()) }
    }

//    @Test
    fun testRound() {
        val privateRound = fragment.javaClass.getDeclaredMethod("round", Int::class.java)
        privateRound.isAccessible = true
        assertEquals(115, privateRound.invoke(fragment, 150))
        assertEquals(85, privateRound.invoke(fragment, 60))
        assertEquals(100, privateRound.invoke(fragment, 100))
    }

//    @Test
    fun testUpdateWallet() {
        val privateUpdateWallet = fragmentMock.javaClass
            .getDeclaredMethod(
                "updateWallet",
                Double::class.java,
                Double::class.java,
                Int::class.java
            )
        privateUpdateWallet.isAccessible = true
        val DDmock = mockk<TextView>(relaxed = true)
        val BTCmock = mockk<TextView>(relaxed = true)
        every { DDmock.text } returns ""
        every { BTCmock.text } returns ""
        every { fragmentMock.amountFieldDD } returns DDmock
        every { fragmentMock.amountFieldBTC } returns BTCmock
        privateUpdateWallet.invoke(fragmentMock, 10.0, 10.0, 0)
        verify { DDmock.setText("9990.0") }
        verify { BTCmock.setText("1010.0") }
        privateUpdateWallet.invoke(fragmentMock, 10.0, 10.0, 1)
        verify { DDmock.setText("10000.0") }
        verify { BTCmock.setText("1000.0") }
    }
}
