package nl.tudelft.trustchain.eurotoken.benchmarks

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object UsageLogger {

    private var dao: UsageEventsDao? = null
    private val scope = CoroutineScope(Dispatchers.IO) // Use IO dispatcher for database operations

    // Call this ideally from your Application class or a central initialization point
    fun initialize(context: Context) {
        if (dao == null) {
            dao = UsageAnalyticsDatabase.getInstance(context.applicationContext).usageEventsDao()
        }
    }

    private fun generateTransactionId(): String = UUID.randomUUID().toString()
    private fun generateTransferId(): String = UUID.randomUUID().toString()
    private fun getCurrentTimestamp(): Long = System.currentTimeMillis()

    // --- Logging methods ---

    fun logTransactionStart(payload: String): String {
        val transactionId = generateTransactionId()
        val event = TransactionStartEvent(
            transactionId = transactionId,
            timestamp = getCurrentTimestamp(),
            payload = payload
        )
        scope.launch { dao?.insertTransactionStartEvent(event) }
        return transactionId // Return to caller to correlate subsequent events
    }

    fun logTransactionError(transactionId: String, error: String) {
        val event = TransactionErrorEvent(
            transactionId = transactionId,
            timestamp = getCurrentTimestamp(),
            error = error
        )
        scope.launch { dao?.insertTransactionErrorEvent(event) }
    }

    fun logTransactionCancel(transactionId: String, reason: TransactionCancelReason) { 
        val event = TransactionCancelEvent(
            transactionId = transactionId,
            timestamp = getCurrentTimestamp(),
            reason = reason
        )
        scope.launch { dao?.insertTransactionCancelEvent(event) }
    }

    fun logTransactionDone(transactionId: String) { 
        val event = TransactionDoneEvent(
            transactionId = transactionId,
            timestamp = getCurrentTimestamp()
        )
        scope.launch { dao?.insertTransactionDoneEvent(event) }
    }

    fun logTransferStart(transactionId: String, payloadSize: Long, direction: TransferDirection): String { 
        val transferId = generateTransferId()
        val event = TransferStartEvent(
            transactionId = transactionId,
            transferId = transferId,
            timestamp = getCurrentTimestamp(),
            payloadSize = payloadSize,
            direction = direction
        )
        scope.launch { dao?.insertTransferStartEvent(event) }
        return transferId // Return to caller for correlating end/error events
    }

    fun logTransferDone(transferId: String) { 
        val event = TransferDoneEvent(
            transferId = transferId,
            timestamp = getCurrentTimestamp()
        )
        scope.launch { dao?.insertTransferDoneEvent(event) }
    }

    fun logTransferError(transferId: String, error: TransferError) { 
        val event = TransferErrorEvent(
            transferId = transferId,
            timestamp = getCurrentTimestamp(),
            error = error
        )
        scope.launch { dao?.insertTransferErrorEvent(event) }
    }

}
