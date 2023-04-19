package nl.tudelft.detoks_engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks_engine.manage_tokens.Transaction
import nl.tudelft.trustchain.detoks_engine.trustchain.GroupedAdapter
import nl.tudelft.trustchain.detoks_engine.trustchain.TrustChainTransactionCommunity
import org.junit.Assert
import org.junit.Test
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


internal class SenderUnitTest {
    private val trustChainCommunity: TrustChainTransactionCommunity = IPv8Android.getInstance().getOverlay<TrustChainTransactionCommunity>()!!
    private val communityAdapter: GroupedAdapter = GroupedAdapter(trustChainCommunity)


    // @Test
    fun sendToken() {
        val token = "test-${System.currentTimeMillis()}-${UUID.randomUUID()}"
        val finished = AtomicBoolean(false)

        communityAdapter.onTransactionAgreementReceived { transaction, isForMe ->
            if (!isForMe)
                return@onTransactionAgreementReceived
            val ts = Transaction.fromTrustChainTransactionObject(transaction).tokens
            Assert.assertArrayEquals("WA", ts.toTypedArray(), arrayOf(token))
            finished.set(true)
        }

        Transaction(listOf(token)).also {
            communityAdapter.proposeTransaction(it.toTrustChainTransaction(), communityAdapter.getPeers()[0])
        }

        runBlocking {
            val start = System.currentTimeMillis()
            while (!finished.get() && (System.currentTimeMillis() - start) < 1000) {
                delay(100)
            }
        }
        Assert.assertTrue("TLE", finished.get())
    }
}
