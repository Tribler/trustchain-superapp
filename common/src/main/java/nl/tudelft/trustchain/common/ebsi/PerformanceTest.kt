package nl.tudelft.trustchain.common.ebsi

import android.content.Context
import java.util.*

class PerformanceTest(
    private val context: Context,
    private val uuid: UUID
) {
    val TAG = PerformanceTest::class.simpleName!!
    val wallet = EBSIWallet(context)

    fun run() {

    }
}
