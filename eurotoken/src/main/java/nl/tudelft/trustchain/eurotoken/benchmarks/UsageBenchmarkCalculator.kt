package nl.tudelft.trustchain.eurotoken.benchmarks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Data class for results that have a maximum and an average value.
 * @param max The maximum value observed.
 * @param average The average value calculated.
 * @param unit The unit of measurement for these values (e.g., "ms", "bytes/ms").
 */
data class MaxAvgResult(val max: Double, val average: Double, val unit: String = "")

/**
 * Data class for payload size results, where max is Long.
 */
data class MaxAvgLongResult(val max: Long, val average: Double, val unit: String = "")

class UsageBenchmarkCalculator(private val dao: UsageEventsDao) {

    private suspend fun <T> onIO(block: suspend CoroutineScope.() -> T) = withContext(Dispatchers.IO, block)

    // --- Transaction Benchmarks ---

    /**
     * Calculates the maximum and average total payload size for successful transactions.
     * Payload size for a transaction is the sum of payload sizes of all its transfers.
     */
    suspend fun calculateMaxAndAvgTransactionPayloadSize(): MaxAvgLongResult? = onIO {
        val successfulTransactions = dao.getAllTransactionDoneEvents()
        if (successfulTransactions.isEmpty()) return@onIO null

        var totalPayloadSum = 0L
        var maxPayload = 0L
        val transactionPayloads = mutableListOf<Long>()

        for (txDone in successfulTransactions) {
            val transfers = dao.getTransferStartEventsForTransaction(txDone.transactionId)
            val currentTransactionPayload = transfers.sumOf { it.payloadSize }
            if (currentTransactionPayload > 0) {
                transactionPayloads.add(currentTransactionPayload)
                totalPayloadSum += currentTransactionPayload
                maxPayload = max(maxPayload, currentTransactionPayload)
            }
        }
        if (transactionPayloads.isEmpty()) {
            return@onIO MaxAvgLongResult(0L, 0.0, "bytes")
        }
        MaxAvgLongResult(maxPayload, totalPayloadSum.toDouble() / transactionPayloads.size, "bytes")
    }

    /**
     * Calculates the maximum and average time to complete a successful transaction.
     * Time is calculated from TransactionStartEvent to TransactionDoneEvent.
     */
    suspend fun calculateMaxAndAvgTransactionTime(): MaxAvgResult? = onIO {
        val successfulTransactions = dao.getAllTransactionDoneEvents()
        if (successfulTransactions.isEmpty()) return@onIO null

        var totalDurationSum = 0L
        var maxDuration = 0L
        val transactionDurations = mutableListOf<Long>()

        for (txDone in successfulTransactions) {
            dao.getTransactionStartEvent(txDone.transactionId)?.let {
                val duration = txDone.timestamp - it.timestamp
                if (duration >= 0) {
                    transactionDurations.add(duration)
                    totalDurationSum += duration
                    maxDuration = max(maxDuration, duration)
                }
            }
        }

        if (transactionDurations.isEmpty()) {
            return@onIO MaxAvgResult(0.0, 0.0, "ms")
        }
        MaxAvgResult(maxDuration.toDouble(), totalDurationSum.toDouble() / transactionDurations.size, "ms")
    }

    /**
     * Calculates the total number of transactions initiated.
     */
    suspend fun calculateTotalTransactionCount(): Long = onIO {
        dao.getTransactionStartCount()
    }

    /**
     * Calculates the average success rate of transactions.
     * (Total successful transactions / Total initiated transactions) * 100
     */
    suspend fun calculateAvgTransactionSuccessRate(): Double = onIO {
        val totalStarted = dao.getTransactionStartCount()
        if (totalStarted == 0L) {
            return@onIO 0.0
        }
        val totalDone = dao.getTransactionDoneCount()
        (totalDone.toDouble() / totalStarted) * 100.0
    }

    /**
     * Calculates the average error rate of transactions.
     */
    suspend fun calculateAvgTransactionErrorRate(): Double = onIO {
        val totalStarted = dao.getTransactionStartCount()
        if (totalStarted == 0L) {
            return@onIO 0.0
        }
        val totalErrored = dao.getTransactionErrorCount()
        ((totalErrored).toDouble() / totalStarted) * 100.0
    }

    /**
     * Calculates the average manual cancel rate of transactions.
     * Estimating when users gave up on a transaction.
     */
    suspend fun calculateAvgTransactionCancelRate(): Double = onIO {
        val totalStarted = dao.getTransactionStartCount()
        if (totalStarted == 0L) {
            return@onIO 0.0
        }
        val allCancelEvents = dao.getAllTransactionCancelEvents()
        val manualCancels = allCancelEvents.count {it.reason == TransactionCancelReason.MANUAL}
        (manualCancels.toDouble() / totalStarted) * 100.0
    }

    /**
     * Calculates the rate of transactions that were cancelled due to timeout.
     * (Total transactions cancelled with reason TIMEOUT / Total initiated transactions) * 100
     */
    suspend fun calculateAvgTransactionTimeoutRate(): Double = onIO {
        val totalStarted = dao.getTransactionStartCount()
        if (totalStarted == 0L) {
            return@onIO 0.0
        }
        val allCancelEvents = dao.getAllTransactionCancelEvents()
        val timeoutCancels = allCancelEvents.count { it.reason == TransactionCancelReason.TIMEOUT }
        (timeoutCancels.toDouble() / totalStarted) * 100.0
    }

    // --- Transfer Benchmarks ---

    /**
     * Calculates the maximum and average throughput for successful transfers.
     * Throughput = payloadSize / (TransferDone.timestamp - TransferStart.timestamp).
     * Unit: bytes per millisecond (bytes/ms).
     */
    suspend fun calculateMaxAndAvgTransferThroughput(): MaxAvgResult? = onIO {
        val successfulTransfers = dao.getAllTransferDoneEvents()
        if (successfulTransfers.isEmpty()) {
            return@onIO null
        }

        var totalThroughputSum = 0.0
        var maxThroughput = 0.0
        val throughputs = mutableListOf<Double>()

        for (transferDone in successfulTransfers) {
            dao.getTransferStartEventByTransferId(transferDone.transferId)?.let { transferStart ->
                val duration = transferDone.timestamp - transferStart.timestamp
                if (duration > 0 && transferStart.payloadSize > 0) {
                    val throughput = transferStart.payloadSize.toDouble() / duration
                    throughputs.add(throughput)
                    totalThroughputSum += throughput
                    maxThroughput = max(maxThroughput, throughput)
                }
            }
        }

        if (throughputs.isEmpty()) {
            return@onIO MaxAvgResult(0.0, 0.0, "bytes/ms")
        }
        MaxAvgResult(maxThroughput, totalThroughputSum / throughputs.size, "bytes/ms")
    }

    /**
     * Calculates the total number of transfers initiated.
     */
    suspend fun calculateTotalTransferCount(): Long = onIO {
        dao.getTransferStartCount()
    }

    /**
     * Calculates the average failure rate of transfers.
     * (Total transfer error events / Total initiated transfers) * 100
     */
    suspend fun calculateAvgTransferFailureRate(): Double = onIO {
        val totalStarted = dao.getTransferStartCount()
        if (totalStarted == 0L) {
            return@onIO 0.0
        }
        val totalErrored = dao.getTransferErrorCount()
        (totalErrored.toDouble() / totalStarted) * 100.0
    }

    /**
     * Calculates the average success rate of transfers.
     * (Total successful transfers / Total initiated transfers) * 100
     */
    suspend fun calculateAvgTransferSuccessRate(): Double = onIO {
        val totalStarted = dao.getTransferStartCount()
        if (totalStarted == 0L) {
            return@onIO 0.0
        }
        val totalDone = dao.getTransferDoneCount()
        (totalDone.toDouble() / totalStarted) * 100.0
    }
}
