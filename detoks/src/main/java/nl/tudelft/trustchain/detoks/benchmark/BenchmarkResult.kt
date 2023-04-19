package nl.tudelft.trustchain.detoks.benchmark

import com.github.mikephil.charting.data.Entry

/**
 * This is a wrapper around benchmark results for the TransactionEngine.
 */
data class BenchmarkResult (
    var timePerBlock : ArrayList<Entry>,
    var totalTime : Long,
    var payloadBandwith : Double,
    var lostPackets: Int
)
